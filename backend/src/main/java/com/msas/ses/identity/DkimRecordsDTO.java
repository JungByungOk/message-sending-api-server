package com.msas.ses.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DkimRecordsDTO {
    private String domain;
    private String verificationStatus;
    private List<DkimRecordDTO> dkimRecords;
}
