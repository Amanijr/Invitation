package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.FieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageCardGeneratorServiceTest {

    @Mock
    private QRCodeService qrCodeService;

    @InjectMocks
    private ImageCardGeneratorService imageCardGeneratorService;

    private List<TemplateFieldConfigDto> configs;
    private Map<String, String> dataMap;

    @BeforeEach
    void setUp() {
        TemplateFieldConfigDto nameField = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.GUEST_NAME)
                .x(20.0)
                .y(30.0)
                .width(60.0)
                .height(10.0)
                .fontSize(36)
                .fontColor("#FFFFFF")
                .alignment("CENTER")
                .fontWeight("BOLD")
                .build();

        TemplateFieldConfigDto qrField = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.QR_CODE)
                .x(35.0)
                .y(50.0)
                .width(30.0)
                .height(30.0)
                .qrSize(200)
                .build();

        configs = List.of(nameField, qrField);

        dataMap = Map.of(
                "GUEST_NAME", "Jane & John Doe",
                "EVENT_NAME", "Gala Night 2026",
                "QR_CODE", "SAMPLE-TOKEN-XYZ"
        );
    }

    @Test
    void renderCardImage_returnsValidBytes() {
        when(qrCodeService.generateQRCodeImage(anyString()))
                .thenReturn("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

        byte[] renderedPng = imageCardGeneratorService.renderCardImage(null, configs, dataMap);

        assertNotNull(renderedPng);
        assertTrue(renderedPng.length > 0);
    }

    @Test
    void renderCardImageBase64_returnsBase64String() {
        when(qrCodeService.generateQRCodeImage(anyString()))
                .thenReturn("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

        String base64Image = imageCardGeneratorService.renderCardImageBase64(null, configs, dataMap);

        assertNotNull(base64Image);
        assertFalse(base64Image.isEmpty());
    }

    @Test
    void renderCardImage_keepsLongTextInsideFieldBox() throws Exception {
        BufferedImage background = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = background.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 400);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(background, "png", baos);

        TemplateFieldConfigDto dateField = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.GUEST_NAME)
                .x(10.0)
                .y(10.0)
                .width(20.0)
                .height(20.0)
                .fontSize(72)
                .fontColor("#000000")
                .alignment("CENTER")
                .fontWeight("BOLD")
                .build();

        byte[] rendered = imageCardGeneratorService.renderCardImage(
                baos.toByteArray(),
                List.of(dateField),
                Map.of("GUEST_NAME", "Friday, September 11, 2026 The Slipway Dar es Salaam")
        );

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(rendered));
        assertEquals(400, result.getWidth());
        Color corner = new Color(result.getRGB(2, 2));
        assertEquals(255, corner.getRed());
        assertEquals(255, corner.getGreen());
        assertEquals(255, corner.getBlue());
        Color outsideBox = new Color(result.getRGB(390, 200));
        assertEquals(255, outsideBox.getRed());
        assertEquals(255, outsideBox.getGreen());
        assertEquals(255, outsideBox.getBlue());
    }

    @Test
    void renderCardImage_ignoresEventDetailSlots() throws Exception {
        BufferedImage background = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = background.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 400, 400);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(background, "png", baos);

        TemplateFieldConfigDto eventName = TemplateFieldConfigDto.builder()
                .fieldType(FieldType.EVENT_NAME)
                .x(0.0)
                .y(0.0)
                .width(100.0)
                .height(40.0)
                .fontSize(72)
                .fontColor("#000000")
                .alignment("CENTER")
                .fontWeight("BOLD")
                .build();

        byte[] rendered = imageCardGeneratorService.renderCardImage(
                baos.toByteArray(),
                List.of(eventName),
                Map.of("EVENT_NAME", "SHOULD NOT PRINT")
        );

        BufferedImage result = ImageIO.read(new ByteArrayInputStream(rendered));
        Color center = new Color(result.getRGB(200, 40));
        assertEquals(255, center.getRed());
        assertEquals(255, center.getGreen());
        assertEquals(255, center.getBlue());
    }
}
