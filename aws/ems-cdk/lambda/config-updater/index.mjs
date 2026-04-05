import { SSMClient, PutParameterCommand } from '@aws-sdk/client-ssm';

const ssmClient = new SSMClient({});
const { SSM_MODE_PARAM, SSM_CALLBACK_URL_PARAM, SSM_CALLBACK_SECRET_PARAM } = process.env;

export const handler = async (event) => {
  const body = JSON.parse(event.body || '{}');
  const updates = [];

  if (body.mode !== undefined) {
    updates.push(ssmClient.send(new PutParameterCommand({
      Name: SSM_MODE_PARAM, Value: body.mode, Type: 'String', Overwrite: true,
    })));
  }
  if (body.callback_url !== undefined) {
    updates.push(ssmClient.send(new PutParameterCommand({
      Name: SSM_CALLBACK_URL_PARAM, Value: body.callback_url, Type: 'String', Overwrite: true,
    })));
  }
  if (body.callback_secret !== undefined) {
    updates.push(ssmClient.send(new PutParameterCommand({
      Name: SSM_CALLBACK_SECRET_PARAM, Value: body.callback_secret, Type: 'String', Overwrite: true,
    })));
  }

  await Promise.all(updates);
  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ success: true, updated: Object.keys(body) }),
  };
};
