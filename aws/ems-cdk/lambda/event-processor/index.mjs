import { DynamoDBClient, PutItemCommand } from '@aws-sdk/client-dynamodb';
import { SSMClient, GetParameterCommand } from '@aws-sdk/client-ssm';

const dynamo = new DynamoDBClient({});
const ssm = new SSMClient({});
const SEND_RESULTS_TABLE = process.env.SEND_RESULTS_TABLE;

// SSM 캐시 (30초)
let ssmCache = { mode: null, callbackUrl: null, callbackSecret: null, expiry: 0 };

async function getSSMParams() {
  if (Date.now() < ssmCache.expiry) return ssmCache;

  const [mode, url, secret] = await Promise.all([
    ssm.send(new GetParameterCommand({ Name: process.env.SSM_MODE_PARAM })),
    ssm.send(new GetParameterCommand({ Name: process.env.SSM_CALLBACK_URL_PARAM })),
    ssm.send(new GetParameterCommand({ Name: process.env.SSM_CALLBACK_SECRET_PARAM })),
  ]);

  ssmCache = {
    mode: mode.Parameter.Value,
    callbackUrl: url.Parameter.Value,
    callbackSecret: secret.Parameter.Value,
    expiry: Date.now() + 30000,
  };
  return ssmCache;
}

export const handler = async (event) => {
  const params = await getSSMParams();

  for (const record of event.Records) {
    const snsMessage = JSON.parse(record.Sns.Message);
    const eventType = snsMessage.eventType || snsMessage.notificationType;
    const mail = snsMessage.mail || {};
    const messageId = mail.messageId || '';
    const timestamp = mail.timestamp || new Date().toISOString();

    // tenant_id 추출 (태그에서)
    let tenantId = '';
    if (mail.tags && mail.tags.tenant_id) {
      tenantId = Array.isArray(mail.tags.tenant_id) ? mail.tags.tenant_id[0] : mail.tags.tenant_id;
    }

    let correlationId = '';
    if (mail.tags && mail.tags.correlation_id) {
      correlationId = Array.isArray(mail.tags.correlation_id) ? mail.tags.correlation_id[0] : mail.tags.correlation_id;
    }

    // 수신자 추출
    let recipients = [];
    let extraFields = {};
    if (eventType === 'Send') {
      recipients = mail.destination || [];
    } else if (eventType === 'Bounce' && snsMessage.bounce) {
      recipients = snsMessage.bounce.bouncedRecipients?.map(r => r.emailAddress) || [];
    } else if (eventType === 'Complaint' && snsMessage.complaint) {
      recipients = snsMessage.complaint.complainedRecipients?.map(r => r.emailAddress) || [];
    } else if (eventType === 'Delivery' && snsMessage.delivery) {
      recipients = snsMessage.delivery.recipients || [];
    } else if (eventType === 'Open') {
      recipients = mail.destination || [];
    } else if (eventType === 'Click' && snsMessage.click) {
      recipients = mail.destination || [];
      extraFields.click_link = snsMessage.click.link || '';
    } else if (eventType === 'Reject') {
      recipients = mail.destination || [];
    } else if (eventType === 'DeliveryDelay' && snsMessage.deliveryDelay) {
      recipients = snsMessage.deliveryDelay.delayedRecipients?.map(r => r.emailAddress) || [];
    } else if (eventType === 'RenderingFailure') {
      recipients = mail.destination || [];
    }

    // 상태 매핑 (DynamoDB 저장용)
    const statusMap = {
      Send: 'SENDING',
      Delivery: 'DELIVERED',
      Bounce: 'BOUNCED',
      Complaint: 'COMPLAINED',
      Open: 'OPENED',
      Click: 'CLICKED',
      Reject: 'REJECTED',
      DeliveryDelay: 'DELAYED',
      RenderingFailure: 'RENDER_FAILED',
      Subscription: 'DELIVERED',
    };
    const status = statusMap[eventType] || eventType;

    // ① DynamoDB 저장 (항상)
    try {
      const item = {
        tenant_id: { S: tenantId || 'unknown' },
        message_id: { S: messageId },
        status: { S: status },
        event_type: { S: eventType },
        recipients: { SS: recipients.length > 0 ? recipients : ['unknown'] },
        timestamp: { S: timestamp },
        ttl: { N: String(Math.floor(Date.now() / 1000) + 604800) }, // 7일
      };
      if (correlationId) item.correlation_id = { S: correlationId };
      // 추가 필드 저장 (click link 등)
      for (const [key, value] of Object.entries(extraFields)) {
        if (value) item[key] = { S: value };
      }
      await dynamo.send(new PutItemCommand({
        TableName: SEND_RESULTS_TABLE,
        Item: item,
      }));
      console.log(`Event saved: ${messageId} -> ${status}`);
    } catch (e) {
      console.error(`DynamoDB save failed: ${e.message}`);
      throw e;
    }

    // ② Callback 모드일 때 ESM 호출
    if (params.mode === 'callback' && !params.callbackUrl) {
      console.warn(JSON.stringify({
        level: 'WARN',
        message: 'mode=callback but callback_url is empty. Events stored in DynamoDB only. Set /ems/callback_url via PUT /config.',
      }));
    }

    if (params.mode === 'callback' && params.callbackUrl) {
      try {
        const callbackBody = JSON.stringify({
          tenantId,
          messageId,
          correlationId,
          eventType: {
            Send: 'SEND',
            Delivery: 'DELIVERY',
            Bounce: 'BOUNCE',
            Complaint: 'COMPLAINT',
            Open: 'OPEN',
            Click: 'CLICK',
            Reject: 'REJECT',
            DeliveryDelay: 'DELIVERY_DELAY',
            RenderingFailure: 'RENDERING_FAILURE',
            Subscription: 'SUBSCRIPTION',
          }[eventType] || eventType.toUpperCase(),
          timestamp,
          recipients,
          details: {},
        });

        const response = await fetch(params.callbackUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Callback-Secret': params.callbackSecret || '',
          },
          body: callbackBody,
          signal: AbortSignal.timeout(10000),
        });

        console.log(`Callback sent: ${response.status}`);
      } catch (e) {
        console.warn(`Callback failed (event already in DynamoDB): ${e.message}`);
        // 콜백 실패해도 DynamoDB에 이미 저장됨 → 보정 폴링으로 보완
      }
    }
  }
};
