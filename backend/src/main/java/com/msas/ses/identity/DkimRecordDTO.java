package com.msas.ses.identity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DkimRecordDTO {
    private String name;
    private String type;
    private String value;
}
