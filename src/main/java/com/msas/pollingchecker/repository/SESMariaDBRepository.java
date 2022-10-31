package com.msas.pollingchecker.repository;

import com.msas.pollingchecker.model.NewEmailEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Mapper
public interface SESMariaDBRepository {

    /*
     * 신규 이메일 전송 목록 조회
     *
     * PollingNewEmailFromNFTDB 서비스에서 MariaDB를 확인한다.
     */
    @Deprecated
    List<HashMap<String, Objects>> getNewEmail();

    /*
     * 신규 이메일 전송 목록 조회 (using Nested ResultMap)
     *
     * PollingNewEmailFromNFTDB 서비스에서 MariaDB를 확인한다.
     */
    List<NewEmailEntity> findNewEmail();

    /*
     * 이메일 전송 직후 결과 등록
     *
     * PollingNewEmailFromNFTDB 서비스에서
     * 이메일 전송 건당 발송 처리 결과를 MariaDB에 업데이트 한다.
     */
    int UpdateSendEmailStatus(int email_send_dtl_seq,
                              String ses_msg_id,                // 발송 ID
                              String stm_last_upd_user_id       // 업데이트 서버 이름
    );

    /*
     * 이메일 최종 전송 결과 상태 등록한다.
     *
     * PollingChecker 서비스에서 DynamoDB 전송 결과 이벤트를 확인하고
     * MariaDB 이메일 상태 정보를 업데이트 한다.
     */
    int UpdateFinalEmailStatus(String ses_msg_id,
                               String send_sts_cd,                    // SS (성공) or SF (실패)
                               String send_rslt_typ_cd,               // 발송 결과 코드 (delivery, bounce, ...)
                               String stm_last_upd_user_id            // 업데이트 서버 이름
                         );

}
