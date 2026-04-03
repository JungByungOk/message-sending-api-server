package com.msas.pollingchecker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class NewEmailDetailEntity {

    private int email_send_dtl_seq;
    private int email_send_seq;

    private String send_sts_cd;
    private LocalDateTime ses_real_send_dt;
    private String send_rslt_typ_cd;
    private String rcv_email_addr;
    private String send_email_addr;
    private String email_title;
    private String email_cts;
    private String email_tmplet_id;
    private String ses_msg_id;

    private LocalDateTime stm_fir_reg_dt;
    private String stm_fir_reg_user_id;
    private LocalDateTime stm_last_upd_dt;
    private String stm_last_upd_user_id;

    private List<NewEmailDetailAttachEntity> newEmailDetailAttachEntities;

}
