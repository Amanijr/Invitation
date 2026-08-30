package com.InvitationSystem.InvitationSystem.provider;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryRequest {

    private UUID invitationId;
    private UUID guestId;
    private String guestName;
    private String eventName;
    private String eventDate;
    private String venue;
    private String invitationToken;
    private String invitationUrl;
    private String qrCodeUrl;
    private String qrCodeBase64;
    private String cardReference;
    private String renderedHtml;
    private byte[] cardImageBytes;
    private String cardImageFileName;
    private byte[] pdfBytes;
    private String pdfFileName;
    private String recipientEmail;
    private String recipientPhone;
    private DeliveryChannel channel;
    private String idempotencyKey;
}
