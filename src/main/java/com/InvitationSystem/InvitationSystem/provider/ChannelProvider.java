package com.InvitationSystem.InvitationSystem.provider;

import com.InvitationSystem.InvitationSystem.entity.DeliveryChannel;

public interface ChannelProvider {

    DeliveryChannel getChannel();

    DeliveryResult send(DeliveryRequest request);
}
