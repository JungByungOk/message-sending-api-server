package com.msas.pollingchecker.service;

import com.msas.pollingchecker.repository.SESEventsDynamoDBRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SESStatusService {

    private final SESEventsDynamoDBRepository sesEventsDynamoDBRepository;

    public void UpdateEmailResult(String sesMessageId)
    {

        //  1) rdbms 상태 변경


        //  2) dynamo 삭제
        sesEventsDynamoDBRepository.deleteItemBySESMessageId(sesMessageId);

    }

    public void UpdateMessageId(String emailSendDtlSeq, String messageId)
    {
        /*
         * UPDATE adm_email_send_dtl
         * SET SES_MSG_ID = "{0}"
         * WHERE EMAIL_SEND_DTL_SEQ = {1};
         */
    }
}
