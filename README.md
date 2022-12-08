# message_sending_api_server

## Current Setting 
> 신규 전송 대기 이메일 체크 주기 - 1 분  
> 신규 전송 대기 이메일 목록 가져오는 개수 - 280 개

> 이메일 전송 결과 이벤트 확인 주기 - 1 분  
> 이메일 전송 결과 이벤트 목록 가져오는 개수 - 300 개

## Features

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
> ㄴ [x] attachment

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

## deployment  
1. gradle bootJar 빌드 -> /build/libs/nte.jar 생성 됩니다.
2. 프로젝트의 deploy/bin 폴더로 복사한 후에 서버의 /svc/nte 경로에 업로드 한다.
3. /svc/nte/script/start.sh 스크립트로 서버를 구동한다.

```
<주의>

/svc/script/start.sh  
   -Dspring.profiles.active={dev or prod} 확인 합니다.
  
/svc/config/nte-config.yml
   nte.jar 내부의 Properties 의 속성 값을 변경하여 서버 구동 합니다.  
   외부 설정 값이 없을 경우 내부의 properties 속성이 반영 됩니다.
   - Database 확인
   - AWS 확인
```

## reference

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
   > https://github.com/pengrad/java-telegram-bot-api/#updating-messages  
   > https://github.com/rubenlagus/TelegramBots

logback appender
- https://github.com/paolodenti/telegram-logback
- https://github.com/maricn/logback-slack-appender