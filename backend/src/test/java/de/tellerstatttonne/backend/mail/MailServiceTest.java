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
    void sendHtml_versendetMultipartMitHtmlUndPlainAlternative() throws Exception {
        mailService.sendHtml("html@example.com", "HTML", "<p>Hallo &amp; willkommen</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        assertThat(sent.getContentType()).contains("multipart/");
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("html@example.com");

        Object content = sent.getContent();
        assertThat(content).isInstanceOf(jakarta.mail.Multipart.class);
        jakarta.mail.Multipart mp = (jakarta.mail.Multipart) content;
        // Im Spring-MimeMessageHelper wird ein verschachteltes alternative-Multipart erzeugt
        boolean foundHtml = containsBodyOfType(mp, "text/html");
        boolean foundPlain = containsBodyOfType(mp, "text/plain");
        assertThat(foundHtml).as("HTML-Part vorhanden").isTrue();
        assertThat(foundPlain).as("Plain-Part vorhanden").isTrue();
    }

    @Test
    void sendHtml_mitExpliziterPlainAlternative_verwendetDiese() throws Exception {
        mailService.sendHtml("a@b.de", "Subj", "<p>HTML-Teil</p>", "Plain-Teil");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        sent.saveChanges();

        Object content = sent.getContent();
        assertThat(content).isInstanceOf(jakarta.mail.Multipart.class);
        jakarta.mail.Multipart mp = (jakarta.mail.Multipart) content;
        assertThat(extractBodyByType(mp, "text/plain")).contains("Plain-Teil");
        assertThat(extractBodyByType(mp, "text/html")).contains("HTML-Teil");
    }

    private static boolean containsBodyOfType(jakarta.mail.Multipart mp, String type) throws Exception {
        for (int i = 0; i < mp.getCount(); i++) {
            jakarta.mail.BodyPart bp = mp.getBodyPart(i);
            if (bp.isMimeType(type)) return true;
            Object inner = bp.getContent();
            if (inner instanceof jakarta.mail.Multipart innerMp && containsBodyOfType(innerMp, type)) {
                return true;
            }
        }
        return false;
    }

    private static String extractBodyByType(jakarta.mail.Multipart mp, String type) throws Exception {
        for (int i = 0; i < mp.getCount(); i++) {
            jakarta.mail.BodyPart bp = mp.getBodyPart(i);
            if (bp.isMimeType(type)) return bp.getContent().toString();
            Object inner = bp.getContent();
            if (inner instanceof jakarta.mail.Multipart innerMp) {
                String found = extractBodyByType(innerMp, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
