import { SQSClient, SendMessageBatchCommand } from '@aws-sdk/client-sqs';
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { GetCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';
import { S3Client, GetObjectCommand } from '@aws-sdk/client-s3';

const sqs = new SQSClient({});
const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const s3 = new S3Client({});
const QUEUE_URL = process.env.SEND_QUEUE_URL;
const CONFIG_TABLE = process.env.TENANT_CONFIG_TABLE;
const BUCKET = process.env.BATCH_BUCKET;

export const handler = async (event) => {
  const body = JSON.parse(event.body || '{}');
  const { tenantId, templateName, from, subject, htmlBody, recipients, s3Key } = body;

  // Get tenant config
  const configResult = await ddb.send(new GetCommand({
    TableName: CONFIG_TABLE,
    Key: { tenant_id: tenantId },
  }));
  const config = configResult.Item || {};
  const sesTenantName = config.ses_tenant_name || null;
  const configSetName = config.config_set_name || `tenant-${tenantId}`;

  // Get recipients from body or S3
  let recipientList = recipients || [];
  if (s3Key && recipientList.length === 0) {
    const s3Response = await s3.send(new GetObjectCommand({ Bucket: BUCKET, Key: s3Key }));
    const s3Body = await s3Response.Body.transformToString();
    recipientList = JSON.parse(s3Body);
  }

  // Create SQS messages in batches of 10
  let enqueued = 0;
  for (let i = 0; i < recipientList.length; i += 10) {
    const batch = recipientList.slice(i, i + 10);
    const entries = batch.map((recipient, idx) => ({
      Id: `msg-${i + idx}`,
      MessageBody: JSON.stringify({
        tenantId,
        sesTenantName,
        configSetName,
        correlationId: recipient.correlationId || `${tenantId}-${Date.now()}-${i + idx}`,
        from: from || recipient.from,
        to: recipient.to || recipient.email,
        templateName: templateName || null,
        templateData: recipient.templateData || {},
        subject: subject || null,
        body: htmlBody || null,
        emailSendSeq: recipient.emailSendSeq || null,
        emailSendDtlSeq: recipient.emailSendDtlSeq || null,
      }),
    }));

    await sqs.send(new SendMessageBatchCommand({
      QueueUrl: QUEUE_URL,
      Entries: entries,
    }));
    enqueued += entries.length;
  }

  return {
    statusCode: 200,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ enqueued, total: recipientList.length }),
  };
};
