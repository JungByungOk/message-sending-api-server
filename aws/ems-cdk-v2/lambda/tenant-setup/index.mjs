import {
  SESv2Client,
  CreateEmailIdentityCommand,
  DeleteEmailIdentityCommand,
  GetEmailIdentityCommand,
  CreateConfigurationSetCommand,
  DeleteConfigurationSetCommand,
  GetConfigurationSetCommand,
  CreateConfigurationSetEventDestinationCommand,
  CreateEmailTemplateCommand,
  GetEmailTemplateCommand,
  UpdateEmailTemplateCommand,
  DeleteEmailTemplateCommand,
  ListEmailTemplatesCommand,
  GetAccountCommand,
  PutAccountVdmAttributesCommand,
} from '@aws-sdk/client-sesv2';
import { DynamoDBClient } from '@aws-sdk/client-dynamodb';
import { PutCommand, GetCommand, DeleteCommand, ScanCommand, DynamoDBDocumentClient } from '@aws-sdk/lib-dynamodb';

const ses = new SESv2Client({ region: process.env.SES_REGION });
const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));
const CONFIG_TABLE = process.env.TENANT_CONFIG_TABLE;
const RESULTS_TABLE = process.env.SEND_RESULTS_TABLE;
const EVENT_BUS_NAME = process.env.EVENT_BUS_NAME;

export const handler = async (event) => {
  const method = event.httpMethod;
  const body = method === 'GET' || method === 'DELETE'
    ? event.queryStringParameters || {}
    : JSON.parse(event.body || '{}');
  const action = body.action || event.queryStringParameters?.action;

  try {
    let result;
    switch (action) {
      case 'CREATE_IDENTITY':
        result = await createIdentity(body);
        break;
      case 'DELETE_IDENTITY':
        result = await deleteIdentity(body);
        break;
      case 'GET_IDENTITY':
        result = await getIdentity(body);
        break;
      case 'CREATE_CONFIGSET':
        result = await createConfigSet(body);
        break;
      case 'DELETE_CONFIGSET':
        result = await deleteConfigSet(body);
        break;
      case 'GET_CONFIGSET':
        result = await getConfigSet(body);
        break;
      case 'CREATE_TEMPLATE':
        result = await createTemplate(body);
        break;
      case 'UPDATE_TEMPLATE':
        result = await updateTemplate(body);
        break;
      case 'DELETE_TEMPLATE':
        result = await deleteTemplate(body);
        break;
      case 'GET_TEMPLATE':
        result = await getTemplate(body);
        break;
      case 'LIST_TEMPLATES':
        result = await listTemplates();
        break;
      case 'GET_ACCOUNT':
        result = await getAccount();
        break;
      case 'PUT_VDM':
        result = await putVdm(body);
        break;
      case 'CLEAR_EVENTS':
        result = await clearEvents(body);
        break;
      default:
        return respond(400, { error: `Unknown action: ${action}` });
    }
    return respond(200, result);
  } catch (err) {
    console.error(`Action ${action} failed:`, err);
    return respond(500, { error: err.message });
  }
};

async function createIdentity({ tenantId, domain }) {
  const identity = domain || tenantId;
  const result = await ses.send(new CreateEmailIdentityCommand({
    EmailIdentity: identity,
    Tags: [{ Key: 'tenant_id', Value: tenantId }],
  }));
  return { identity, dkimAttributes: result.DkimAttributes };
}

async function deleteIdentity({ domain }) {
  await ses.send(new DeleteEmailIdentityCommand({ EmailIdentity: domain }));
  return { deleted: domain };
}

async function getIdentity({ domain }) {
  const result = await ses.send(new GetEmailIdentityCommand({ EmailIdentity: domain }));
  return {
    identity: domain,
    verified: result.VerifiedForSendingStatus,
    dkimAttributes: result.DkimAttributes,
  };
}

async function createConfigSet({ tenantId }) {
  const configSetName = `tenant-${tenantId}`;
  await ses.send(new CreateConfigurationSetCommand({
    ConfigurationSetName: configSetName,
    SendingOptions: { SendingEnabled: true },
    Tags: [{ Key: 'tenant_id', Value: tenantId }],
  }));

  // Register in DynamoDB
  await ddb.send(new PutCommand({
    TableName: CONFIG_TABLE,
    Item: {
      tenant_id: tenantId,
      config_set_name: configSetName,
      created_at: new Date().toISOString(),
      ttl: Math.floor(Date.now() / 1000) + 10 * 365 * 86400,
    },
  }));

  return { configSetName };
}

async function deleteConfigSet({ tenantId }) {
  const configSetName = `tenant-${tenantId}`;
  await ses.send(new DeleteConfigurationSetCommand({ ConfigurationSetName: configSetName }));
  await ddb.send(new DeleteCommand({ TableName: CONFIG_TABLE, Key: { tenant_id: tenantId } }));
  return { deleted: configSetName };
}

async function getConfigSet({ tenantId }) {
  const configSetName = `tenant-${tenantId}`;
  try {
    const result = await ses.send(new GetConfigurationSetCommand({ ConfigurationSetName: configSetName }));
    return { configSetName, sendingOptions: result.SendingOptions };
  } catch (err) {
    return { configSetName, tenantId, error: 'Not found' };
  }
}

async function createTemplate({ templateName, subject, htmlBody, textBody }) {
  await ses.send(new CreateEmailTemplateCommand({
    TemplateName: templateName,
    TemplateContent: {
      Subject: subject,
      Html: htmlBody || '',
      Text: textBody || '',
    },
  }));
  return { templateName, created: true };
}

async function updateTemplate({ templateName, subject, htmlBody, textBody }) {
  await ses.send(new UpdateEmailTemplateCommand({
    TemplateName: templateName,
    TemplateContent: {
      Subject: subject,
      Html: htmlBody || '',
      Text: textBody || '',
    },
  }));
  return { templateName, updated: true };
}

async function deleteTemplate({ templateName }) {
  await ses.send(new DeleteEmailTemplateCommand({ TemplateName: templateName }));
  return { templateName, deleted: true };
}

async function getTemplate({ templateName }) {
  const result = await ses.send(new GetEmailTemplateCommand({ TemplateName: templateName }));
  return { templateName, content: result.TemplateContent };
}

async function listTemplates() {
  const result = await ses.send(new ListEmailTemplatesCommand({ PageSize: 100 }));
  return { templates: result.TemplatesMetadata || [] };
}

async function getAccount() {
  const result = await ses.send(new GetAccountCommand({}));
  return {
    maxSendRate: result.SendQuota?.MaxSendRate,
    max24HourSend: result.SendQuota?.Max24HourSend,
    sentLast24Hours: result.SendQuota?.SentLast24Hours,
    sendingEnabled: result.SendingEnabled,
    productionAccessEnabled: result.ProductionAccessEnabled,
    vdmAttributes: result.VdmAttributes,
  };
}

async function putVdm({ enabled }) {
  await ses.send(new PutAccountVdmAttributesCommand({
    VdmAttributes: {
      VdmEnabled: enabled ? 'ENABLED' : 'DISABLED',
      DashboardAttributes: { EngagementMetrics: enabled ? 'ENABLED' : 'DISABLED' },
      GuardianAttributes: { OptimizedSharedDelivery: enabled ? 'ENABLED' : 'DISABLED' },
    },
  }));
  return { vdmEnabled: enabled };
}

async function clearEvents({ tenantId }) {
  // Clear send results for a tenant (for testing)
  const scanResult = await ddb.send(new ScanCommand({
    TableName: RESULTS_TABLE,
    FilterExpression: 'tenant_id = :tid',
    ExpressionAttributeValues: { ':tid': tenantId },
  }));
  let deleted = 0;
  for (const item of scanResult.Items || []) {
    await ddb.send(new DeleteCommand({
      TableName: RESULTS_TABLE,
      Key: { tenant_id: item.tenant_id, correlation_id: item.correlation_id },
    }));
    deleted++;
  }
  return { deleted };
}

function respond(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}
