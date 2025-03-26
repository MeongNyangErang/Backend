package com.meongnyangerang.meongnyangerang.component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

@ExtendWith(MockitoExtension.class)
public class MailComponentTest {

  @Mock
  private JavaMailSender javaMailSender;

  @InjectMocks
  private MailComponent mailComponent;

  @Test
  @DisplayName("이메일 발송 성공 테스트")
  void sendMailSuccess() {
    // given
    String email = "test@example.com";
    String subject = "Test Subject";
    String text = "Test Content";

    // when
    mailComponent.sendMail(email, subject, text);

    // then
    verify(javaMailSender, times(1)).send(any(MimeMessagePreparator.class));
  }

}
