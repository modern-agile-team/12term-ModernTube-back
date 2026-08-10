package com.moderntube.moderntubo_backend.service;

import com.moderntube.moderntubo_backend.cache.EmailVerificationCache;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Service
@Slf4j
@AllArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final EmailVerificationCache emailVerificationCache;
    private static final int CODE_LENGTH = 6;
    private static final String CHARACTERS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 메일 템플릿을 가져와서 코드를 넣는 로직 (내부로직이라 공개가 안됨.)
     * @param code 인증코드를 입력
     * @return 입려간 인증코드를 템플릿에 적용하여 템플릿 채로 리턴
     * @throws IOException
     */
    private String loadEmailTemplate(String code) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/mailTemplates/email-verification.html");
        String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return html.replace("{{CODE}}", code);
    }

    /**
     * 메서드내에 난수를 생성해서 인증코드를 생성후 to에게 메일을 전송.
     * @param to 보낼유저의 이메일주소
     * @throws MessagingException
     * @throws IOException
     */
    public void sendVerificationEmail(String to) throws MessagingException, IOException {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        for (int i = 0; i < CODE_LENGTH; i++) code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));

        emailVerificationCache.saveCode(to, code.toString());

        helper.setTo(to);
        helper.setSubject("[modernTube] 이메일 인증번호");
        helper.setText(loadEmailTemplate(code.toString()), true); // true = HTML로 처리
        javaMailSender.send(message);
    }

}
