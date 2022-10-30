package com.msas.ses.service;

import com.msas.pollingchecker.repository.SESMariaDBRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev58")
@SpringBootTest
class MariaDBTest {

    @Autowired
    private SESMariaDBRepository sesMariaDBRepository;

    @Test
    @DisplayName("마스터 조회 테스트")
    public void findAllFromMariaDB()
    {
        //given

        //when

        //then

    }


}