package com.bschool.msgrestapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.presence")
public record PresenceProperties(int offlineThresholdMinutes) {
}
