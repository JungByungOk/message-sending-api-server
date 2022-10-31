package com.msas.ses.service;

import com.msas.pollingchecker.model.NewEmailDetailAttachEntity;
import com.msas.pollingchecker.model.NewEmailDetailEntity;
import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
import com.msas.pollingchecker.types.EnumSESEventTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest
class MariaDBTest {

    @Autowired
    private SESMariaDBRepository sesMariaDBRepository;

    @Test
    @DisplayName("이메일 전송 최종 결과 업데이트 테스트")
    public void finalEmailStatus()
    {
        //given
        EnumSESEventTypeCode enumSESEventTypeCode = EnumSESEventTypeCode.valueOf("Delivery");
        String send_rslt_typ_cd = enumSESEventTypeCode.name();
        String send_sts_cd = enumSESEventTypeCode.getEmailSendStatusCode().name();

        //when
        int n = sesMariaDBRepository.UpdateFinalEmailStatus("4cf6cb32-58d1-11ed-9b6a-0242ac120002", send_sts_cd, send_rslt_typ_cd, "NTS");

        //then
        assertThat(n).isEqualTo(1);
    }

    @Value("${spring.application.name}")
    String server_name;

    @Test
    @DisplayName("이메일 전송 직후 상태 업데이트 테스트")
    public void sendEmailStatus()
    {
        // given
        String ses_message_id = "4cf6cb32-58d1-11ed-9b6a-0242ac120002";

        // when
        int n = sesMariaDBRepository.UpdateSendEmailStatus(14, ses_message_id, server_name);

        // then
        assertThat(n).isEqualTo(1);
    }

    @Test
    @DisplayName("신규 전송 이메일 목록 조회")
    public void getNewEmail()
    {
        // given

        // when
        List<HashMap<String, Objects>> result = sesMariaDBRepository.getNewEmail();

        // then
        assertThat(result).isNotNull().isNotEmpty();

    }

    @Test
    @DisplayName("신규 전송 이메일 목록 조회 - nested resultmap")
    public void findNewEmail()
    {

        // given

        // when
        List<NewEmailEntity> newEmailEntities = sesMariaDBRepository.findNewEmail();

        // then
        assertThat(newEmailEntities).isNotNull().isNotEmpty();

    }


}