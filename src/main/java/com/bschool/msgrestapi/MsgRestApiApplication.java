package com.bschool.msgrestapi;

import com.bschool.msgrestapi.config.AppProperties;
import com.bschool.msgrestapi.config.JwtProperties;
import com.bschool.msgrestapi.config.MailProperties;
import com.bschool.msgrestapi.config.PresenceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, JwtProperties.class, MailProperties.class, PresenceProperties.class})
public class MsgRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgRestApiApplication.class, args);
    }

}
