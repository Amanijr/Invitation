package com.InvitationSystem.InvitationSystem.service;

import com.InvitationSystem.InvitationSystem.Dto.invitationsDto.BatchRenderResultDto;
import com.InvitationSystem.InvitationSystem.entity.Invitation;

import java.util.List;
import java.util.UUID;

public interface ECardRenderingEngineService {

    /**
     * Renders a personalized high-res E-Card image for an invitation, saves it to disk via FileStorageService,
     * updates the invitation's cardReference, and persists the entity.
     */
    Invitation renderAndStoreCard(UUID invitationId);

    /**
     * Renders and stores an E-Card using an existing Invitation object within a transaction.
     */
    Invitation renderAndStoreCard(Invitation invitation);

    /**
     * Renders all invitations for an event in bulk with fault isolation and streaming memory cleanup.
     */
    BatchRenderResultDto renderBatchForEvent(UUID eventId);

    /**
     * Renders a list of invitations by ID in bulk with per-item error handling.
     */
    BatchRenderResultDto renderBatch(List<UUID> invitationIds);

    /**
     * Streams rendered PNG byte array for an invitation directly.
     */
    byte[] renderCardImageBytes(UUID invitationId);

    /**
     * Guest card image by unique token. Does not consume admission.
     */
    byte[] renderCardImageBytesByToken(String uniqueToken);
}
