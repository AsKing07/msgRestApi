package com.bschool.msgrestapi;

import com.bschool.msgrestapi.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class MsgRestApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgRestApiApplication.class, args);
    }

}
