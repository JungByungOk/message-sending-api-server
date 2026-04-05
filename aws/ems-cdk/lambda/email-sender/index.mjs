import { SESv2Client, SendEmailCommand } from '@aws-sdk/client-sesv2';
import { DynamoDBClient, PutItemCommand, GetItemCommand } from '@aws-sdk/client-dynamodb';

const ses = new SESv2Client({});
const dynamo = new DynamoDBClient({});
const TENANT_CONFIG_TABLE = process.env.TENANT_CONFIG_TABLE;
const IDEMPOTENCY_TABLE = process.env.IDEMPOTENCY_TABLE;

export const handler = async (event) => {
  for (const record of event.Records) {
    const body = JSON.parse(record.body);
    const messageId = body.messageId || `${Date.now()}-${Math.random().toString(36).slice(2)}`;

    // 멱등성 체크
    try {
      const existing = await dynamo.send(new GetItemCommand({
        TableName: IDEMPOTENCY_TABLE,
        Key: { message_id: { S: messageId } },
      }));
      if (existing.Item) {
        console.log(`Duplicate message skipped: ${messageId}`);
        continue;
      }
    } catch (e) {
      console.warn('Idempotency check failed, proceeding:', e.message);
    }

    // 테넌트 설정 조회
    let configSetName = null;
    if (body.tenantId) {
      try {
        const config = await dynamo.send(new GetItemCommand({
          TableName: TENANT_CONFIG_TABLE,
          Key: { tenant_id: { S: body.tenantId } },
        }));
        configSetName = config.Item?.config_set_name?.S;
      } catch (e) {
        console.warn('Tenant config lookup failed:', e.message);
      }
    }

    // SES 발송
    const params = {
      FromEmailAddress: body.from,
      Destination: {
        ToAddresses: Array.isArray(body.to) ? body.to : [body.to],
        CcAddresses: body.cc || [],
        BccAddresses: body.bcc || [],
      },
      Content: {},
    };

    if (configSetName) {
      params.ConfigurationSetName = configSetName;
    }

    // 태그 추가
    if (body.tags && body.tags.length > 0) {
      params.EmailTags = body.tags.map(t => ({ Name: t.name, Value: t.value }));
    }
    // tenant_id 태그 추가
    if (body.tenantId) {
      params.EmailTags = params.EmailTags || [];
      params.EmailTags.push({ Name: 'tenant_id', Value: body.tenantId });
    }

    // 템플릿 발송 vs 일반 발송
    if (body.templateName) {
      params.Content.Template = {
        TemplateName: body.templateName,
        TemplateData: JSON.stringify(body.templateData || {}),
      };
    } else {
      params.Content.Simple = {
        Subject: { Data: body.subject || '' },
        Body: {
          Html: { Data: body.body || '' },
        },
      };
    }

    try {
      const result = await ses.send(new SendEmailCommand(params));
      console.log(`Email sent: ${result.MessageId}`);

      // 멱등성 기록 (TTL 24h)
      await dynamo.send(new PutItemCommand({
        TableName: IDEMPOTENCY_TABLE,
        Item: {
          message_id: { S: messageId },
          ses_message_id: { S: result.MessageId },
          ttl: { N: String(Math.floor(Date.now() / 1000) + 86400) },
        },
      }));
    } catch (e) {
      console.error(`Email send failed: ${e.message}`);
      throw e; // SQS 재시도
    }
  }
};
