import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { UpdateCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const TABLE = process.env.TENANT_CONFIG_TABLE;

export const handler = async (event) => {
  const detail = event.detail || {};
  const detailType = event['detail-type'];

  console.log(`Tenant sync event: ${detailType}`, JSON.stringify(detail));

  // Extract tenant info from the event
  const tenantId = detail.tenantId || detail.configurationSetName?.replace('tenant-', '') || null;
  if (!tenantId) {
    console.warn('No tenant ID found in event');
    return;
  }

  const now = new Date().toISOString();
  let status = 'ACTIVE';

  if (detailType === 'SES Reputation Pause') {
    status = 'PAUSED';
  }

  await ddb.send(new UpdateCommand({
    TableName: TABLE,
    Key: { tenant_id: tenantId },
    UpdateExpression: 'SET #status = :status, last_event = :event, updated_at = :now',
    ExpressionAttributeNames: { '#status': 'status' },
    ExpressionAttributeValues: {
      ':status': status,
      ':event': detailType,
      ':now': now,
    },
  }));

  console.log(`Tenant ${tenantId} status updated to ${status}`);
};
