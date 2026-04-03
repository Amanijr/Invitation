package com.InvitationSystem.InvitationSystem.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class QRCodeService {

    private static final int QR_CODE_WIDTH = 300;
    private static final int QR_CODE_HEIGHT = 300;
    private static final String QR_CODE_FORMAT = "png";

    /**
     * Generate QR code from invitation token or data
     * @param qrData The data to encode (typically invitation token or URL)
     * @return Base64 encoded PNG image of QR code
     */
    public String generateQRCodeImage(String qrData) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, QR_CODE_FORMAT, outputStream);
            
            // Return as Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code and save to file
     * @param qrData The data to encode
     * @param filePath Path to save the QR code image
     */
    public void generateQRCodeFile(String qrData, String filePath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_WIDTH, QR_CODE_HEIGHT);
            
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, QR_CODE_FORMAT, path);
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Failed to generate QR code file: " + e.getMessage(), e);
        }
    }

    /**
     * Generate QR code for invitation with guest link
     * @param invitationToken The unique invitation token
     * @param baseUrl The base URL of the application
     * @return Base64 encoded QR code
     */
    public String generateInvitationQRCode(String invitationToken, String baseUrl) {
        String invitationUrl = baseUrl + "/api/v1/invitations/scan/" + invitationToken;
        return generateQRCodeImage(invitationUrl);
    }
}
