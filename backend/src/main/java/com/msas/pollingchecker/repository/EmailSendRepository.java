package com.msas.pollingchecker.repository;

import com.msas.pollingchecker.model.NewEmailEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Mapper
public interface EmailSendRepository {

    /*
     * 신규 이메일 전송 목록 조회
     */
    @Deprecated
    List<HashMap<String, Objects>> getNewEmail();

    /*
     * 신규 이메일 전송 목록 조회 (using Nested ResultMap)
     */
    List<NewEmailEntity> findNewEmail();

    /*
     * 이메일 전송이 스케쥴러로 이관된 상태 (DBMS → Quartz)
     */
    int UpdateSendEmailStatus2Scheduler(int email_send_dtl_seq,
                                        String send_sts_cd,               // Queued
                                        String stm_last_upd_user_id       // 업데이트 서버 이름
    );

    /*
     * 이메일 전송이 AWS SES 로 이관된 상태
     * Quartz Job -> AWS SES 로 작업 이관된 상태
     */
    int UpdateSendEmailStatus2AWSSES(int email_send_dtl_seq,
                                     String send_sts_cd,               // Sending (성공) or Error (실패)
                                     String send_rslt_typ_cd,          // 발송 결과 코드 (Delivery, Bounce, ...)
                                     String ses_message_id,            // 실제 SES 발송 ID
                                     String stm_last_upd_user_id       // 업데이트 서버 이름
    );

    /*
     * 이메일 최종 전송 결과 상태
     *
     * PollingChecker 서비스에서 DynamoDB 전송 결과 이벤트를 확인하고
     * MariaDB 이메일 상태 정보를 업데이트 한다.
     */
    // correlation_id로 매칭, ses_message_id 별도 저장
    int UpdateFinalEmailStatus(String correlation_id,
                               String send_sts_cd,                    // Delivered/Bounced/Complained/Rejected/Delayed/Error
                               String send_rslt_typ_cd,               // 발송 결과 코드 (Delivery, Bounce, ...)
                               String ses_message_id,                 // 실제 SES 발송 ID
                               String stm_last_upd_user_id            // 업데이트 서버 이름
    );

    /*
     * 이메일 이벤트 이력 저장
     */
    int InsertEmailEventLog(String correlation_id,
                            String ses_message_id,
                            String tenant_id,
                            String event_type,
                            String recipients,
                            String extra_data,
                            String stm_fir_reg_user_id
    );

    /*
     * 메일링 필터링 이메일 등록
     */
    int InsertBlacklistEmail(
            String email_addr,
            String email_cutoff_typ_cd,
            String server_name
    );

}
