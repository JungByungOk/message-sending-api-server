import * as cdk from 'aws-cdk-lib';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as events from 'aws-cdk-lib/aws-events';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as sqs from 'aws-cdk-lib/aws-sqs';
import * as eventsources from 'aws-cdk-lib/aws-lambda-event-sources';
import { Construct } from 'constructs';

export class EmsV2Stack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);
    cdk.Tags.of(this).add('Project', 'ems');

    // Parameters
    const allowedIps = new cdk.CfnParameter(this, 'AllowedIps', {
      type: 'CommaDelimitedList',
      description: 'ESM 서버 공인 IP 목록',
      default: '0.0.0.0/0',
    });
    const esmServerIp = this.node.tryGetContext('esmServerIp') || process.env.ESM_SERVER_IP || null;

    // ==================== DynamoDB ====================

    const sendResultsTable = new dynamodb.Table(this, 'EmsSendResults', {
      tableName: 'ems-send-results',
      partitionKey: { name: 'tenant_id', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'correlation_id', type: dynamodb.AttributeType.STRING },
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

    const suppressionTable = new dynamodb.Table(this, 'EmsSuppression', {
      tableName: 'ems-suppression',
      partitionKey: { name: 'email', type: dynamodb.AttributeType.STRING },
      sortKey: { name: 'tenant_id', type: dynamodb.AttributeType.STRING },
      billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
      timeToLiveAttribute: 'ttl',
      removalPolicy: cdk.RemovalPolicy.RETAIN,
    });

    // ==================== S3 (대량 발송 수신자 목록) ====================

    const batchBucket = new s3.Bucket(this, 'EmsBatchBucket', {
      bucketName: `ems-batch-${this.account}-${this.region}`,
      lifecycleRules: [{ expiration: cdk.Duration.days(7) }],
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      autoDeleteObjects: true,
    });

    // ==================== SQS ====================

    const sendDlq = new sqs.Queue(this, 'EmsSendDlq', {
      queueName: 'ems-send-dlq',
      retentionPeriod: cdk.Duration.days(14),
    });

    const sendQueue = new sqs.Queue(this, 'EmsSendQueue', {
      queueName: 'ems-send-queue',
      visibilityTimeout: cdk.Duration.seconds(180),
      deadLetterQueue: { queue: sendDlq, maxReceiveCount: 3 },
    });

    // ==================== EventBridge ====================

    const eventBus = new events.EventBus(this, 'EmsEventBus', {
      eventBusName: 'ems-ses-events',
    });

    // Archive for replay (30 days)
    new events.Archive(this, 'EmsEventArchive', {
      sourceEventBus: eventBus,
      archiveName: 'ems-ses-event-archive',
      retention: cdk.Duration.days(30),
      eventPattern: { source: ['aws.ses'] },
    });

    // ==================== Lambda Functions ====================

    // --- email-sender (SQS trigger) ---
    const emailSenderFn = new lambda.Function(this, 'EmsEmailSender', {
      functionName: 'ems-email-sender',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/email-sender'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      reservedConcurrentExecutions: 5,
      environment: {
        SES_REGION: this.region,
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
      },
    });
    emailSenderFn.addEventSource(new eventsources.SqsEventSource(sendQueue, {
      batchSize: 10,
      reportBatchItemFailures: true,
    }));
    sendResultsTable.grantWriteData(emailSenderFn);
    emailSenderFn.addToRolePolicy(new iam.PolicyStatement({
      actions: ['ses:SendEmail', 'ses:SendTemplatedEmail', 'ses:SendRawEmail'],
      resources: ['*'],
    }));

    // --- enqueue (API Gateway trigger) ---
    const enqueueFn = new lambda.Function(this, 'EmsEnqueue', {
      functionName: 'ems-enqueue',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/enqueue'),
      timeout: cdk.Duration.seconds(29),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        SEND_QUEUE_URL: sendQueue.queueUrl,
        TENANT_CONFIG_TABLE: tenantConfigTable.tableName,
        BATCH_BUCKET: batchBucket.bucketName,
      },
    });
    sendQueue.grantSendMessages(enqueueFn);
    tenantConfigTable.grantReadData(enqueueFn);
    batchBucket.grantRead(enqueueFn);

    // --- event-processor (EventBridge trigger - delivery events) ---
    const eventProcessorFn = new lambda.Function(this, 'EmsEventProcessor', {
      functionName: 'ems-event-processor',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/event-processor'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
      },
    });
    sendResultsTable.grantReadWriteData(eventProcessorFn);

    // Rule 2: Delivery events → event-processor
    new events.Rule(this, 'EmsDeliveryEventsRule', {
      ruleName: 'ems-delivery-events',
      eventBus,
      eventPattern: {
        source: ['aws.ses'],
        detailType: ['Send', 'Delivery', 'Open', 'Click', 'Reject', 'DeliveryDelay', 'RenderingFailure'],
      },
      targets: [new targets.LambdaFunction(eventProcessorFn)],
    });

    // --- suppression (EventBridge trigger - bounce/complaint) ---
    const suppressionFn = new lambda.Function(this, 'EmsSuppressionFn', {
      functionName: 'ems-suppression',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/suppression'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
        SUPPRESSION_TABLE: suppressionTable.tableName,
      },
    });
    sendResultsTable.grantReadWriteData(suppressionFn);
    suppressionTable.grantReadWriteData(suppressionFn);

    // Rule 1: Bounce/Complaint → suppression
    new events.Rule(this, 'EmsBounceComplaintRule', {
      ruleName: 'ems-bounce-complaint',
      eventBus,
      eventPattern: {
        source: ['aws.ses'],
        detailType: ['Bounce', 'Complaint'],
      },
      targets: [new targets.LambdaFunction(suppressionFn)],
    });

    // --- tenant-sync (EventBridge trigger - tenant status) ---
    const tenantSyncFn = new lambda.Function(this, 'EmsTenantSync', {
      functionName: 'ems-tenant-sync',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/tenant-sync'),
      timeout: cdk.Duration.seconds(15),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        TENANT_CONFIG_TABLE: tenantConfigTable.tableName,
      },
    });
    tenantConfigTable.grantReadWriteData(tenantSyncFn);

    // Rule 3: Tenant status change → tenant-sync
    new events.Rule(this, 'EmsTenantStatusRule', {
      ruleName: 'ems-tenant-status',
      eventBus,
      eventPattern: {
        source: ['aws.ses'],
        detailType: ['SES Reputation Pause'],
      },
      targets: [new targets.LambdaFunction(tenantSyncFn)],
    });

    // --- tenant-setup (API Gateway trigger) ---
    const tenantSetupFn = new lambda.Function(this, 'EmsTenantSetup', {
      functionName: 'ems-tenant-setup',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/tenant-setup'),
      timeout: cdk.Duration.seconds(30),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        TENANT_CONFIG_TABLE: tenantConfigTable.tableName,
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
        SES_REGION: this.region,
        EVENT_BUS_NAME: eventBus.eventBusName,
      },
    });
    tenantConfigTable.grantReadWriteData(tenantSetupFn);
    sendResultsTable.grantReadWriteData(tenantSetupFn);
    tenantSetupFn.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        'ses:CreateEmailIdentity', 'ses:DeleteEmailIdentity', 'ses:GetEmailIdentity',
        'ses:CreateConfigurationSet', 'ses:DeleteConfigurationSet', 'ses:GetConfigurationSet',
        'ses:CreateConfigurationSetEventDestination', 'ses:UpdateConfigurationSetEventDestination',
        'ses:CreateEmailTemplate', 'ses:GetEmailTemplate', 'ses:UpdateEmailTemplate',
        'ses:DeleteEmailTemplate', 'ses:ListEmailTemplates',
        'ses:GetAccount', 'ses:PutAccountVdmAttributes',
        'ses:TagResource',
        'cloudwatch:GetMetricStatistics', 'cloudwatch:GetMetricData',
        'ce:GetCostAndUsage',
      ],
      resources: ['*'],
    }));

    // --- event-query (API Gateway trigger) ---
    const eventQueryFn = new lambda.Function(this, 'EmsEventQuery', {
      functionName: 'ems-event-query',
      runtime: lambda.Runtime.NODEJS_20_X,
      handler: 'index.handler',
      code: lambda.Code.fromAsset('lambda/event-query'),
      timeout: cdk.Duration.seconds(15),
      memorySize: 256,
      logRetention: logs.RetentionDays.ONE_WEEK,
      environment: {
        SEND_RESULTS_TABLE: sendResultsTable.tableName,
      },
    });
    sendResultsTable.grantReadData(eventQueryFn);

    // ==================== API Gateway ====================

    const api = new apigateway.RestApi(this, 'EmsApiGateway', {
      restApiName: 'ems-api-v2',
      description: 'Joins EMS API Gateway v2',
      deployOptions: {
        stageName: 'prod',
        throttlingRateLimit: 100,
        throttlingBurstLimit: 200,
      },
      policy: new iam.PolicyDocument({
        statements: [
          new iam.PolicyStatement({
            effect: iam.Effect.ALLOW,
            principals: [new iam.AnyPrincipal()],
            actions: ['execute-api:Invoke'],
            resources: ['execute-api:/*/*/*'],
            conditions: {
              IpAddress: {
                'aws:SourceIp': esmServerIp
                  ? esmServerIp.split(',').map((ip: string) => ip.trim())
                  : allowedIps.valueAsList,
              },
            },
          }),
        ],
      }),
    });

    const apiKey = api.addApiKey('EmsApiKey', { apiKeyName: 'ems-api-key-v2' });
    const usagePlan = api.addUsagePlan('EmsUsagePlan', {
      name: 'ems-usage-plan-v2',
      throttle: { rateLimit: 100, burstLimit: 200 },
    });
    usagePlan.addApiKey(apiKey);
    usagePlan.addApiStage({ stage: api.deploymentStage });

    // POST /email-enqueue → Lambda (enqueue)
    const emailEnqueueResource = api.root.addResource('email-enqueue');
    emailEnqueueResource.addMethod('POST', new apigateway.LambdaIntegration(enqueueFn), {
      apiKeyRequired: true,
    });

    // /tenant-setup → Lambda (tenant-setup)
    const tenantSetupResource = api.root.addResource('tenant-setup');
    tenantSetupResource.addMethod('GET', new apigateway.LambdaIntegration(tenantSetupFn), { apiKeyRequired: true });
    tenantSetupResource.addMethod('POST', new apigateway.LambdaIntegration(tenantSetupFn), { apiKeyRequired: true });
    tenantSetupResource.addMethod('PUT', new apigateway.LambdaIntegration(tenantSetupFn), { apiKeyRequired: true });
    tenantSetupResource.addMethod('DELETE', new apigateway.LambdaIntegration(tenantSetupFn), { apiKeyRequired: true });

    // GET /event-query → Lambda (event-query)
    const eventQueryResource = api.root.addResource('event-query');
    eventQueryResource.addMethod('GET', new apigateway.LambdaIntegration(eventQueryFn), { apiKeyRequired: true });

    // ==================== Outputs ====================

    new cdk.CfnOutput(this, 'ApiGatewayUrl', { value: api.url, description: 'API Gateway v2 Endpoint' });
    new cdk.CfnOutput(this, 'ApiKeyId', { value: apiKey.keyId, description: 'API Key ID' });
    new cdk.CfnOutput(this, 'EventBusArn', { value: eventBus.eventBusArn, description: 'EventBridge Bus ARN' });
    new cdk.CfnOutput(this, 'SendQueueUrl', { value: sendQueue.queueUrl, description: 'SQS Send Queue URL' });
    new cdk.CfnOutput(this, 'BatchBucketName', { value: batchBucket.bucketName, description: 'S3 Batch Bucket' });
  }
}
