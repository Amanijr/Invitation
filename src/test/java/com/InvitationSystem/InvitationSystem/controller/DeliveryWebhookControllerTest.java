package com.InvitationSystem.InvitationSystem.controller;

import com.InvitationSystem.InvitationSystem.service.DeliveryWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeliveryWebhookControllerTest {

    @Mock
    private DeliveryWebhookService deliveryWebhookService;

    @InjectMocks
    private DeliveryWebhookController deliveryWebhookController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(deliveryWebhookController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
        ReflectionTestUtils.setField(deliveryWebhookController, "smsGateWebhookSecret", "desk-secret");
        ReflectionTestUtils.setField(deliveryWebhookController, "evolutionWebhookSecret", "evo-secret");
    }

    @Test
    void rejectsMissingSecret() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/sms-gate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"sms:delivered\"}"))
                .andExpect(status().isUnauthorized());
        verify(deliveryWebhookService, never()).applySmsGateEvent(any());
    }

    @Test
    void acceptsValidSecret() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/sms-gate")
                        .header("X-Webhook-Secret", "desk-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"sms:delivered\",\"payload\":{\"messageId\":\"abc\"}}"))
                .andExpect(status().isOk());
        verify(deliveryWebhookService).applySmsGateEvent(any());
    }

    @Test
    void evolutionRejectsMissingSecret() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/evolution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages.update\"}"))
                .andExpect(status().isUnauthorized());
        verify(deliveryWebhookService, never()).applyEvolutionEvent(any());
    }

    @Test
    void evolutionAcceptsApikeyHeader() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/evolution")
                        .header("apikey", "evo-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"messages.update\",\"data\":{\"id\":\"BAE5\",\"status\":\"DELIVERY_ACK\"}}"))
                .andExpect(status().isOk());
        verify(deliveryWebhookService).applyEvolutionEvent(any());
    }
}
