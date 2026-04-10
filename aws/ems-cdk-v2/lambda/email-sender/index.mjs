import { SESv2Client, SendEmailCommand } from '@aws-sdk/client-sesv2';
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PutCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ses = new SESv2Client({ region: process.env.SES_REGION });
const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const TABLE = process.env.SEND_RESULTS_TABLE;

export const handler = async (event) => {
  const failures = [];

  for (const record of event.Records) {
    try {
      const msg = JSON.parse(record.body);
      const { tenantId, sesTenantName, configSetName, correlationId, from, to, templateName, templateData, subject, body } = msg;

      const params = {
        FromEmailAddress: from,
        Destination: { ToAddresses: [to] },
        ConfigurationSetName: configSetName || undefined,
        EmailTags: [{ Name: 'correlation_id', Value: correlationId }],
      };

      // TenantName for SES Multi-Tenant (if available)
      if (sesTenantName) {
        params.ListManagementOptions = undefined; // placeholder for future TenantName support
      }

      if (templateName) {
        params.Content = {
          Template: {
            TemplateName: templateName,
            TemplateData: JSON.stringify(templateData || {}),
          },
        };
      } else {
        params.Content = {
          Simple: {
            Subject: { Data: subject || '(No Subject)' },
            Body: { Html: { Data: body || '' } },
          },
        };
      }

      const result = await ses.send(new SendEmailCommand(params));
      const messageId = result.MessageId;

      // Write initial Sending status to DynamoDB
      const now = new Date().toISOString();
      await ddb.send(new PutCommand({
        TableName: TABLE,
        Item: {
          tenant_id: tenantId,
          correlation_id: correlationId,
          message_id: messageId,
          status: 'Sending',
          from_email: from,
          to_email: to,
          template_name: templateName || null,
          timestamp: now,
          ttl: Math.floor(Date.now() / 1000) + 7 * 86400,
        },
      }));
    } catch (err) {
      console.error('Send failed:', err.message, record.messageId);
      failures.push({ itemIdentifier: record.messageId });
    }
  }

  return { batchItemFailures: failures };
};
