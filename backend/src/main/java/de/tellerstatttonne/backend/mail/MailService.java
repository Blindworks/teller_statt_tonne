package de.tellerstatttonne.backend.mail;

import de.tellerstatttonne.backend.systemlog.SystemLogEventType;
import de.tellerstatttonne.backend.systemlog.event.SystemLogEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public MailService(JavaMailSender mailSender, MailProperties properties,
                       ApplicationEventPublisher eventPublisher) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
    }

    @Async("mailExecutor")
    public void sendPlainText(String to, String subject, String body) {
        sendInternal(to, subject, body, null);
    }

    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html) {
        sendInternal(to, subject, htmlToPlain(html), html);
    }

    @Async("mailExecutor")
    public void sendHtml(String to, String subject, String html, String plainText) {
        sendInternal(to, subject, plainText, html);
    }

    private void sendInternal(String to, String subject, String plainText, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            boolean multipart = html != null;
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(properties.from(), properties.fromName(), StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            helper.setSubject(subject);
            if (multipart) {
                helper.setText(plainText, html);
            } else {
                helper.setText(plainText, false);
            }
            mailSender.send(message);
            log.info("Mail an {} gesendet (Betreff: {})", to, subject);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            log.error("Mail-Versand an {} fehlgeschlagen (Betreff: {}): {}", to, subject, e.getMessage(), e);
            eventPublisher.publishEvent(SystemLogEvent.of(SystemLogEventType.MAIL_DELIVERY_FAILED)
                .message("Mail-Versand an " + to + " fehlgeschlagen (Betreff: " + subject + "): "
                    + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                .build());
        }
    }

    private static String htmlToPlain(String html) {
        if (html == null) return "";
        String text = html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</p\\s*>", "\n\n")
            .replaceAll("(?i)</li\\s*>", "\n")
            .replaceAll("<[^>]+>", "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
        return text.replaceAll("[ \\t]+\n", "\n").replaceAll("\n{3,}", "\n\n").trim();
    }
}
