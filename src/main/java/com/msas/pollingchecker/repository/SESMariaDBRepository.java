package com.msas.pollingchecker.repository;

import com.msas.pollingchecker.model.AdmEmailSendDtl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface SESMariaDBRepository {

    // ADM_EMAIL_SEND_MST + ADM_EMAIL_SEND_DTL + ADM_EMAIL_ATTACH_FILE_LST => send_sts_cd='SR' 조회
    void getNewEmailBySendSTSCD();

    // 이메일 전송 등록
    // ADM_EMAIL_SEND_MST + ADM_EMAIL_SEND_DTL => email_send_seq={email_send_seq} 업데이트
    @Update("")
    int UpdateSendEmailStatus(int email_send_seq,
                              String send_sts_cd,               // SM - 발송 결과 이벤트 받기 전은 처리중
                              LocalDateTime ses_real_send_dt,   // 발송 일시
                              String ses_msg_id,                // 발송 ID
                              LocalDateTime stm_last_upd_dt,    // 업데이트 시간 (now)
                              String stm_last_upd_user_id       // 업데이트 서버 이름
    );

    // 이메일 전송 결과 상태 등록
    int UpdateSendResult(String messageId,
                         String send_sts_cd,                    // SS (성공) or SF (실패)
                         String send_rslt_typ_cd,               // 발송 결과 코드 (delivery, bounce, ...)
                         LocalDateTime stm_last_upd_dt,         // 업데이트 시간
                         String stm_last_upd_user_id            // 업데이트 서버 이름
                         );

}
