package com.bschool.msgrestapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.message")
public record AppProperties(int maxLength) {
}
