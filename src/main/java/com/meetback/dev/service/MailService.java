package com.meetback.dev.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    public void sendPasswordResetEmail(
            String email,
            String resetLink
    ) {

        SimpleMailMessage mailMessage =
                new SimpleMailMessage();

        mailMessage.setFrom(
                from
        );

        mailMessage.setTo(
                email
        );

        mailMessage.setSubject(
                "[MeetBack] 비밀번호 재설정"
        );

        mailMessage.setText(
                """
               MeetBack 비밀번호 재설정을 요청하셨습니다.

               아래 링크를 통해 새로운 비밀번호를 설정해주세요.

               %s

               본인이 요청하지 않았다면 이 메일을 무시해주세요.
               """
                        .formatted(
                                resetLink
                        )
        );

        mailSender.send(
                mailMessage
        );

    }

}
