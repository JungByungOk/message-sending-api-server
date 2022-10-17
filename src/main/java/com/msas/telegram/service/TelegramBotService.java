package com.msas.telegram.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetMeResponse;
import com.pengrad.telegrambot.response.GetUpdatesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotService {

    private final TelegramBot telegramBot;

    @Value("${bot.telegram.channel-names}")
    private List<String> channelNames = new ArrayList<>();

    // channelName : channelId mapping data ...
    private final HashMap<String, Long> channelMap = new HashMap<>();

    @PostConstruct
    public void init(){

        /*
         {
          "update_id": 175775917,
          "channel_post": {
            "message_id": 136,
            "sender_chat": {
              "id": -1001877435674,
              "type": "channel",
              "title": "NFTReally.Notification"
            },
            "date": 1666007691,
            "chat": {
         ->   "id": -1001877435674,
              "type": "channel",
         ->   "title": "NFTReally.Notification"
            },
         -> "text": "/start",
            "entities": [
              {
         ->     "type": "bot_command",
                "offset": 0,
                "length": 6
              }
            ]
          }
         */

        //+++++++++++++++++++++++++++++++++++++++
        // /getUpdates 리스너
        // bot_command = /start 확인하고, 채널 채팅방 이름 확인하여 채널 아이디 저장
        //+++++++++++++++++++++++++++++++++++++++
        telegramBot.setUpdatesListener(updates -> {

            log.info("[UpdatesListener] Receive Message\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(updates.get(0)));

            try
            {
                // channelPost & /start & title=channelName 이면, chatId 저장
                if(updates.get(0).channelPost() != null)
                    if(updates.get(0).channelPost().text().toLowerCase().compareTo("/start") == 0)
                    {
                        for(String channelName : channelNames)
                        {
                            if(updates.get(0).channelPost().chat().title().compareTo(channelName) == 0)
                                channelMap.put(channelName, updates.get(0).channelPost().chat().id());
                        }
                    }
            }
            catch(NullPointerException e)
            {
                log.error("chat().id() update skip...");
            }
            finally
            {
                log.info("[UpdatesListener] channelInfo = {}", channelMap);
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });

    }

    /**
     * Bot 정보 가져오기
     */
    public User GetMe()
    {
        GetMe request = new GetMe();
        GetMeResponse getMeResponse = telegramBot.execute(request);

        return getMeResponse.user();
    }

    /**
     * 지정한 채널로 Message 보내기
     */
    public Message SendMessage(String channelName, String text)
    {
        if(channelMap.isEmpty())
        {
            // /start 시작한 채널이 없습니다.
            return new Message();
        }

        if(!channelMap.containsKey(channelName))
        {
            // 일치하는 채널이 없습니다.
            return new Message();
        }

        return SendMessage(channelMap.get(channelName), text);
    }

    protected Message SendMessage(long id, String text)
    {
        // method.1
        //SendResponse sendResponse = telegramBot.execute(new SendMessage(chatId, "Hello!"));

        // method.2
        SendMessage request = new SendMessage(id, text)
                //.parseMode(ParseMode.HTML)
                //.disableWebPagePreview(true)
                //.disableNotification(true)
                //.replyToMessageId(1)
                .replyMarkup(new InlineKeyboardMarkup());//channel로 메시지 전송은 inlineKeyboardMarkup으로 설정해야 한다.
        SendResponse sendResponse = telegramBot.execute(request);

        log.debug(sendResponse.description());

        return sendResponse.message();
    }

    /**
     * Message 가져오기
     */
    public List<Update> GetUpdates()
    {
        GetUpdates getUpdates = new GetUpdates().limit(100).offset(0).timeout(3);
        GetUpdatesResponse updatesResponse = telegramBot.execute(getUpdates);

        return updatesResponse.updates();
    }

}
