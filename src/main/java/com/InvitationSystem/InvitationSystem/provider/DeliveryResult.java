package com.InvitationSystem.InvitationSystem.provider;

import com.InvitationSystem.InvitationSystem.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResult {

    private boolean success;
    private DeliveryStatus status;
    private String recipientContact;
    private String providerReference;
    private String providerResponse;
    private String errorMessage;
}
