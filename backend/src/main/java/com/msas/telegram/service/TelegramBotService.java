package com.msas.telegram.service;

import com.google.gson.GsonBuilder;
import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.TelegramException;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    private final TelegramBot telegramBot;
    // channelName : channelId mapping data ...
    private final HashMap<String, Long> channelMap = new HashMap<>();
    @Value("${bot.telegram.channel-names}")
    private List<String> channelNames = new ArrayList<>();

    @PostConstruct
    public void init() {
        //+++++++++++++++++++++++++++++++++++++++
        // /getUpdates 리스너 등록
        // bot_command = /start 확인하고, 채널 채팅방 이름 확인하여 채널 아이디 저장
        //+++++++++++++++++++++++++++++++++++++++
        telegramBot.setUpdatesListener(new UpdatesListener() {
            @Override
            public int process(List<Update> updates) {
                log.info("[UpdatesListener] Receive Message\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(updates.get(0)));

                try {
                    // channelPost & /start & title=channelName 이면, chatId 저장
                    if (updates.get(0).channelPost() != null)
                        if (updates.get(0).channelPost().text().toLowerCase().compareTo("/start") == 0) {
                            for (String channelName : channelNames) {
                                if (updates.get(0).channelPost().chat().title().compareTo(channelName) == 0)
                                    channelMap.put(channelName, updates.get(0).channelPost().chat().id());
                            }
                        }
                } catch (Exception e) {
                    log.error("chat().id() update skip...");
                } finally {
                    log.info("[UpdatesListener] channelInfo = {}", channelMap);
                }

                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            }
        }, new ExceptionHandler() {
            @Override
            public void onException(TelegramException e) {
                /*
                https://github.com/yagop/node-telegram-bot-api/issues/488
                com.pengrad.telegrambot.TelegramException: GetUpdates failed with error_code 409 Conflict: terminated by other getUpdates request; make sure that only one bot instance is running
                이 오류는 동일한 봇 토큰을 사용하는 봇의 인스턴스가 1개 이상일 때 발생하며, 이는 Telegram API 에서 오류를 유발합니다.
                이것이 봇 토큰을 변경하거나 다른 작업을 닫으면 이 오류가 해결되는 이유입니다.
                */
                log.warn(e.toString());
            }
        });
    }

    /**
     * Bot 정보 가져오기
     */
    public User GetMe() {
        GetMe request = new GetMe();
        GetMeResponse getMeResponse = telegramBot.execute(request);
        return getMeResponse.user();
    }

    /**
     * ChatId 정보 가져오기
     */
    public HashMap<String, Long> GetChetIds() {
        return channelMap;
    }

    /**
     * 지정한 채널로 Message 보내기
     */
    public Message SendMessage(String channelName, String text) {
        if (channelMap.isEmpty()) {
            // /start 시작한 채널이 없습니다.
            return new Message();
        }

        if (!channelMap.containsKey(channelName)) {
            // 일치하는 채널이 없습니다.
            return new Message();
        }

        return SendMessage(channelMap.get(channelName), text);
    }

    protected Message SendMessage(long id, String text) {
        // method.1
        //SendResponse sendResponse = telegramBot.execute(new SendMessage(chatId, "Hello!"));

        // method.2
        SendMessage request = new SendMessage(id, text)
                //.parseMode(ParseMode.HTML)
                //.disableWebPagePreview(true)
                //.disableNotification(true)
                //.replyToMessageId(1)
                .replyMarkup(new InlineKeyboardMarkup()); // channel로 메시지 전송은 inlineKeyboardMarkup으로 설정해야 한다.
        SendResponse sendResponse = telegramBot.execute(request);

        log.debug(sendResponse.description());

        return sendResponse.message();
    }

    /**
     * Message 가져오기
     */
    public List<Update> GetUpdates() {
        GetUpdates getUpdates = new GetUpdates().limit(100).offset(0).timeout(0);
        GetUpdatesResponse updatesResponse = telegramBot.execute(getUpdates);

        return updatesResponse.updates();
    }

}
