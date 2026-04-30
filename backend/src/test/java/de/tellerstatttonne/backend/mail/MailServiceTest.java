package de.tellerstatttonne.backend.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class MailServiceTest {

    private JavaMailSender mailSender;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        Session session = Session.getInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
        MailProperties props = new MailProperties("noreply@test.local", "Teller statt Tonne Test");
        mailService = new MailService(mailSender, props);
    }

    @Test
    void sendPlainText_versendetMimeMessageMitKorrektenFeldern() throws Exception {
        mailService.sendPlainText("empfaenger@example.com", "Hallo", "Inhalt mit Umlauten äöüß");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();

        assertThat(sent.getAllRecipients()).hasSize(1);
        assertThat(sent.getAllRecipients()[0].toString()).isEqualTo("empfaenger@example.com");
        assertThat(sent.getSubject()).isEqualTo("Hallo");
        assertThat(sent.getFrom()[0].toString()).contains("noreply@test.local");
        assertThat(sent.getContent().toString()).contains("Umlauten äöüß");
    }

    @Test
    void send_smtpFehlerWirdGeschluckt() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> mailService.sendPlainText("a@b.de", "x", "y"))
            .doesNotThrowAnyException();

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendHtml_setztHtmlContentType() throws Exception {
        mailService.sendHtml("html@example.com", "HTML", "<p>Hallo</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        assertThat(sent.getContentType()).contains("text/html");
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("html@example.com");
    }
}
