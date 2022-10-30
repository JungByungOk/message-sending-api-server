package com.msas.pollingchecker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AdmEmailAttachFileLst {

    private long email_attach_file_seq;
    private int email_send_dtl_seq;

    private String file_path;
    private String file_nm;
    private String orgn_file_nm;
    private long file_sz;

    private LocalDateTime stm_fir_reg_dt;
    private String stm_fir_reg_user_id;
    private LocalDateTime stm_last_upd_dt;
    private String stm_last_upd_user_id;

}
