var aws = require("aws-sdk");
var DynamoDB = new aws.DynamoDB.DocumentClient();

exports.handler = function (event, context, callback) {
  console.info("[RECV] EVENT\n" + JSON.stringify(event, null, 2));
  
  var SnsPublishTime = event.Records[0].Sns.Timestamp;
  
  var SESMessage = event.Records[0].Sns.Message;

  SESMessage = JSON.parse(SESMessage);

  var SESEventType = SESMessage.eventType;
  var SESMessageId = SESMessage.mail.messageId;
  var SESDestinationAddress = SESMessage.mail.destination[0].toString();
  
  var SESCustomTag = SESMessage.mail.tags['customTag'];
  
  if(SESCustomTag != null){
    SESCustomTag = SESMessage.mail.tags['customTag'].toString();
    console.info("CustomTag\n" + SESCustomTag.toString());
  }

  var params = {
    TableName: "SESEvents",
    Item: {
      SESMessageId: SESMessageId,
      SnsPublishTime: SnsPublishTime,
      
      DestinationEmail: SESDestinationAddress,

      EventType: SESEventType,
      CustomTag: SESCustomTag,
      Message: SESMessage
    }
  };

  DynamoDB.put(params, function (err) {
    console.info("[PUT] SES Event to Dynamo");
    if (err) {
      console.log(err);
      callback(err);
    }
    else {
      callback(null, '');
    }
  });

};