import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { UpdateCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const TABLE = process.env.SEND_RESULTS_TABLE;

export const handler = async (event) => {
  const detail = event.detail || {};
  const eventType = event['detail-type'] || detail.eventType;
  const mail = detail.mail || {};
  const tags = mail.tags || {};

  const correlationId = tags.correlation_id?.[0] || null;
  const tenantId = tags.tenant_id?.[0] || mail.tenantId || 'unknown';
  const messageId = mail.messageId || null;

  if (!correlationId) {
    console.warn('No correlation_id in event, skipping:', eventType);
    return;
  }

  const statusMap = {
    Send: 'Sending',
    Delivery: 'Delivered',
    Open: 'Delivered',
    Click: 'Delivered',
    Reject: 'Rejected',
    DeliveryDelay: 'Delayed',
    RenderingFailure: 'Error',
  };

  const status = statusMap[eventType] || 'Sending';
  const now = new Date().toISOString();

  const updateParams = {
    TableName: TABLE,
    Key: { tenant_id: tenantId, correlation_id: correlationId },
    UpdateExpression: 'SET #status = :status, event_type = :eventType, message_id = :messageId, updated_at = :now',
    ExpressionAttributeNames: { '#status': 'status' },
    ExpressionAttributeValues: {
      ':status': status,
      ':eventType': eventType,
      ':messageId': messageId,
      ':now': now,
    },
  };

  await ddb.send(new UpdateCommand(updateParams));
  console.log(`Event processed: ${eventType} → ${status} (correlationId: ${correlationId})`);
};
