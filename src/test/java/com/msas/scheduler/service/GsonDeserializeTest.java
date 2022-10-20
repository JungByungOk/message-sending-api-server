package com.msas.scheduler.service;

class GsonDeserializeTest {

    /*
        테스트 JSON 스트링
        {
            "jobName": "test-job15",
            "jobGroup": "test-group1",
            "description": "스케쥴 작업 등록 테스트",
            "startDateAt": "2022-10-21T11:02:00",
            "templateName": "TEST-Template-Welcome",
            "from": "no-reply@nftreally.io",
            "templatedEmailList": [
                {
                    "to": [
                        "jbo2541@gmail.com"
                    ],
                    "cc": [
                        "jbo2541@outlook.com"
                    ],
                    "bcc": [
                        "bojung@digicaps.com"
                    ],
                    "templateData": {
                        "user_name": "Jung Byung Ok",
                        "nickname": "Leo",
                        "membership": "Standard"
                    }
                }
            ],
            "tags": [
                {
                    "name": "promotion",
                    "value": "welcome promotion-#1"
                }
            ]
        }
     */

//    @Test
//    void deserialize() {
//
//        String strJson = "{\\n    \"jobName\": \"test-job15\",\\n    \"jobGroup\": \"test-group1\",\\n    \"description\": \"\\uc2a4\\ucf00\\uc974 \\uc791\\uc5c5 \\ub4f1\\ub85d \\ud14c\\uc2a4\\ud2b8\",\\n    \"startDateAt\": \"2022-10-21T11:02:00\",\\n    \"templateName\": \"TEST-Template-Welcome\",\\n    \"from\": \"no-reply@nftreally.io\",\\n    \"templatedEmailList\": [\\n        {\\n            \"to\": [\\n                \"jbo2541@gmail.com\"\\n            ],\\n            \"cc\": [\\n                \"jbo2541@outlook.com\"\\n            ],\\n            \"bcc\": [\\n                \"bojung@digicaps.com\"\\n            ],\\n            \"templateData\": {\\n                \"user_name\": \"Jung Byung Ok\",\\n                \"nicname\": \"Leo\",\\n                \"membership\": \"Standard\"\\n            }\\n        }\\n    ],\\n    \"tags\": [\\n        {\\n            \"name\": \"promotion\",\\n            \"value\": \"welcome promotion-#1\"\\n        }\\n    ]\\n}";
//
//        List<XTemplatedEmailDto> templatedEmailDtoList = new ArrayList<>();
//
//        RequestTemplatedEmailScheduleJobDTO requestTemplatedEmailScheduleJobDTO = new RequestTemplatedEmailScheduleJobDTO();
//        requestTemplatedEmailScheduleJobDTO.setJobName("job1");
//        requestTemplatedEmailScheduleJobDTO.setJobGroup("group1");
//        requestTemplatedEmailScheduleJobDTO.setDescription("jobdatamap deserialize");
//        requestTemplatedEmailScheduleJobDTO.setStartDateAt(LocalDateTime.now());
//        {
//            XTemplatedEmailDto templatedEmailDto = new XTemplatedEmailDto();
//            templatedEmailDto.setTemplateName("template1");
//            templatedEmailDto.setTags());
//        }
//        requestTemplatedEmailScheduleJobDTO.setTemplatedEmailList();
//    }
}