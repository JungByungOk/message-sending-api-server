package com.msas.ses.service;

import com.google.gson.GsonBuilder;
import com.msas.pollingchecker.model.SESEventsEntity;
import com.msas.pollingchecker.repository.SESEventsDynamoDBRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("dev")
@SpringBootTest
class MailStatusServiceTest {

    @Autowired
    private SESEventsDynamoDBRepository sesEventsDynamoDBRepository;

    @Test
    @DisplayName("DynamoDB PartiQL Select 쿼리 - 커스텀 태그로 조회 테스트")
    public void getItemByCustomTagFromDynamoDB()
    {
        //given
        String CustomTag = "20221025154600";

        //when
        Optional<List<SESEventsEntity>> resultList = sesEventsDynamoDBRepository.getItemsByCumstomTag(CustomTag);

        //then
        assertThat(resultList)
                .isNotNull()
                .isNotEmpty();

        System.out.printf("[Size=%s]\nResultList=\n%s%n", resultList.map(List::size).orElse(0),
                new GsonBuilder().setPrettyPrinting().create().toJson(resultList));

    }

    @Test
    @DisplayName("DynamoDB PartiQL Select 쿼리 - 전체 조회 테스트")
    public void getItemFromDynamoDB()
    {
        //when
        Optional<List<SESEventsEntity>> resultList = sesEventsDynamoDBRepository.getItems();

        //then
        assertThat(resultList)
                .isNotNull()
                .isNotEmpty();

        System.out.printf("[Size=%s]\nResultList=\n%s%n", resultList.map(List::size).orElse(0),
                new GsonBuilder().setPrettyPrinting().create().toJson(resultList));

    }

    @Test
    @DisplayName("DynamoDB PartiQL Select 쿼리 - MessageId로 아이템 삭제")
    public void DeleteEventItem()
    {

    }

}