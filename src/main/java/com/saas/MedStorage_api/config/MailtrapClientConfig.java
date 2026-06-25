package com.saas.MedStorage_api.config;

import io.mailtrap.client.MailtrapClient;
import io.mailtrap.config.MailtrapConfig;
import io.mailtrap.factory.MailtrapClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailtrapClientConfig {

    @Bean
    public MailtrapClient mailtrapClient(@Value("${mailtrap.api-token}") String apiToken) {
        MailtrapConfig config = new MailtrapConfig.Builder()
                .token(apiToken)
                .build();
        return MailtrapClientFactory.createMailtrapClient(config);
    }
}
