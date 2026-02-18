package org.nooshet.identity.service.impl;

import org.nooshet.identity.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@nooshet.org}")
    private String fromAddress;

    @Override
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // For now, we'll just send simple emails as we don't have Thymeleaf set up yet
        // In a real app, integrate Thymeleaf or another template engine
       sendSimpleEmail(to, subject, "HTML email not yet supported. Content: " + variables);
    }

    @Override
    @Async
    public void sendSimpleEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
        } catch (Exception e) {
            // Log error
            e.printStackTrace();
        }
    }

    public String loadAndFillTemplate(String templatePath, Map<String, Object> variables) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(templatePath)), StandardCharsets.UTF_8);
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
            }
            return content;
        } catch (Exception e) {
            e.printStackTrace();
            return "Your verification code is: " + variables.getOrDefault("otp", "");
        }
    }
}
