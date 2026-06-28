package ru.romanov.walletservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Getter
@Setter
@ConfigurationProperties(value = "app.commission")
public class CommissionProperties {
    private BigDecimal percent;
    private Map<String, UUID> techAccounts;
}