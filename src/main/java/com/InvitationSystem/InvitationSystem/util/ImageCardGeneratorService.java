package com.InvitationSystem.InvitationSystem.util;

import com.InvitationSystem.InvitationSystem.Dto.templatesDto.TemplateFieldConfigDto;
import com.InvitationSystem.InvitationSystem.entity.FieldType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class ImageCardGeneratorService {

    @Autowired
    private QRCodeService qrCodeService;

    /**
     * Renders background image + field overlays (text & QR) into a PNG byte array.
     */
    public byte[] renderCardImage(byte[] backgroundImageBytes, List<TemplateFieldConfigDto> configs, Map<String, String> dataMap) {
        BufferedImage background = null;

        if (backgroundImageBytes != null && backgroundImageBytes.length > 0) {
            try (InputStream is = new ByteArrayInputStream(backgroundImageBytes)) {
                background = ImageIO.read(is);
            } catch (Exception e) {
                // Ignore and fallback to blank canvas
            }
        }

        if (background == null) {
            background = createDefaultCanvas();
        }

        int canvasWidth = background.getWidth();
        int canvasHeight = background.getHeight();

        // Create a copy of the background to avoid mutating original
        BufferedImage composite = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = composite.createGraphics();

        // Draw original background
        g2d.drawImage(background, 0, 0, null);

        // Configure rendering quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (configs != null) {
            for (TemplateFieldConfigDto config : configs) {
                if (config.getFieldType() == null) continue;
                if (config.getFieldType() != FieldType.GUEST_NAME && config.getFieldType() != FieldType.QR_CODE) {
                    continue;
                }

                double pxX = (config.getX() / 100.0) * canvasWidth;
                double pxY = (config.getY() / 100.0) * canvasHeight;
                double pxW = (config.getWidth() / 100.0) * canvasWidth;
                double pxH = (config.getHeight() / 100.0) * canvasHeight;

                if (config.getFieldType() == FieldType.QR_CODE) {
                    renderQrCodeOverlay(g2d, config, dataMap, pxX, pxY, pxW, pxH);
                } else {
                    renderTextOverlay(g2d, config, dataMap, pxX, pxY, pxW, pxH, canvasHeight);
                }
            }
        }

        g2d.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(composite, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render card image: " + e.getMessage(), e);
        }
    }

    /**
     * Renders card image and returns Base64 encoded string.
     */
    public String renderCardImageBase64(byte[] backgroundImageBytes, List<TemplateFieldConfigDto> configs, Map<String, String> dataMap) {
        byte[] imageBytes = renderCardImage(backgroundImageBytes, configs, dataMap);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private void renderQrCodeOverlay(Graphics2D g2d, TemplateFieldConfigDto config, Map<String, String> dataMap,
                                     double x, double y, double w, double h) {
        String qrData = dataMap != null ? dataMap.getOrDefault("QR_CODE", "SAMPLE-TOKEN-12345") : "SAMPLE-TOKEN-12345";
        if (config.getSampleText() != null && !config.getSampleText().isBlank()) {
            qrData = config.getSampleText();
        }

        try {
            String base64Qr = qrCodeService.generateQRCodeImage(qrData);
            byte[] qrBytes = Base64.getDecoder().decode(base64Qr);
            try (InputStream is = new ByteArrayInputStream(qrBytes)) {
                BufferedImage qrImage = ImageIO.read(is);
                if (qrImage != null) {
                    g2d.drawImage(qrImage, (int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h), null);
                }
            }
        } catch (Exception e) {
            // Draw placeholder rectangle if QR fails
            g2d.setColor(Color.WHITE);
            g2d.fillRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
            g2d.setColor(Color.BLACK);
            g2d.drawRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
            g2d.drawString("[QR CODE]", (int) Math.round(x + 10), (int) Math.round(y + h / 2));
        }
    }

    private void renderTextOverlay(Graphics2D g2d, TemplateFieldConfigDto config, Map<String, String> dataMap,
                                   double x, double y, double w, double h, int canvasHeight) {
        String key = config.getFieldType().name();
        String text = dataMap != null ? dataMap.get(key) : null;
        if (text == null || text.isBlank()) {
            text = config.getSampleText();
        }
        if (text == null || text.isBlank()) {
            text = getDefaultTextForField(config.getFieldType());
        }

        int fontStyle = "BOLD".equalsIgnoreCase(config.getFontWeight()) ? Font.BOLD : Font.PLAIN;
        int fontSize = config.getFontSize() != null ? config.getFontSize() : 24;

        // Scale font size proportionally if canvas is large (base height 1080).
        // The designer uses the same base so overlay size matches the press.
        int scaledFontSize = Math.max(10, (int) Math.round(fontSize * (canvasHeight / 1080.0)));
        String fontFamily = config.getFontFamily() != null ? config.getFontFamily() : "SansSerif";

        Color textColor = parseColor(config.getFontColor());
        g2d.setColor(textColor);

        String alignment = config.getAlignment() != null ? config.getAlignment().toUpperCase() : "CENTER";
        int boxW = Math.max(1, (int) Math.round(w));
        int boxH = Math.max(1, (int) Math.round(h));

        Font font = new Font(fontFamily, fontStyle, scaledFontSize);
        FontMetrics metrics = g2d.getFontMetrics(font);
        List<String> lines = wrapText(text, metrics, boxW);
        int trySize = scaledFontSize;
        while (trySize > 10 && (totalTextHeight(lines, metrics) > boxH || anyLineOverflows(lines, metrics, boxW))) {
            trySize -= 1;
            font = new Font(fontFamily, fontStyle, trySize);
            metrics = g2d.getFontMetrics(font);
            lines = wrapText(text, metrics, boxW);
        }

        g2d.setFont(font);
        Shape previousClip = g2d.getClip();
        g2d.clip(new Rectangle2D.Double(x, y, w, h));

        int lineHeight = metrics.getHeight();
        int totalH = totalTextHeight(lines, metrics);
        double startY = y + (h - totalH) / 2.0 + metrics.getAscent();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineWidth = metrics.stringWidth(line);
            double drawX = x;
            switch (alignment) {
                case "CENTER":
                    drawX = x + (w - lineWidth) / 2.0;
                    break;
                case "RIGHT":
                    drawX = x + w - lineWidth;
                    break;
                case "LEFT":
                default:
                    drawX = x;
                    break;
            }
            g2d.drawString(line, (int) Math.round(drawX), (int) Math.round(startY + (i * lineHeight)));
        }

        g2d.setClip(previousClip);
    }

    private static int totalTextHeight(List<String> lines, FontMetrics metrics) {
        if (lines.isEmpty()) return 0;
        return lines.size() * metrics.getHeight();
    }

    private static boolean anyLineOverflows(List<String> lines, FontMetrics metrics, int maxWidth) {
        for (String line : lines) {
            if (metrics.stringWidth(line) > maxWidth) return true;
        }
        return false;
    }

    static List<String> wrapText(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return lines;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (metrics.stringWidth(candidate) <= maxWidth || current.length() == 0) {
                current = new StringBuilder(candidate);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private Color parseColor(String hexColor) {
        if (hexColor == null || hexColor.isBlank()) {
            return Color.WHITE;
        }
        try {
            if (!hexColor.startsWith("#")) {
                hexColor = "#" + hexColor;
            }
            return Color.decode(hexColor);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private String getDefaultTextForField(FieldType fieldType) {
        return switch (fieldType) {
            case GUEST_NAME -> "Guest Name";
            case EVENT_NAME -> "Event Name";
            case EVENT_DATE -> "11 September 2026";
            case EVENT_TIME -> "6:00 PM";
            case EVENT_VENUE -> "The Slipway, Dar es Salaam";
            default -> "Sample Text";
        };
    }

    private BufferedImage createDefaultCanvas() {
        BufferedImage img = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(15, 23, 42)); // Slate 900
        g2.fillRect(0, 0, 1920, 1080);
        g2.setColor(new Color(99, 102, 241)); // Indigo 500
        g2.drawRect(50, 50, 1820, 980);
        g2.dispose();
        return img;
    }
}
