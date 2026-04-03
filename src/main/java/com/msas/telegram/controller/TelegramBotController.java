package com.msas.telegram.controller;

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

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramBotController {

    private final TelegramBotService telegramBotService;

    /**
     * 채팅 메시지 전송
     */
    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(@Valid @RequestBody RequestTelegramMessageDTO requestTelegramMessageDTO)
    {
        Message message = telegramBotService.SendMessage(requestTelegramMessageDTO.getChannelName(), requestTelegramMessageDTO.getMessage());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(message), headers, HttpStatus.OK);
    }

    /**
     * 채팅 봇 설정 정보 조회
     */
    @GetMapping("/info")
    public ResponseEntity<String> getBotInfo()
    {
        User user = telegramBotService.GetMe();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(user), headers, HttpStatus.OK);
    }

    /**
     * ChatId 정보 조회
     */
    @GetMapping("/ids")
    public ResponseEntity<String> getChatIds()
    {
        HashMap<String, Long> ids = telegramBotService.GetChetIds();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(new Gson().toJson(ids), headers, HttpStatus.OK);
    }

}
