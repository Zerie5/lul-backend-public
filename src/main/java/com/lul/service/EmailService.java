package com.lul.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.lul.config.SendGridConfig;
import com.sendgrid.helpers.mail.objects.Personalization;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final SendGridConfig sendGridConfig;

    @Value("${app.sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${app.sendgrid.from.email}")
    private String fromEmail;

    @Value("${app.sendgrid.from.name}")
    private String fromName;

    @Autowired
    private SendGrid sendGrid;

    public void sendWelcomeEmail(String toEmail, String userName) throws IOException {
        Email from = new Email("zerie@lul.com", "Lul Team");
        Email to = new Email(toEmail);
        String subject = "Welcome to Lul";
        Content content = new Content("text/plain", "Welcome " + userName + " to Lul!");
        
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sg.api(request);
        
        if (response.getStatusCode() >= 400) {
            throw new IOException("Failed to send email. Status code: " + response.getStatusCode());
        }
    }

    public void sendOtpEmail(String email, String userName) throws IOException {
        Email from = new Email("zerie@lulpay.com", "Lul Team");
        Email to = new Email(email);
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setTemplateId(sendGridConfig.getOtpTemplateId());

        String otp = generateOtp(); // Generate 6-digit OTP

        Personalization personalization = new Personalization();
        personalization.addTo(to);
        personalization.addDynamicTemplateData("user_name", userName);
        personalization.addDynamicTemplateData("otp", otp);
        mail.addPersonalization(personalization);

        SendGrid sg = new SendGrid(sendGridConfig.getSendgridApiKey());
        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());
        
        Response response = sg.api(request);
        
        if (response.getStatusCode() >= 400) {
            throw new IOException("Failed to send OTP email");
        }
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public void sendEmail(String to, String subject, String body) throws Exception {
        try {
            Email from = new Email(fromEmail, fromName);
            Email toEmail = new Email(to);
            
            Mail mail = new Mail();
            mail.setFrom(from);
            mail.setTemplateId(sendGridConfig.getVerificationTemplateId());
            
            Personalization personalization = new Personalization();
            personalization.addTo(toEmail);
            personalization.addDynamicTemplateData("user_name", to.split("@")[0]); // Simple username from email
            personalization.addDynamicTemplateData("otp_code", generateTestOtp());
            
            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            
            if (response.getStatusCode() >= 400) {
                log.error("SendGrid error: {}", response.getBody());
                throw new Exception("Email service error: " + response.getStatusCode());
            }
            
            log.info("Test verification email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage());
            throw new Exception("Email service is currently unavailable");
        }
    }

    private String generateTestOtp() {
        return String.format("%06d", new Random().nextInt(1000000));
    }
}
