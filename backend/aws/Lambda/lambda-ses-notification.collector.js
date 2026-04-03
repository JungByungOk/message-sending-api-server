//--------------------------Lambda Code Begins------------------------
console.log("Loading event");

var aws = require("aws-sdk");
var ddb = new aws.DynamoDB({ params: { TableName: "SESNotifications" } });

exports.handler = function (event, context, callback) {
  console.log("Received event:", JSON.stringify(event, null, 2));

  var SnsPublishTime = event.Records[0].Sns.Timestamp;
  var SnsTopicArn = event.Records[0].Sns.TopicArn;
  var SESMessage = event.Records[0].Sns.Message;

  SESMessage = JSON.parse(SESMessage);

  var SESMessageType = SESMessage.notificationType;
  var SESMessageId = SESMessage.mail.messageId;
  var SESDestinationAddress = SESMessage.mail.destination.toString();
  var LambdaReceiveTime = new Date().toString();

  if (SESMessageType == "Bounce") {
    var SESreportingMTA = SESMessage.bounce.reportingMTA;
    var SESbounceSummary = JSON.stringify(SESMessage.bounce.bouncedRecipients);
    var itemParams = {
      Item: {
        SESMessageId: { S: SESMessageId },
        SnsPublishTime: { S: SnsPublishTime },
        SESreportingMTA: { S: SESreportingMTA },
        SESDestinationAddress: { S: SESDestinationAddress },
        SESbounceSummary: { S: SESbounceSummary },
        SESMessageType: { S: SESMessageType },
      },
    };
    ddb.putItem(itemParams, function (err, data) {
      if (err) {
        callback(err)
      } else {
        console.log(data);
        callback(null,'')
      }
    });
  } else if (SESMessageType == "Delivery") {
    var SESsmtpResponse1 = SESMessage.delivery.smtpResponse;
    var SESreportingMTA1 = SESMessage.delivery.reportingMTA;
    var itemParamsdel = {
      Item: {
        SESMessageId: { S: SESMessageId },
        SnsPublishTime: { S: SnsPublishTime },
        SESsmtpResponse: { S: SESsmtpResponse1 },
        SESreportingMTA: { S: SESreportingMTA1 },
        SESDestinationAddress: { S: SESDestinationAddress },
        SESMessageType: { S: SESMessageType },
      },
    };
    ddb.putItem(itemParamsdel, function (err, data) {
      if (err) {
        callback(err)
      } else {
        console.log(data);
        callback(null,'')
      }
    });
  } else if (SESMessageType == "Complaint") {
    var SESComplaintFeedbackType = SESMessage.complaint.complaintFeedbackType;
    var SESFeedbackId = SESMessage.complaint.feedbackId;
    var itemParamscomp = {
      Item: {
        SESMessageId: { S: SESMessageId },
        SnsPublishTime: { S: SnsPublishTime },
        SESComplaintFeedbackType: { S: SESComplaintFeedbackType },
        SESFeedbackId: { S: SESFeedbackId },
        SESDestinationAddress: { S: SESDestinationAddress },
        SESMessageType: { S: SESMessageType },
      },
    };
    ddb.putItem(itemParamscomp, function (err, data) {
      if (err) {
        callback(err)
      } else {
        console.log(data);
        callback(null,'')
      }
    });
  }
};
//------------------------Lambda Code Ends----------------------------