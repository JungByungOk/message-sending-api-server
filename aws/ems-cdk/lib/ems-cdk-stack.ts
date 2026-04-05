import * as cdk from 'aws-cdk-lib';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as sns from 'aws-cdk-lib/aws-sns';
import * as snsSubscriptions from 'aws-cdk-lib/aws-sns-subscriptions';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as eventsources from 'aws-cdk-lib/aws-lambda-event-sources';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import * as ses from 'aws-cdk-lib/aws-ses';
import * as sesActions from 'aws-cdk-lib/aws-ses-actions';
import { Construct } from 'constructs';

export class EmsCdkStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // ============================================================
    // DynamoDB Tables
    // ============================================================

    const sendResultsTable = new dynamodb.Table(this, 'EmsSendResults', {
      tableName: 'ems-send-results',
      partitionKey: { name: 'tenant_id', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'message_id', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      timeToLiveAttribute: 'ttl',
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });
    sendResultsTable.addGlobalSecondaryIndex({
      indexName: 'gsi-tenant-timestamp',
      partitionKey: { name: 'tenant_id', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'timestamp', type: dynamodb.AttributeType.STRING },
      projectionType: dynamodb.ProjectionType.ALL,
    });

    const tenantConfigTable = new dynamodb.Table(this, 'EmsTenantConfig', {
      tableName: 'ems-tenant-config',
      partitionKey: { name: 'tenant_id', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      timeToLiveAttribute: 'ttl',
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    const idempotencyTable = new dynamodb.Table(this, 'EmsIdempotency', {
      tableName: 'ems-idempotency',
      partitionKey: { name: 'message_id', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      timeToLiveAttribute: 'ttl',
      removalPolicy: cdk.RemovalPolicy.DESTROY,
    });

    // ============================================================
    // SSM Parameter Store (기본값)
    // ============================================================

    const ssmMode = new ssm.StringParameter(this, 'EmsMode', {
      parameterName: '/ems/mode',
      stringValue: 'callback',
      description: '발송결과 수신 모드 (callback / polling)',
    });

    const ssmCallbackUrl = new ssm.StringParameter(this, 'EmsCallbackUrl', {
      parameterName: '/ems/callback_url',
      stringValue: '',
      description: 'ESM Callback URL',
    });

    const ssmCallbackSecret = new ssm.StringParameter(this, 'EmsCallbackSecret', {
      parameterName: '/ems/callback_secret',
      stringValue: '',
      description: 'Callback Secret Token',
      type: ssm.ParameterType.STRING,
    });

    // ============================================================
    // SQS - 발송 큐
    // ============================================================

    const sendDlq = new sqs.Queue(this, 'EmsSendDlq', {
      queueName: 'ems-send-dlq',
      retentionPeriod: cdk.Duration.days(14),
    });

    const sendQueue = new sqs.Queue(this, 'EmsSendQueue', {
      queueName: 'ems-send-queue',
      visibilityTimeout: cdk.Duration.seconds(60),
      deadLetterQueue: {
        queue: sendDlq,
        maxReceiveCount: 3,
      },
    });

    // ============================================================
    // SNS - SES 이벤트 토픽
    // ============================================================

    const sesEventTopic = new sns.Topic(this, 'EmsSesEventTopic', {
      topicName: 'ems-ses-events',
      displayName: 'EMS SES Event Notifications',
    });

    // ============================================================
    // Lambda - email-sender
    // ============================================================

    const emailSenderFn = new lambda.Function(this, 'EmsEmailSender', {
      functionName: 'ems-email-sender',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/email-sender'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      environment: {
        TENANT_CONFIG_TABLE: tenantConfigTable.tableName,
        IDEMPOTENCY_TABLE: idempotencyTable.tableName,
        SES_REGION: this.region,
      },
    });
    emailSenderFn.addEventSource(new eventsources.SqsEventSource(sendQueue, {
      batchSize: 10,
    }));
    tenantConfigTable.grantReadData(emailSenderFn);
    idempotencyTable.grantReadWriteData(emailSenderFn);
    emailSenderFn.addToRolePolicy(new iam.PolicyStatement({
      actions: ['ses:SendEmail', 'ses:SendTemplatedEmail', 'ses:SendRawEmail'],
      resources: ['*'],
    }));

    // ============================================================
    // Lambda - event-processor
    // ============================================================

    const eventProcessorDlq = new sqs.Queue(this, 'EmsEventProcessorDlq', {
      queueName: 'ems-event-processor-dlq',
      retentionPeriod: cdk.Duration.days(14),
    });

    const eventProcessorFn = new lambda.Function(this, 'EmsEventProcessor', {
      functionName: 'ems-event-processor',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/event-processor'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      deadLetterQueue: eventProcessorDlq,
      environment: {
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
        SSM_MODE_PARAM: ssmMode.parameterName,
        SSM_CALLBACK_URL_PARAM: ssmCallbackUrl.parameterName,
        SSM_CALLBACK_SECRET_PARAM: ssmCallbackSecret.parameterName,
      },
    });
    sesEventTopic.addSubscription(new snsSubscriptions.LambdaSubscription(eventProcessorFn));
    sendResultsTable.grantReadWriteData(eventProcessorFn);
    ssmMode.grantRead(eventProcessorFn);
    ssmCallbackUrl.grantRead(eventProcessorFn);
    ssmCallbackSecret.grantRead(eventProcessorFn);

    // ============================================================
    // Lambda - event-query
    // ============================================================

    const eventQueryFn = new lambda.Function(this, 'EmsEventQuery', {
      functionName: 'ems-event-query',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/event-query'),
      timeout: cdk.Duration.seconds(15),
      memorySize: 256,
      environment: {
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
      },
    });
    sendResultsTable.grantReadData(eventQueryFn);

    // ============================================================
    // Lambda - tenant-setup
    // ============================================================

    const tenantSetupFn = new lambda.Function(this, 'EmsTenantSetup', {
      functionName: 'ems-tenant-setup',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/tenant-setup'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      environment: {
        TENANT_CONFIG_TABLE: tenantConfigTable.tableName,
        SES_EVENT_TOPIC_ARN: sesEventTopic.topicArn,
        SES_REGION: this.region,
      },
    });
    tenantConfigTable.grantReadWriteData(tenantSetupFn);
    tenantSetupFn.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ses:CreateEmailIdentity', 'ses:DeleteEmailIdentity', 'ses:GetEmailIdentity',
        'ses:CreateConfigurationSet', 'ses:DeleteConfigurationSet', 'ses:GetConfigurationSet',
        'ses:CreateConfigurationSetEventDestination',
        'ses:CreateEmailTemplate', 'ses:UpdateEmailTemplate', 'ses:DeleteEmailTemplate', 'ses:ListEmailTemplates',
      ],
      resources: ['*'],
    }));
    tenantSetupFn.addToRolePolicy(new iam.PolicyStatement({
      actions: ['sns:Subscribe', 'sns:Unsubscribe'],
      resources: [sesEventTopic.topicArn],
    }));

    // ============================================================
    // API Gateway
    // ============================================================

    const api = new apigateway.RestApi(this, 'EmsApiGateway', {
      restApiName: 'ems-api',
      description: 'Joins EMS API Gateway',
      deployOptions: {
        stageName: 'prod',
        throttlingRateLimit: 100,
        throttlingBurstLimit: 200,
      },
      defaultCorsPreflightOptions: {
        allowOrigins: apigateway.Cors.ALL_ORIGINS,
        allowMethods: apigateway.Cors.ALL_METHODS,
      },
    });

    // API Key 인증
    const apiKey = api.addApiKey('EmsApiKey', {
      apiKeyName: 'ems-api-key',
      description: 'ESM Server API Key',
    });

    const usagePlan = api.addUsagePlan('EmsUsagePlan', {
      name: 'ems-usage-plan',
      throttle: { rateLimit: 100, burstLimit: 200 },
    });
    usagePlan.addApiKey(apiKey);
    usagePlan.addApiStage({ stage: api.deploymentStage });

    // POST /send-email → SQS
    const sendEmailResource = api.root.addResource('send-email');
    const sqsIntegration = new apigateway.AwsIntegration({
      service: 'sqs',
      path: `${this.account}/${sendQueue.queueName}`,
      integrationHttpMethod: 'POST',
      options: {
        credentialsRole: new iam.Role(this, 'ApiGatewaySqsRole', {
          assumedBy: new iam.ServicePrincipal('apigateway.amazonaws.com'),
          inlinePolicies: {
            sqs: new iam.PolicyDocument({
              statements: [new iam.PolicyStatement({
                actions: ['sqs:SendMessage'],
                resources: [sendQueue.queueArn],
              })],
            }),
          },
        }),
        requestParameters: {
          'integration.request.header.Content-Type': "'application/x-www-form-urlencoded'",
        },
        requestTemplates: {
          'application/json': 'Action=SendMessage&MessageBody=$util.urlEncode($input.body)',
        },
        integrationResponses: [{
          statusCode: '200',
          responseTemplates: {
            'application/json': '{"messageId": "$input.path(\'$.SendMessageResponse.SendMessageResult.MessageId\')"}',
          },
        }],
      },
    });
    sendEmailResource.addMethod('POST', sqsIntegration, {
      apiKeyRequired: true,
      methodResponses: [{ statusCode: '200' }],
    });

    // GET /results → Lambda (event-query)
    const resultsResource = api.root.addResource('results');
    resultsResource.addMethod('GET', new apigateway.LambdaIntegration(eventQueryFn), {
      apiKeyRequired: true,
    });

    // PUT /config → SSM Parameter Store (Lambda proxy)
    const configResource = api.root.addResource('config');
    const configFn = new lambda.Function(this, 'EmsConfigUpdater', {
      functionName: 'ems-config-updater',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromInline(`
const { SSMClient, PutParameterCommand } = require('@aws-sdk/client-ssm');
const ssm = new SSMClient({});
exports.handler = async (event) => {
  const body = JSON.parse(event.body || '{}');
  const params = [
    { name: '/ems/mode', value: body.mode || 'callback' },
    { name: '/ems/callback_url', value: body.callback_url || '' },
    { name: '/ems/callback_secret', value: body.callback_secret || '' },
  ];
  for (const p of params) {
    await ssm.send(new PutParameterCommand({
      Name: p.name, Value: p.value, Type: 'String', Overwrite: true,
    }));
  }
  return { statusCode: 200, body: JSON.stringify({ message: 'Config updated' }) };
};
      `),
      timeout: cdk.Duration.seconds(10),
    });
    ssmMode.grantWrite(configFn);
    ssmCallbackUrl.grantWrite(configFn);
    ssmCallbackSecret.grantWrite(configFn);
    configResource.addMethod('PUT', new apigateway.LambdaIntegration(configFn), {
      apiKeyRequired: true,
    });

    // /tenant-setup → Lambda (tenant-setup)
    const tenantSetupResource = api.root.addResource('tenant-setup');
    tenantSetupResource.addMethod('GET', new apigateway.LambdaIntegration(tenantSetupFn), {
      apiKeyRequired: true,
    });
    tenantSetupResource.addMethod('POST', new apigateway.LambdaIntegration(tenantSetupFn), {
      apiKeyRequired: true,
    });
    tenantSetupResource.addMethod('DELETE', new apigateway.LambdaIntegration(tenantSetupFn), {
      apiKeyRequired: true,
    });

    // ============================================================
    // Outputs
    // ============================================================

    new cdk.CfnOutput(this, 'ApiGatewayUrl', {
      value: api.url,
      description: 'API Gateway Endpoint URL',
    });

    new cdk.CfnOutput(this, 'ApiKeyId', {
      value: apiKey.keyId,
      description: 'API Key ID (콘솔에서 값 확인)',
    });

    new cdk.CfnOutput(this, 'SesEventTopicArn', {
      value: sesEventTopic.topicArn,
      description: 'SNS Topic ARN for SES events',
    });

    new cdk.CfnOutput(this, 'SendQueueUrl', {
      value: sendQueue.queueUrl,
      description: 'SQS Send Queue URL',
    });
  }
}
