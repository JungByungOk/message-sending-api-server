package com.msas.pollingchecker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Deprecated
public class AdmEmailBlMst {

    private String email_addr;
    private String email_cutoff_typ_cd;
    private String email_cutoff_yn;

    private LocalDateTime stm_fir_reg_dt;
    private String stm_fir_reg_user_id;
    private LocalDateTime stm_last_upd_dt;
    private String stm_last_upd_user_id;

}
