package com.meongnyangerang.meongnyangerang.component;

import static com.meongnyangerang.meongnyangerang.exception.ErrorCode.EMAIL_NOT_SEND;

import com.meongnyangerang.meongnyangerang.exception.MeongnyangerangException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailComponent {

  private final JavaMailSender javaMailSender;

  public void sendMail(String mail, String subject, String text) {

    MimeMessagePreparator msg = new MimeMessagePreparator() {
      @Override
      public void prepare(MimeMessage mimeMessage) throws Exception {
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        // 메일을 보낼 대상
        mimeMessageHelper.setTo(mail);
        // 메일 제목
        mimeMessageHelper.setSubject(subject);
        // 메일 내용 (내용과, html 설정)
        mimeMessageHelper.setText(text, true);
      }
    };

    try {
      javaMailSender.send(msg);
    } catch (Exception e) {
      log.error("메일 발송 실패: {}", e.getMessage(), e); // 로그 추가 (예외 메시지와 스택 트레이스 출력)
      throw new MeongnyangerangException(EMAIL_NOT_SEND);
    }
  }
}
