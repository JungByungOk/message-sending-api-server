import { SESv2Client, CreateEmailIdentityCommand, DeleteEmailIdentityCommand, GetEmailIdentityCommand,
  CreateConfigurationSetCommand, DeleteConfigurationSetCommand, GetConfigurationSetCommand,
  CreateConfigurationSetEventDestinationCommand,
  CreateEmailTemplateCommand, GetEmailTemplateCommand, UpdateEmailTemplateCommand, DeleteEmailTemplateCommand, ListEmailTemplatesCommand,
  GetAccountCommand,
} from '@aws-sdk/client-sesv2';
import { DynamoDBClient, PutItemCommand, GetItemCommand, DeleteItemCommand, ScanCommand, BatchWriteItemCommand } from '@aws-sdk/client-dynamodb';

const ses = new SESv2Client({});
const dynamo = new DynamoDBClient({});
const TENANT_CONFIG_TABLE = process.env.TENANT_CONFIG_TABLE;
const SEND_RESULTS_TABLE = process.env.SEND_RESULTS_TABLE;
const IDEMPOTENCY_TABLE = process.env.IDEMPOTENCY_TABLE;
const SES_EVENT_TOPIC_ARN = process.env.SES_EVENT_TOPIC_ARN;
const TENANT_TTL = parseInt(process.env.TENANT_CONFIG_TTL_SECONDS || String(365 * 10 * 86400), 10);

export const handler = async (event) => {
  const method = event.httpMethod;
  const params = event.queryStringParameters || {};
  const body = event.body ? JSON.parse(event.body) : {};
  const action = body.action || params.action || '';

  try {
    switch (action) {
      case 'CREATE': return await createTenant(body);
      case 'ACTIVATE': return await activateTenant(body);
      case 'DKIM_STATUS': return await getDkimStatus(params.tenantId || params.domain || body.domain);
      case 'CREATE_IDENTITY': return await createIdentity(body.domain);
      case 'DELETE_IDENTITY': return await deleteIdentity(params.domain || body.domain);
      case 'VERIFY_EMAIL': return await verifyEmail(body.email);
      case 'EMAIL_STATUS': return await getEmailStatus(params.email || body.email);
      case 'RESEND_VERIFICATION': return await resendVerification(body.email);
      case 'CREATE_CONFIGSET': return await createConfigSet(body.tenantId);
      case 'GET_CONFIGSET': return await getConfigSet(params.tenantId || body.tenantId);
      case 'DELETE_CONFIGSET': return await deleteConfigSet(params.tenantId || body.tenantId);
      case 'DELETE_TENANT_CONFIG': return await deleteTenantConfig(params.tenantId || body.tenantId);
      case 'CREATE_TEMPLATE': return await createTemplate(body);
      case 'GET_TEMPLATE': return await getTemplate(params.templateName || body.templateName);
      case 'UPDATE_TEMPLATE': return await updateTemplate(body);
      case 'DELETE_TEMPLATE': return await deleteTemplate(body);
      case 'LIST_TEMPLATES': return await listTemplates();
      case 'CLEAR_EVENTS': return await clearEvents();
      case 'GET_ACCOUNT': return await getAccount();
      default:
        return respond(400, { message: `Unknown action: ${action}` });
    }
  } catch (e) {
    console.error(`Action ${action} failed:`, e.message);
    return respond(500, { message: e.message });
  }
};

// === Tenant ===

async function createTenant(body) {
  const { tenantId, tenantName, domain } = body;
  const configSetName = `tenant-${tenantId}`;

  // 1. SES Identity 생성 (이미 존재하면 현재 상태 조회)
  let verified = false;
  let dkimTokens = [];
  try {
    const identityResult = await ses.send(new CreateEmailIdentityCommand({ EmailIdentity: domain }));
    verified = identityResult.VerifiedForSendingStatus || false;
    dkimTokens = identityResult.DkimAttributes?.Tokens || [];
  } catch (e) {
    if (e.name === 'AlreadyExistsException') {
      const existing = await ses.send(new GetEmailIdentityCommand({ EmailIdentity: domain }));
      verified = existing.VerifiedForSendingStatus || false;
      dkimTokens = existing.DkimAttributes?.Tokens || [];
    } else {
      throw e;
    }
  }

  // 2. Configuration Set 생성 (이메일 이벤트 추적용)
  try {
    await ses.send(new CreateConfigurationSetCommand({ ConfigurationSetName: configSetName }));
  } catch (e) {
    if (e.name !== 'AlreadyExistsException') throw e;
  }

  // 3. SNS 이벤트 연결
  if (SES_EVENT_TOPIC_ARN) {
    try {
      await ses.send(new CreateConfigurationSetEventDestinationCommand({
        ConfigurationSetName: configSetName,
        EventDestinationName: `${configSetName}-sns`,
        EventDestination: {
          MatchingEventTypes: ['SEND', 'DELIVERY', 'BOUNCE', 'COMPLAINT', 'OPEN', 'CLICK', 'REJECT', 'DELIVERY_DELAY', 'RENDERING_FAILURE'],
          Enabled: true,
          SnsDestination: { TopicArn: SES_EVENT_TOPIC_ARN },
        },
      }));
    } catch (e) {
      if (e.name !== 'AlreadyExistsException') throw e;
    }
  }

  // 4. DynamoDB에 테넌트 설정 저장 (ConfigSet 포함)
  await dynamo.send(new PutItemCommand({
    TableName: TENANT_CONFIG_TABLE,
    Item: {
      tenant_id: { S: tenantId },
      tenant_name: { S: tenantName || '' },
      domain: { S: domain },
      config_set_name: { S: configSetName },
      verification_status: { S: verified ? 'SUCCESS' : 'PENDING' },
      ttl: { N: String(Math.floor(Date.now() / 1000) + TENANT_TTL) },
    },
  }));

  const dkimRecords = dkimTokens.map(token => ({
    name: `${token}._domainkey.${domain}`,
    type: 'CNAME',
    value: `${token}.dkim.amazonses.com`,
  }));

  return respond(200, {
    tenantId,
    configSetName,
    verificationStatus: verified ? 'SUCCESS' : 'PENDING',
    dkimRecords,
  });
}

async function activateTenant(body) {
  const { tenantId, domain } = body;
  const configSetName = `tenant-${tenantId}`;

  // ConfigSet 생성
  try {
    await ses.send(new CreateConfigurationSetCommand({ ConfigurationSetName: configSetName }));
  } catch (e) {
    if (e.name !== 'AlreadyExistsException') throw e;
  }

  // SNS 이벤트 연결
  if (SES_EVENT_TOPIC_ARN) {
    try {
      await ses.send(new CreateConfigurationSetEventDestinationCommand({
        ConfigurationSetName: configSetName,
        EventDestinationName: `${configSetName}-sns`,
        EventDestination: {
          MatchingEventTypes: ['SEND', 'DELIVERY', 'BOUNCE', 'COMPLAINT', 'OPEN', 'CLICK', 'REJECT', 'DELIVERY_DELAY', 'RENDERING_FAILURE'],
          Enabled: true,
          SnsDestination: { TopicArn: SES_EVENT_TOPIC_ARN },
        },
      }));
    } catch (e) {
      if (e.name !== 'AlreadyExistsException') throw e;
    }
  }

  // DynamoDB 업데이트
  await dynamo.send(new PutItemCommand({
    TableName: TENANT_CONFIG_TABLE,
    Item: {
      tenant_id: { S: tenantId },
      domain: { S: domain || '' },
      config_set_name: { S: configSetName },
      ttl: { N: String(Math.floor(Date.now() / 1000) + TENANT_TTL) },
    },
  }));

  return respond(200, { configSetName, tenantId });
}

// === Identity ===

async function createIdentity(domain) {
  const result = await ses.send(new CreateEmailIdentityCommand({ EmailIdentity: domain }));
  const dkimTokens = result.DkimAttributes?.Tokens || [];
  return respond(200, {
    domain,
    verificationStatus: result.VerifiedForSendingStatus ? 'SUCCESS' : 'PENDING',
    dkimRecords: dkimTokens.map(t => ({
      name: `${t}._domainkey.${domain}`, type: 'CNAME', value: `${t}.dkim.amazonses.com`,
    })),
  });
}

async function getDkimStatus(domain) {
  const result = await ses.send(new GetEmailIdentityCommand({ EmailIdentity: domain }));
  const dkimTokens = result.DkimAttributes?.Tokens || [];
  return respond(200, {
    domain,
    verificationStatus: result.VerifiedForSendingStatus ? 'SUCCESS' : 'PENDING',
    dkimRecords: dkimTokens.map(t => ({
      name: `${t}._domainkey.${domain}`, type: 'CNAME', value: `${t}.dkim.amazonses.com`,
    })),
  });
}

async function deleteIdentity(domain) {
  await ses.send(new DeleteEmailIdentityCommand({ EmailIdentity: domain }));
  return respond(200, { message: 'Deleted', domain });
}

// === Email Identity (개별 이메일 인증) ===

async function verifyEmail(email) {
  try {
    await ses.send(new CreateEmailIdentityCommand({ EmailIdentity: email }));
  } catch (e) {
    if (e.name === 'AlreadyExistsException') {
      // 이미 등록된 이메일이면 현재 상태를 조회하여 반환
      const result = await ses.send(new GetEmailIdentityCommand({ EmailIdentity: email }));
      const status = result.VerifiedForSendingStatus ? 'SUCCESS' : 'PENDING';
      return respond(200, { email, verificationStatus: status });
    }
    throw e;
  }
  return respond(200, { email, verificationStatus: 'PENDING' });
}

async function getEmailStatus(email) {
  const result = await ses.send(new GetEmailIdentityCommand({ EmailIdentity: email }));
  let verificationStatus = 'PENDING';
  if (result.VerifiedForSendingStatus === true) {
    verificationStatus = 'SUCCESS';
  } else if (result.VerificationStatus === 'FAILED') {
    verificationStatus = 'FAILED';
  }
  return respond(200, { email, verificationStatus });
}

async function resendVerification(email) {
  try {
    await ses.send(new DeleteEmailIdentityCommand({ EmailIdentity: email }));
  } catch (e) {
    if (e.name !== 'NotFoundException') throw e;
  }
  await ses.send(new CreateEmailIdentityCommand({ EmailIdentity: email }));
  return respond(200, { email, verificationStatus: 'PENDING', message: '인증 이메일이 재발송되었습니다' });
}

// === ConfigSet ===

async function createConfigSet(tenantId) {
  const name = `tenant-${tenantId}`;
  await ses.send(new CreateConfigurationSetCommand({ ConfigurationSetName: name }));
  return respond(201, { configSetName: name });
}

async function getConfigSet(tenantId) {
  const name = `tenant-${tenantId}`;
  const result = await ses.send(new GetConfigurationSetCommand({ ConfigurationSetName: name }));
  return respond(200, { configSetName: result.ConfigurationSetName, tenantId });
}

async function deleteConfigSet(tenantId) {
  const name = `tenant-${tenantId}`;
  try {
    await ses.send(new DeleteConfigurationSetCommand({ ConfigurationSetName: name }));
  } catch (e) {
    if (e.name !== 'NotFoundException') throw e;
  }
  return respond(200, { message: 'Deleted', configSetName: name });
}

async function deleteTenantConfig(tenantId) {
  try {
    await dynamo.send(new DeleteItemCommand({
      TableName: TENANT_CONFIG_TABLE,
      Key: { tenant_id: { S: tenantId } },
    }));
  } catch (e) {
    console.warn(`deleteTenantConfig: ${e.message}`);
  }
  return respond(200, { message: 'Deleted', tenantId });
}

// === Template ===

async function getTemplate(templateName) {
  const result = await ses.send(new GetEmailTemplateCommand({ TemplateName: templateName }));
  return respond(200, {
    templateName: result.TemplateName,
    subjectPart: result.TemplateContent?.Subject || '',
    htmlPart: result.TemplateContent?.Html || '',
    textPart: result.TemplateContent?.Text || '',
  });
}

async function createTemplate(body) {
  await ses.send(new CreateEmailTemplateCommand({
    TemplateName: body.templateName,
    TemplateContent: {
      Subject: body.subjectPart,
      Html: body.htmlPart,
      Text: body.textPart,
    },
  }));
  return respond(200, { awsRequestId: 'ok', templateName: body.templateName });
}

async function updateTemplate(body) {
  await ses.send(new UpdateEmailTemplateCommand({
    TemplateName: body.templateName,
    TemplateContent: {
      Subject: body.subjectPart,
      Html: body.htmlPart,
      Text: body.textPart,
    },
  }));
  return respond(200, { awsRequestId: 'ok', templateName: body.templateName });
}

async function deleteTemplate(body) {
  await ses.send(new DeleteEmailTemplateCommand({ TemplateName: body.templateName }));
  return respond(200, { awsRequestId: 'ok', templateName: body.templateName });
}

async function listTemplates() {
  const result = await ses.send(new ListEmailTemplatesCommand({ PageSize: 100 }));
  const templates = (result.TemplatesMetadata || []).map(t => ({
    name: t.TemplateName,
    createdTimestamp: t.CreatedTimestamp?.toISOString(),
  }));
  return respond(200, templates);
}

// === Util ===

// === Account ===

async function getAccount() {
  const result = await ses.send(new GetAccountCommand({}));
  const quota = result.SendQuota || {};
  return respond(200, {
    maxSendRate: quota.MaxSendRate,
    max24HourSend: quota.Max24HourSend,
    sentLast24Hours: quota.SentLast24Hours,
    remaining24Hours: (quota.Max24HourSend || 0) - (quota.SentLast24Hours || 0),
    productionAccess: result.ProductionAccessEnabled || false,
  });
}

// === Clear Events (발송 결과 초기화) ===

async function clearEvents() {
  let totalDeleted = 0;

  for (const tableName of [SEND_RESULTS_TABLE, IDEMPOTENCY_TABLE]) {
    if (!tableName) continue;
    let lastKey = undefined;
    do {
      const scanParams = { TableName: tableName, Limit: 500 };
      if (lastKey) scanParams.ExclusiveStartKey = lastKey;
      const result = await dynamo.send(new ScanCommand(scanParams));
      const items = result.Items || [];
      lastKey = result.LastEvaluatedKey;

      if (items.length === 0) break;

      // DynamoDB BatchWriteItem은 25개씩
      for (let i = 0; i < items.length; i += 25) {
        const batch = items.slice(i, i + 25);
        const keyName = tableName === IDEMPOTENCY_TABLE ? 'message_id' : 'message_id';
        await dynamo.send(new BatchWriteItemCommand({
          RequestItems: {
            [tableName]: batch.map(item => ({
              DeleteRequest: { Key: { message_id: item.message_id } },
            })),
          },
        }));
        totalDeleted += batch.length;
      }
    } while (lastKey);
  }

  console.log(`CLEAR_EVENTS completed. Total deleted: ${totalDeleted}`);
  return respond(200, { message: 'Events cleared', totalDeleted });
}

function respond(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}
