package com.msas.ses.service;

import com.msas.pollingchecker.model.NewEmailEntity;
import com.msas.pollingchecker.repository.SESMariaDBRepository;
import com.msas.pollingchecker.types.EnumSESEventTypeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest
class MariaDBTest {

    @Value("${spring.application.name}")
    String serverName;

    @Autowired
    private SESMariaDBRepository sesMariaDBRepository;

    @Test
    @DisplayName("이메일 전송 최종 결과 업데이트 테스트")
    public void finalEmailStatus()
    {
        //given
        EnumSESEventTypeCode enumSESEventTypeCode = EnumSESEventTypeCode.valueOf("Delivery");
        String send_rslt_typ_cd = enumSESEventTypeCode.name().toUpperCase(Locale.ENGLISH);
        String send_sts_cd = enumSESEventTypeCode.getEmailSendStatusCode().name();

        //when
        int n = sesMariaDBRepository.UpdateFinalEmailStatus("4cf6cb32-58d1-11ed-9b6a-0242ac120002", send_sts_cd, send_rslt_typ_cd, "NTS");

        //then
        assertThat(n).isEqualTo(1);
    }

    @Test
    @DisplayName("이메일 전송 SES 이관 상태 업데이트 테스트")
    public void sendEmailStatus()
    {
        // given
        String ses_message_id = "4cf6cb32-58d1-11ed-9b6a-0242ac120002";
        String send_sts_cd = "SM";

        // when
        int n = sesMariaDBRepository.UpdateSendEmailStatus2AWSSES(14, send_sts_cd, null, ses_message_id, serverName);

        // then
        assertThat(n).isEqualTo(1);
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

    @Test
    @DisplayName("전송 결과 최종 상태 업데이트")
    public void UpdateFinalEmailStatusTest()
    {

        // given
        String ses_message_id = "4cf6cb32-58d1-11ed-9b6a-0242ac120002";
        String send_sts_cd = "SX";
        String send_rslt_typ_cd = "DELIVERY";

        // when
        int result = sesMariaDBRepository.UpdateFinalEmailStatus(ses_message_id, send_sts_cd, send_rslt_typ_cd, serverName);

        // then
        assertThat(result).isEqualTo(1);

    }

    @Test
    @DisplayName("블랙리스트 이메일 등록")
    public void addBlacklist()
    {
        // given
        int result = 0;

        // when
        try {

            result = sesMariaDBRepository.InsertBlacklistEmail("test1@joins.com", "BOUNCE", serverName);

        }
        catch(DuplicateKeyException e)
        {
            e.getMessage();
        }

        // then
        assertThat(result).isEqualTo(1);

    }

}