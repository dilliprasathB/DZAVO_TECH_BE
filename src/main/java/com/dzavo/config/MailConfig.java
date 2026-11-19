package com.dzavo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger logger = LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setDefaultEncoding("UTF-8");

        String cleanUsername = (username != null) ? username.trim() : "";
        // Clean password: remove accidental spaces from pasted Gmail App Passwords (xxxx xxxx xxxx xxxx -> xxxxxxxxxxxxxxxx)
        String cleanPassword = (password != null) ? password.trim().replace(" ", "") : "";

        boolean hasCredentials = !cleanUsername.isBlank() && !cleanPassword.isBlank();

        if (hasCredentials) {
            mailSender.setUsername(cleanUsername);
            mailSender.setPassword(cleanPassword);
            logger.info("[MailConfig] JavaMailSender configured with SMTP host={}:{}, username={}", host, port, cleanUsername);
        } else {
            logger.warn("[MailConfig] JavaMailSender initialized without active credentials. Set SMTP_USERNAME and SMTP_PASSWORD in DZAVO_BE/.env to enable live email delivery.");
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(hasCredentials));
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        return mailSender;
    }
}
