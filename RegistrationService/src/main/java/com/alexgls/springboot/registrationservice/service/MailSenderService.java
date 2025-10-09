package com.alexgls.springboot.registrationservice.service;

import com.alexgls.springboot.registrationservice.exception.SendMailException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailSenderService {

    private final MailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Async
    public void sendMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        }catch (Exception exception){
            throw new SendMailException("Произошла ошибка, связанная с отправкой сообщения пользователю: "+exception.getMessage());
        }
    }
}
