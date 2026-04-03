package com.InvitationSystem.InvitationSystem.util;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@invitationsystem.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void sendSimpleEmail_success() {
        assertDoesNotThrow(() -> emailService.sendSimpleEmail("a@b.com", "sub", "body"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_failureWrapsException() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () -> emailService.sendSimpleEmail("a@b.com", "sub", "body"));
    }

    @Test
    void sendHtmlEmail_success() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendHtmlEmail("a@b.com", "sub", "<h1>Hello</h1>"));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendInvitationEmail_overloadWithTemplate_success() {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> emailService.sendInvitationEmail(
                "guest@example.com",
                "Invitation",
                "<html>card</html>",
                null,
                "invitation.pdf"
        ));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
