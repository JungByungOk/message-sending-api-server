package com.msas.ses.service;

import com.google.gson.GsonBuilder;
import com.msas.pollingchecker.model.SESEventsEntity;
import com.msas.pollingchecker.repository.SESDynamoDBRepository;
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
class DynamoDBTest {

    @Autowired
    private SESDynamoDBRepository sesEventsDynamoDBRepository;

    @Test
    @DisplayName("DynamoDB PartiQL Select 쿼리 - 커스텀 태그로 조회 테스트")
    void getItemByCustomTagFromDynamoDB()
    {
        //given
        String CustomTag = "20221025154600";

        //when
        Optional<List<SESEventsEntity>> resultList = sesEventsDynamoDBRepository.getItemsByCustomTag(CustomTag);

        //then
        assertThat(resultList)
                .isNotNull()
                .isNotEmpty();

        System.out.printf("[Size=%s]\nResultList=\n%s%n", resultList.map(List::size).orElse(0),
                new GsonBuilder().setPrettyPrinting().create().toJson(resultList));
    }

    @Test
    @DisplayName("DynamoDB PartiQL Select 쿼리 - 전체 조회 테스트")
    void getItemFromDynamoDB()
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
    void DeleteEventItem()
    {
        // given
        int result;

        // when
        result = sesEventsDynamoDBRepository.deleteItemBySESMessageIdAndSnsPublishTime(
                "010c01840de996c1-1ca9fd94-a363-4df5-92f3-2b6871ee4017-000000",
                "2022-10-25T06:52:05.154Z"
        );

        // then
        assertThat(result).isEqualTo(1);
    }

}