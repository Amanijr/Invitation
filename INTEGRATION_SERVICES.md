# Integration Services Documentation

This document covers the integration services implemented for the Invitation System.

## Table of Contents
1. [QR Code Service](#qr-code-service)
2. [PDF Service](#pdf-service)
3. [Excel Parser Service](#excel-parser-service)
4. [Email Service](#email-service)

---

## QR Code Service

### Overview
The `QRCodeService` generates QR codes from text data, primarily for invitation URLs.

### Features
- Generate QR code images as Base64 encoded PNG
- Generate QR codes from files
- Generate invitation-specific QR codes with full URLs

### Usage Example
```java
@Autowired
private QRCodeService qrCodeService;

// Generate QR code from text
String qrCodeBase64 = qrCodeService.generateQRCodeImage("https://example.com/invite?token=xyz");

// Generate invitation QR code
String qrCode = qrCodeService.generateInvitationQRCode("unique-token", "http://localhost:8080");
```

### Configuration
- QR Code size: 300x300 pixels
- Format: PNG
- Output: Base64 encoded string

---

## PDF Service

### Overview
The `PDFService` generates PDF receipts with invitation and event details.

### Features
- Generate receipt PDFs with custom data
- Save PDFs to file system
- Support for custom header/footer and additional data

### Usage Example
```java
@Autowired
private PDFService pdfService;

Map<String, String> additionalData = new HashMap<>();
additionalData.put("Event Date", "2026-04-15");
additionalData.put("Location", "Grand Hall");

String pdfBase64 = pdfService.generateReceiptPDF(
    "RECEIPT-001",
    "Wedding Reception",
    "John Doe",
    "john@example.com",
    "+1234567890",
    additionalData
);
```

### PDF Structure
- Header: Receipt title
- Receipt Number and Event Name
- Guest Information (Name, Email, Phone)
- Additional custom data
- Thank you message footer

---

## Excel Parser Service

### Overview
The `ExcelParserService` parses Excel files to extract guest information for bulk invitations.

### Features
- Parse .xlsx and .xls files
- Extract guest data from spreadsheets
- Validate required fields (email, phone)
- Support for optional fields (name, company, notes)

### Expected Excel Format
```
| Email              | Phone        | Name        | Company | Notes      |
|-------------------|--------------|-------------|---------|------------|
| guest1@email.com  | +1234567890 | John Doe    | Acme    | Important  |
| guest2@email.com  | +0987654321 | Jane Smith  | Tech    | VIP        |
```

### Usage Example
```java
@Autowired
private ExcelParserService excelParserService;

// Parse from MultipartFile
List<Map<String, String>> guests = excelParserService.parseExcelFile(multipartFile);

// Parse from byte array
List<Map<String, String>> guests = excelParserService.parseExcelBytes(fileBytes);

// Get expected headers
List<String> headers = excelParserService.getExpectedHeaders();
// Returns: [email, phone, name, company, notes]
```

### Validation
- **Required Fields**: email, phone
- **Optional Fields**: name, company, notes
- Throws `IllegalArgumentException` if required fields are missing

---

## Email Service

### Overview
The `EmailService` sends transactional emails for invitations, receipts, and bulk uploads.

### Features
- Send simple text emails
- Send HTML formatted emails
- Pre-built email templates for common use cases
- Email delivery with exception handling

### Configuration

Update `application.properties`:
```properties
# Gmail Example
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.from=noreply@invitationsystem.com
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

### Gmail Setup
1. Enable 2-Factor Authentication on your Gmail account
2. Generate an App Password at https://myaccount.google.com/apppasswords
3. Use the generated password in `spring.mail.password`

### Usage Examples

**Send Invitation Email**
```java
emailService.sendInvitationEmail(
    "guest@example.com",
    "John Doe",
    "Wedding Reception",
    "unique-invitation-token"
);
```

**Send Receipt Email**
```java
emailService.sendReceiptEmail(
    "guest@example.com",
    "John Doe",
    "Wedding Reception",
    "RECEIPT-001"
);
```

**Send Bulk Upload Confirmation**
```java
emailService.sendBulkUploadConfirmation(
    "organizer@example.com",
    "Wedding Reception",
    150,  // success count
    5     // failure count
);
```

**Send Event Reminder**
```java
emailService.sendEventReminderEmail(
    "guest@example.com",
    "John Doe",
    "Wedding Reception",
    "2026-04-15 at 6:00 PM"
);
```

### Email Templates
All templates are HTML formatted and include:
- Professional styling
- Clear call-to-action buttons
- Guest and event information

---

## Integration with Controllers

### Bulk Upload Controller
```
POST /api/v1/bulk-uploads/upload
Parameters:
- file (MultipartFile) - Excel file with guest data
- eventId (UUID) - Event ID
- templateId (UUID, optional) - Invitation template
- uploadedBy (UUID) - User ID of uploader

Response: BulkUploadResponseDto with success/failure counts
```

### Invitation Controller
```
POST /api/v1/invitations/{invitationId}/qrcode
Parameters:
- invitationId (UUID)
- qrCodeUrl (String, optional) - Custom QR code URL

Response: InvitationDetailedResponseDto with QR code
```

---

## Error Handling

### QR Code Service
- Throws `RuntimeException` if encoding fails
- Logs detailed error messages

### PDF Service
- Throws `RuntimeException` for document creation failures
- Creates parent directories if they don't exist

### Excel Parser Service
- Throws `IllegalArgumentException` for missing required fields
- Throws `RuntimeException` for file parsing failures

### Email Service
- Throws `RuntimeException` for SMTP failures
- Logs failures but doesn't interrupt business operations in controllers

---

## Dependencies
- **ZXing 3.5.2** - QR Code generation (google-zxing)
- **iText 5.5.13.3** - PDF generation
- **Apache POI 5.0.0** - Excel file processing
- **Spring Boot Mail Starter** - Email sending

---

## Future Enhancements
- PDF parsing for guest data extraction
- Additional email providers (SendGrid, AWS SES)
- Cloud storage integration (S3, Azure Blob)
- Email template customization
- Batch email processing with scheduling
