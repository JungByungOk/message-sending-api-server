import { DynamoDBClient, QueryCommand } from '@aws-sdk/client-dynamodb';

const dynamo = new DynamoDBClient({});
const SEND_RESULTS_TABLE = process.env.SEND_RESULTS_TABLE;

export const handler = async (event) => {
  const params = event.queryStringParameters || {};
  const tenantId = params.tenant_id;
  const after = params.after || new Date(Date.now() - 600000).toISOString(); // 기본 10분 전
  const limit = Math.min(parseInt(params.limit || '100', 10), 500);

  if (!tenantId) {
    return {
      statusCode: 400,
      body: JSON.stringify({ message: 'tenant_id is required' }),
    };
  }

  try {
    const result = await dynamo.send(new QueryCommand({
      TableName: SEND_RESULTS_TABLE,
      IndexName: 'gsi-tenant-timestamp',
      KeyConditionExpression: 'tenant_id = :tid AND #ts > :after',
      ExpressionAttributeNames: { '#ts': 'timestamp' },
      ExpressionAttributeValues: {
        ':tid': { S: tenantId },
        ':after': { S: after },
      },
      Limit: limit,
      ScanIndexForward: false, // 최신순
    }));

    const items = (result.Items || []).map(item => ({
      tenantId: item.tenant_id?.S,
      messageId: item.message_id?.S,
      status: item.status?.S,
      eventType: item.event_type?.S,
      recipients: item.recipients?.SS || [],
      timestamp: item.timestamp?.S,
    }));

    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        count: items.length,
        results: items,
        nextToken: result.LastEvaluatedKey ? JSON.stringify(result.LastEvaluatedKey) : null,
      }),
    };
  } catch (e) {
    console.error('Query failed:', e.message);
    return {
      statusCode: 500,
      body: JSON.stringify({ message: 'Query failed: ' + e.message }),
    };
  }
};
