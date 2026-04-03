package com.msas.telegram.coniguration;

import com.pengrad.telegrambot.TelegramBot;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelegramBotConfiguration {

    @Value("${bot.telegram.token-id}")
    private String token;

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot.Builder(token).okHttpClient(new OkHttpClient()).build();
    }

}
