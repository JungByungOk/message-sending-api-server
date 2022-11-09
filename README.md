# message_sending_api_server

### feature

[AWS SES]

---templated-----------------

ㄴ list-templates

ㄴ [x] get-template

ㄴ create-template

ㄴ delete-template

ㄴ update-template

ㄴ [x] test-render-template

---send----------------------

ㄴ send-email

>ㄴ [x] attachment

ㄴ html

ㄴ send-templated-email

>ㄴ [x] attachment

---schedule-------------------------
ㄴ send-schedule-templated-email

ㄴ resume

ㄴ cancel

ㄴ job list query

ㄴ job modify

---polling-------------------------
ㄴ check new email from rdbms

ㄴ check sent email result from aws dynamodb

ㄴ registry blacklist email address

>ㄴ [x] filtering blacklist email sending

[Telegram]

ㄴ Multi-channel name setting

ㄴ Send message by channel name

ㄴ GetUpdate Long Polling

[Slack]

ㄴ [x] Multi-channel name setting

ㄴ [x] Send message by channel name

»»» ¯\_(ツ)_/¯

### reference

aws ses
- https://www.rajith.me/2020/02/send-emails-using-aws-simple-email.html
- https://github.com/Rajithkonara/aws-ses-sdk-spring-boot

quartz
- https://github.com/kenshin579/tutorials-java/blob/master/springboot-quartz-in-memory
- http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials

- https://advenoh.tistory.com/52
- https://advenoh.tistory.com/m/55
- https://advenoh.tistory.com/m/56

telegram
 - https://core.telegram.org/bots/api#getting-updates
 
   (1) https://github.com/pengrad/java-telegram-bot-api/#updating-messages

   (2) https://github.com/rubenlagus/TelegramBots

logback appender
- https://github.com/paolodenti/telegram-logback
- https://github.com/maricn/logback-slack-appender