package com.msas.telegram.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.msas.telegram.dto.RequestTelegramMessageDTO;
import com.msas.telegram.service.TelegramBotService;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;

@Tag(name = "Telegram", description = "Telegram 봇 메시지 발송 API")
@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramBotController {

    private final TelegramBotService telegramBotService;

    @Operation(summary = "메시지 발송", description = "지정한 Telegram 채널로 메시지를 발송합니다.")
    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody RequestTelegramMessageDTO requestTelegramMessageDTO)
    {
        Message message = telegramBotService.SendMessage(requestTelegramMessageDTO.getChannelName(), requestTelegramMessageDTO.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(message), headers, HttpStatus.OK);
    }

    @Operation(summary = "봇 정보 조회", description = "Telegram 봇의 설정 정보를 조회합니다.")
    @GetMapping("/info")
    public ResponseEntity<String> getBotInfo()
    {
        User user = telegramBotService.GetMe();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(user), headers, HttpStatus.OK);
    }

    @Operation(summary = "채널 ID 조회", description = "등록된 Telegram 채널명과 ID 매핑 목록을 조회합니다.")
    @GetMapping("/ids")
    public ResponseEntity<String> getChatIds()
    {
        HashMap<String, Long> ids = telegramBotService.GetChetIds();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(ids), headers, HttpStatus.OK);
    }

}
