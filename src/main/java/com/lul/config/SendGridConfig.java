package com.lul.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import com.sendgrid.SendGrid;

@Configuration
@ConfigurationProperties(prefix = "app.sendgrid")
public class SendGridConfig {

    @Value("${app.sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${app.sendgrid.otp-template-id}")
    private String otpTemplateId;

    @Value("${app.sendgrid.verification-template-id}")
    private String verificationTemplateId;

    @Bean
    public SendGrid getSendGrid() {
        return new SendGrid(sendgridApiKey);
    }

    // Getters and setters
    public String getSendgridApiKey() {
        return sendgridApiKey;
    }

    public void setSendgridApiKey(String sendgridApiKey) {
        this.sendgridApiKey = sendgridApiKey;
    }

    public String getOtpTemplateId() {
        return otpTemplateId;
    }

    public void setOtpTemplateId(String otpTemplateId) {
        this.otpTemplateId = otpTemplateId;
    }

    public String getVerificationTemplateId() {
        return verificationTemplateId;
    }

    public void setVerificationTemplateId(String verificationTemplateId) {
        this.verificationTemplateId = verificationTemplateId;
    }
}