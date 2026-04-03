package com.msas.pollingchecker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class NewEmailEntity {

    private int email_send_seq;

    private String email_typ_cd;
    private String email_cls_cd;
    private String send_div_cd;
    private LocalDateTime rsv_send_dt;
    private String auto_send_yn;

    private LocalDateTime stm_fir_reg_dt;
    private String stm_fir_reg_user_id;
    private LocalDateTime stm_last_upd_dt;
    private String stm_last_upd_user_id;

    private List<NewEmailDetailEntity> newEmailDetailEntities;

}
