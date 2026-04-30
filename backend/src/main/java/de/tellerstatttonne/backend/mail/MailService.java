package de.tellerstatttonne.backend.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    public MailService(JavaMailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Async("mailExecutor")
    public void sendPlainText(String to, String subject, String body) {
        send(to, subject, body, false);
    }

    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html) {
        send(to, subject, html, true);
    }

    private void send(String to, String subject, String content, boolean html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(properties.from(), properties.fromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);
            mailSender.send(message);
            log.info("Mail an {} gesendet (Betreff: {})", to, subject);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Mail-Versand an {} fehlgeschlagen (Betreff: {}): {}", to, subject, e.getMessage(), e);
        }
    }
}
