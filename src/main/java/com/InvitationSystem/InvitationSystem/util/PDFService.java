package com.InvitationSystem.InvitationSystem.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class PDFService {

    /**
     * Generate receipt PDF with invitation details
     * @param receiptNumber Unique receipt number
     * @param eventName Name of the event
     * @param guestName Name of the guest
     * @param guestEmail Email of the guest
     * @param guestPhone Phone of the guest
     * @param additionalData Additional data to include in receipt
     * @return Base64 encoded PDF
     */
    public String generateReceiptPDF(String receiptNumber, String eventName, String guestName, 
                                     String guestEmail, String guestPhone, Map<String, String> additionalData) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Header
            Paragraph header = new Paragraph("EVENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            // Receipt details
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Receipt Number: " + receiptNumber));
            document.add(new Paragraph("Event: " + eventName));
            document.add(new Paragraph("\n"));

            // Guest information
            Paragraph guestInfo = new Paragraph("GUEST INFORMATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            document.add(guestInfo);
            document.add(new Paragraph("Name: " + guestName));
            document.add(new Paragraph("Email: " + guestEmail));
            document.add(new Paragraph("Phone: " + guestPhone));
            document.add(new Paragraph("\n"));

            // Additional data
            if (additionalData != null && !additionalData.isEmpty()) {
                Paragraph additionalInfo = new Paragraph("ADDITIONAL INFORMATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
                document.add(additionalInfo);
                for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                    document.add(new Paragraph(entry.getKey() + ": " + entry.getValue()));
                }
            }

            // Footer
            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Thank you for attending!", FontFactory.getFont(FontFactory.HELVETICA, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // Return as Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate receipt PDF and save to file
     * @param filePath Path to save the PDF
     * @param receiptNumber Unique receipt number
     * @param eventName Name of the event
     * @param guestName Name of the guest
     * @param guestEmail Email of the guest
     * @param guestPhone Phone of the guest
     * @param additionalData Additional data
     */
    public void generateReceiptPDFFile(String filePath, String receiptNumber, String eventName, 
                                       String guestName, String guestEmail, String guestPhone,
                                       Map<String, String> additionalData) {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Header
            Paragraph header = new Paragraph("EVENT RECEIPT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            // Receipt details
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Receipt Number: " + receiptNumber));
            document.add(new Paragraph("Event: " + eventName));
            document.add(new Paragraph("\n"));

            // Guest information
            Paragraph guestInfo = new Paragraph("GUEST INFORMATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
            document.add(guestInfo);
            document.add(new Paragraph("Name: " + guestName));
            document.add(new Paragraph("Email: " + guestEmail));
            document.add(new Paragraph("Phone: " + guestPhone));
            document.add(new Paragraph("\n"));

            // Additional data
            if (additionalData != null && !additionalData.isEmpty()) {
                Paragraph additionalInfo = new Paragraph("ADDITIONAL INFORMATION", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
                document.add(additionalInfo);
                for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                    document.add(new Paragraph(entry.getKey() + ": " + entry.getValue()));
                }
            }

            // Footer
            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("Thank you for attending!", FontFactory.getFont(FontFactory.HELVETICA, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (DocumentException | IOException e) {
            throw new RuntimeException("Failed to generate PDF file: " + e.getMessage(), e);
        }
    }

    public String generateInvitationCardPdf(String renderedHtml) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            XMLWorkerHelper.getInstance().parseXHtml(
                    writer,
                    document,
                    new ByteArrayInputStream(renderedHtml.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8
            );

            document.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invitation card PDF: " + e.getMessage(), e);
        }
    }
}
