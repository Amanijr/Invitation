package com.InvitationSystem.InvitationSystem.provider.impl;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import com.InvitationSystem.InvitationSystem.provider.ChannelProvider;
import com.InvitationSystem.InvitationSystem.provider.DeliveryRequest;
import com.InvitationSystem.InvitationSystem.provider.DeliveryResult;
import com.InvitationSystem.InvitationSystem.util.EmailService;
import com.InvitationSystem.InvitationSystem.util.InvitationEmailComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class EmailChannelProvider implements ChannelProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelProvider.class);

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Autowired
    private EmailService emailService;

    @Override
    public DeliveryChannel getChannel() {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public DeliveryResult send(DeliveryRequest request) {
        String recipient = request.getRecipientEmail();
        if (recipient == null || recipient.isBlank()) {
            log.warn("Email delivery rejected: Recipient email is missing or blank for invitation ID: {}", request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(recipient)
                    .errorMessage("Recipient email is missing or empty")
                    .build();
        }

        if (!EMAIL_PATTERN.matcher(recipient).matches()) {
            log.warn("Email delivery rejected: Invalid recipient email format '{}' for invitation ID: {}", recipient, request.getInvitationId());
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(recipient)
                    .errorMessage("Invalid recipient email format: " + recipient)
                    .build();
        }

        String subject = InvitationEmailComposer.subject(request);

        boolean hasCard = request.getCardImageBytes() != null && request.getCardImageBytes().length > 0;
        String plainBody = InvitationEmailComposer.plainText(request);
        String htmlBody = InvitationEmailComposer.html(request, hasCard);

        try {
            String pdfBase64 = null;
            if (!hasCard && request.getPdfBytes() != null && request.getPdfBytes().length > 0) {
                pdfBase64 = Base64.getEncoder().encodeToString(request.getPdfBytes());
            }

            String attachmentName = request.getPdfFileName() != null && !request.getPdfFileName().isBlank()
                    ? request.getPdfFileName()
                    : "invitation-card.pdf";

            if (hasCard) {
                String cardName = request.getCardImageFileName() != null && !request.getCardImageFileName().isBlank()
                        ? request.getCardImageFileName()
                        : "invitation-card.png";
                emailService.sendHtmlEmailWithCard(recipient, subject, plainBody, htmlBody, request.getCardImageBytes(), cardName);
            } else if (pdfBase64 != null) {
                emailService.sendInvitationEmail(recipient, subject, htmlBody, pdfBase64, attachmentName);
            } else {
                emailService.sendHtmlEmail(recipient, subject, plainBody, htmlBody);
            }

            String messageId = "EMAIL-MSG-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Email invitation sent successfully via SMTP to {} (Message ID: {})", recipient, messageId);

            return DeliveryResult.builder()
                    .success(true)
                    .status(DeliveryStatus.SENT)
                    .recipientContact(recipient)
                    .providerReference(messageId)
                    .providerResponse("Email successfully sent via SMTP to " + recipient)
                    .build();
        } catch (Exception e) {
            log.error("Failed to deliver email invitation to {}: {}", recipient, e.getMessage(), e);
            String safeErrorMsg = e.getMessage() != null && !e.getMessage().isBlank()
                    ? e.getMessage()
                    : "Mail server execution error";
            return DeliveryResult.builder()
                    .success(false)
                    .status(DeliveryStatus.FAILED)
                    .recipientContact(recipient)
                    .errorMessage("Email provider error: " + safeErrorMsg)
                    .providerResponse("SMTP Exception: " + e.getClass().getSimpleName())
                    .build();
        }
    }
}

