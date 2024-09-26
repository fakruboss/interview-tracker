package com.fakru.interview.tracker.service;

import com.fakru.interview.tracker.annotation.LogExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @LogExecutionTime
    public void sendEmail(String toEmail, String body, String subject) {
        log.info("sendEmail in thread: {}", Thread.currentThread().getName());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("fafakrudeen@gmail.com");
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);
        mailSender.send(message);
    }
}