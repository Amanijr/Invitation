package com.InvitationSystem.InvitationSystem.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.util.Base64;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@invitationsystem.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email: " + e.getMessage(), e);
        }
    }

    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlBody,
                                            String base64Pdf, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            if (base64Pdf != null && !base64Pdf.isBlank()) {
                byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
                String safeAttachmentName = (attachmentName == null || attachmentName.isBlank())
                        ? "invitation-card.pdf"
                        : attachmentName;
                helper.addAttachment(
                        safeAttachmentName,
                        () -> new java.io.ByteArrayInputStream(pdfBytes),
                        "application/pdf"
                );
            }

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send HTML email with attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Send invitation email to guest
     */
    public void sendInvitationEmail(String guestEmail, String guestName, String eventName, String invitationToken) {
        String invitationUrl = baseUrl + "/api/v1/invitations/token/" + invitationToken;
        
        String htmlBody = buildInvitationEmailHtml(guestName, eventName, invitationUrl);
        sendHtmlEmail(guestEmail, "You're Invited: " + eventName, htmlBody);
    }

    public void sendInvitationEmail(String guestEmail, String subject, String renderedHtml, String pdfBase64,
                                    String attachmentName) {
        sendHtmlEmailWithAttachment(guestEmail, subject, renderedHtml, pdfBase64, attachmentName);
    }

    /**
     * Send receipt email to guest
     */
    public void sendReceiptEmail(String guestEmail, String guestName, String eventName, String receiptNumber) {
        String htmlBody = buildReceiptEmailHtml(guestName, eventName, receiptNumber);
        sendHtmlEmail(guestEmail, "Receipt for " + eventName, htmlBody);
    }

    /**
     * Send bulk upload confirmation email
     */
    public void sendBulkUploadConfirmation(String organizerEmail, String eventName, int successCount, int failureCount) {
        String htmlBody = buildBulkUploadEmailHtml(eventName, successCount, failureCount);
        sendHtmlEmail(organizerEmail, "Bulk Upload Completed: " + eventName, htmlBody);
    }

    /**
     * Send event reminder email
     */
    public void sendEventReminderEmail(String guestEmail, String guestName, String eventName, String eventDate) {
        String htmlBody = buildReminderEmailHtml(guestName, eventName, eventDate);
        sendHtmlEmail(guestEmail, "Reminder: " + eventName + " is coming up!", htmlBody);
    }

    /**
     * Build invitation email HTML body
     */
    private String buildInvitationEmailHtml(String guestName, String eventName, String invitationUrl) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>You're Invited!</h2>" +
                "<p>Dear " + guestName + ",</p>" +
                "<p>We are pleased to invite you to <strong>" + eventName + "</strong>.</p>" +
                "<p><a href='" + invitationUrl + "' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Accept Invitation</a></p>" +
                "<p>If you cannot accept the invitation, please let us know.</p>" +
                "<p>Best regards,<br>Event Team</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build receipt email HTML body
     */
    private String buildReceiptEmailHtml(String guestName, String eventName, String receiptNumber) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>Receipt Confirmation</h2>" +
                "<p>Dear " + guestName + ",</p>" +
                "<p>Thank you for your participation in <strong>" + eventName + "</strong>.</p>" +
                "<p>Your receipt number: <strong>" + receiptNumber + "</strong></p>" +
                "<p>Please keep this receipt for your records.</p>" +
                "<p>Best regards,<br>Event Team</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build bulk upload confirmation email HTML
     */
    private String buildBulkUploadEmailHtml(String eventName, int successCount, int failureCount) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>Bulk Upload Completed</h2>" +
                "<p>Your bulk upload for <strong>" + eventName + "</strong> has been processed.</p>" +
                "<table style='border-collapse: collapse; border: 1px solid #ddd;'>" +
                "<tr style='background-color: #f2f2f2;'>" +
                "<td style='border: 1px solid #ddd; padding: 8px;'>Successful Imports</td>" +
                "<td style='border: 1px solid #ddd; padding: 8px;'>" + successCount + "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='border: 1px solid #ddd; padding: 8px;'>Failed Imports</td>" +
                "<td style='border: 1px solid #ddd; padding: 8px;'>" + failureCount + "</td>" +
                "</tr>" +
                "</table>" +
                "<p>Best regards,<br>Event Team</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build event reminder email HTML
     */
    private String buildReminderEmailHtml(String guestName, String eventName, String eventDate) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h2>Event Reminder</h2>" +
                "<p>Dear " + guestName + ",</p>" +
                "<p>This is a friendly reminder that <strong>" + eventName + "</strong> is happening on <strong>" + eventDate + "</strong>.</p>" +
                "<p>We look forward to seeing you there!</p>" +
                "<p>Best regards,<br>Event Team</p>" +
                "</body>" +
                "</html>";
    }
}
