package com.msas.telegram.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * bot:
 *  telegram:
 *      token-id: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *      channel-names:
 *          - "channel1"
 *          - "channel2"
 */
@Configuration
@ConfigurationProperties(TelegramBotProperties.PREFIX)
@Getter
@Setter
public class TelegramBotProperties {
    public static final String PREFIX = "bot.telegram";

    private String tokenId;

    private List<String> channelNames;
}
