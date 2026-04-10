import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { UpdateCommand, PutCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const RESULTS_TABLE = process.env.SEND_RESULTS_TABLE;
const SUPPRESSION_TABLE = process.env.SUPPRESSION_TABLE;

export const handler = async (event) => {
  const detail = event.detail || {};
  const eventType = event['detail-type']; // 'Bounce' or 'Complaint'
  const mail = detail.mail || {};
  const tags = mail.tags || {};

  const correlationId = tags.correlation_id?.[0] || null;
  const tenantId = tags.tenant_id?.[0] || mail.tenantId || 'unknown';
  const messageId = mail.messageId || null;

  const status = eventType === 'Bounce' ? 'Bounced' : 'Complained';
  const now = new Date().toISOString();

  // Update send results
  if (correlationId) {
    await ddb.send(new UpdateCommand({
      TableName: RESULTS_TABLE,
      Key: { tenant_id: tenantId, correlation_id: correlationId },
      UpdateExpression: 'SET #status = :status, event_type = :eventType, message_id = :messageId, updated_at = :now',
      ExpressionAttributeNames: { '#status': 'status' },
      ExpressionAttributeValues: {
        ':status': status,
        ':eventType': eventType,
        ':messageId': messageId,
        ':now': now,
      },
    }));
  }

  // Extract bounced/complained email addresses
  let emails = [];
  if (eventType === 'Bounce' && detail.bounce) {
    emails = (detail.bounce.bouncedRecipients || []).map(r => r.emailAddress);
  } else if (eventType === 'Complaint' && detail.complaint) {
    emails = (detail.complaint.complainedRecipients || []).map(r => r.emailAddress);
  }

  // Write to suppression table
  for (const email of emails) {
    await ddb.send(new PutCommand({
      TableName: SUPPRESSION_TABLE,
      Item: {
        email,
        tenant_id: tenantId,
        reason: eventType.toUpperCase(),
        created_at: now,
        ttl: Math.floor(Date.now() / 1000) + 365 * 86400, // 1 year
      },
      ConditionExpression: 'attribute_not_exists(email) AND attribute_not_exists(tenant_id)',
    }).catch(err => {
      if (err.name !== 'ConditionalCheckFailedException') throw err;
      // Already exists, skip
    }));
  }

  console.log(`Suppression: ${eventType}, ${emails.length} emails added for tenant ${tenantId}`);
};
