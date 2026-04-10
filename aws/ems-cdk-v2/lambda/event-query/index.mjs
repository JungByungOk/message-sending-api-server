import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { QueryCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const TABLE = process.env.SEND_RESULTS_TABLE;

export const handler = async (event) => {
  const params = event.queryStringParameters || {};
  const tenantId = params.tenant_id;
  const after = params.after || new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
  const limit = parseInt(params.limit || '300', 10);

  if (!tenantId) {
    return respond(400, { error: 'tenant_id is required' });
  }

  const result = await ddb.send(new QueryCommand({
    TableName: TABLE,
    IndexName: 'gsi-tenant-timestamp',
    KeyConditionExpression: 'tenant_id = :tid AND #ts >= :after',
    ExpressionAttributeNames: { '#ts': 'timestamp' },
    ExpressionAttributeValues: {
      ':tid': tenantId,
      ':after': after,
    },
    Limit: limit,
    ScanIndexForward: false,
  }));

  return respond(200, {
    items: result.Items || [],
    count: result.Count,
    lastKey: result.LastEvaluatedKey || null,
  });
};

function respond(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}
