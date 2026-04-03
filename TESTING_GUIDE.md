# Unit Testing Guide

This document provides a comprehensive guide to the unit test suite for the Invitation System.

## Table of Contents
1. [Test Overview](#test-overview)
2. [Test Structure](#test-structure)
3. [Running Tests](#running-tests)
4. [Test Coverage](#test-coverage)
5. [Test Patterns](#test-patterns)
6. [Troubleshooting](#troubleshooting)

---

## Test Overview

The test suite covers:
- **Service Layer**: Business logic and data operations
- **Controller Layer**: REST API endpoints
- **Utility Services**: QR codes, PDFs, email, Excel parsing

**Total Test Classes**: 10
**Total Test Methods**: 80+

### Key Testing Technologies
- **JUnit 5**: Test framework
- **Mockito**: Mocking framework for dependencies
- **Spring Test**: MockMvc for controller testing
- **Jackson**: JSON serialization/deserialization

---

## Test Structure

```
src/test/java/com/InvitationSystem/InvitationSystem/
├── service/
│   └── impl/
│       ├── InvitationServiceImplTest.java      (20 tests)
│       ├── UserServiceImplTest.java            (10 tests)
│       └── BulkUploadServiceImplTest.java      (13 tests)
├── util/
│   ├── QRCodeServiceTest.java                  (8 tests)
│   ├── PDFServiceTest.java                     (8 tests)
│   ├── ExcelParserServiceTest.java             (9 tests)
│   └── EmailServiceTest.java                   (13 tests)
└── controller/
    ├── InvitationControllerTest.java           (14 tests)
    ├── BulkUploadControllerTest.java           (11 tests)
    └── UserControllerTest.java                 (12 tests)
```

---

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=InvitationServiceImplTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=InvitationServiceImplTest#testCreateInvitation_Success
```

### Run Tests with Coverage Report
```bash
mvn test jacoco:report
# Report generated in: target/site/jacoco/index.html
```

### Run Tests with Maven in IDE
1. **VSCode**: Right-click test file → "Run Tests"
2. **IntelliJ IDEA**: Right-click test class → "Run Tests"

### Continuous Testing (Watch Mode)
```bash
mvn test -Dtest=InvitationServiceImplTest -Dgroups=unit
```

---

## Test Coverage

### Service Layer Tests

#### InvitationServiceImplTest (20 tests)
Focus: Core invitation business logic
- [x] Create invitation (success, duplicate)
- [x] Retrieve invitation (by ID, by token, by event, by guest)
- [x] Validate invitation (phone/email matching for non-shareability)
- [x] Update invitation status (sent, delivered, opened, used)
- [x] Generate QR codes
- [x] Delete invitations
- [x] Bulk retrieval operations

**Key Test Cases**:
```java
✓ testCreateInvitation_Success
✓ testCreateInvitation_AlreadyExists_ThrowsException
✓ testValidateInvitation_Success
✓ testValidateInvitation_InvalidPhone_ThrowsException
✓ testValidateInvitation_InvalidEmail_ThrowsException
✓ testGenerateQrCode_Success
✓ testGenerateQrCode_WithCustomUrl_Success
```

#### UserServiceImplTest (10 tests)
Focus: User management and authentication
- [x] Create user (success, duplicate email)
- [x] Retrieve user (by ID, by email)
- [x] Authenticate user (valid/invalid credentials)
- [x] Update user
- [x] Delete user

**Key Test Cases**:
```java
✓ testCreateUser_Success
✓ testCreateUser_EmailAlreadyExists_ThrowsException
✓ testAuthenticate_Success
✓ testAuthenticate_InvalidCredentials
```

#### BulkUploadServiceImplTest (13 tests)
Focus: Bulk processing and file uploads
- [x] Process bulk upload (success, with failures)
- [x] Empty file handling
- [x] Session management (retrieve, by event, by user)
- [x] Guest data processing

**Key Test Cases**:
```java
✓ testProcessBulkUpload_Success
✓ testProcessBulkUpload_WithFailures
✓ testProcessBulkUpload_EmptyFile_ThrowsException
✓ testGetUploadSession_Success
✓ testGetUploadSessionsByEvent_Success
✓ testGetUploadSessionsByUser_Success
```

### Utility Services Tests

#### QRCodeServiceTest (8 tests)
Focus: QR code generation
- [x] Generate QR code from text
- [x] Generate invitation-specific QR codes
- [x] Handle various input lengths
- [x] Base64 encoding validation
- [x] Special characters handling

**Key Test Cases**:
```java
✓ testGenerateQRCodeImage_Success
✓ testGenerateQRCodeImage_WithLongData
✓ testGenerateQRCodeImage_WithSpecialCharacters
✓ testGenerateInvitationQRCode_Success
```

#### PDFServiceTest (8 tests)
Focus: PDF receipt generation
- [x] Generate receipt PDFs
- [x] Handle optional/additional data
- [x] Special characters in PDF
- [x] Large additional data sets
- [x] Base64 encoding

**Key Test Cases**:
```java
✓ testGenerateReceiptPDF_Success
✓ testGenerateReceiptPDF_WithoutAdditionalData
✓ testGenerateReceiptPDF_WithSpecialCharacters
✓ testGenerateReceiptPDF_WithLargeAdditionalData
```

#### ExcelParserServiceTest (9 tests)
Focus: Excel file parsing
- [x] Parse Excel files
- [x] Validate required fields
- [x] Invalid file type handling
- [x] Empty file handling
- [x] Expected headers validation

**Key Test Cases**:
```java
✓ testGetExpectedHeaders
✓ testParseExcelFile_InvalidFileType
✓ testParseExcelBytes_InvalidBytes
✓ testGetExpectedHeaders_Immutable
```

#### EmailServiceTest (13 tests)
Focus: Email composition and sending
- [x] Send simple text emails
- [x] Send HTML formatted emails
- [x] Invitation emails
- [x] Receipt emails
- [x] Bulk upload confirmations
- [x] Event reminder emails
- [x] Special characters handling
- [x] Sequential email sending

**Key Test Cases**:
```java
✓ testSendSimpleEmail_Success
✓ testSendHtmlEmail_Success
✓ testSendInvitationEmail_Success
✓ testSendBulkUploadConfirmation_Success
✓ testSendEventReminderEmail_Success
✓ testSendMultipleEmails_Sequentially
```

### Controller Tests

#### InvitationControllerTest (14 tests)
Focus: REST API endpoints for invitations
- [x] GET /api/v1/invitations/{id}
- [x] GET /api/v1/invitations/token/{token}
- [x] GET /api/v1/invitations/event/{eventId}
- [x] PATCH /api/v1/invitations/{id}/mark-sent
- [x] PATCH /api/v1/invitations/{id}/mark-delivered
- [x] PATCH /api/v1/invitations/{id}/mark-opened
- [x] PATCH /api/v1/invitations/{id}/mark-used
- [x] POST /api/v1/invitations/{id}/qrcode
- [x] DELETE /api/v1/invitations/{id}

**Key Test Cases**:
```java
✓ testGetInvitationById_Success
✓ testGetInvitationByToken_Success
✓ testGetInvitationByToken_InvalidPhone
✓ testMarkAsSent_Success
✓ testGenerateQrCode_Success
✓ testDeleteInvitation_Success
```

#### BulkUploadControllerTest (11 tests)
Focus: Bulk upload REST endpoints
- [x] GET /api/v1/bulk-uploads/session/{id}
- [x] GET /api/v1/bulk-uploads/event/{eventId}
- [x] GET /api/v1/bulk-uploads/user/{uploadedBy}
- [x] GET /api/v1/bulk-uploads (all sessions)
- [x] Error handling for missing sessions

**Key Test Cases**:
```java
✓ testGetUploadSession_Success
✓ testGetUploadSessionsByEvent_Success
✓ testGetUploadSessionsByEvent_Empty
✓ testSessionDtoMapping
```

#### UserControllerTest (12 tests)
Focus: User management endpoints
- [x] POST /api/v1/users/register
- [x] POST /api/v1/users/login
- [x] GET /api/v1/users/{id}
- [x] GET /api/v1/users/email/{email}
- [x] PUT /api/v1/users/{id}
- [x] DELETE /api/v1/users/{id}

**Key Test Cases**:
```java
✓ testRegisterUser_Success
✓ testRegisterUser_DuplicateEmail
✓ testLoginUser_Success
✓ testLoginUser_InvalidCredentials
✓ testUpdateUser_Success
✓ testLoginUser_WithDifferentRoles
```

---

## Test Patterns

### 1. Arrange-Act-Assert Pattern
```java
@Test
void testCreateInvitation_Success() {
    // Arrange: Setup test data and mocks
    when(invitationRepository.findByEventIdAndGuestId(...))
            .thenReturn(Optional.empty());
    when(invitationRepository.save(any(Invitation.class)))
            .thenReturn(invitation);

    // Act: Execute the method under test
    InvitationResponseDto response = invitationService.createInvitation(requestDto);

    // Assert: Verify the results
    assertNotNull(response);
    assertEquals(invitationId, response.getId());
    verify(invitationRepository, times(1)).save(any());
}
```

### 2. Mocking Dependencies
```java
@Mock
private InvitationRepository invitationRepository;

@Mock
private QRCodeService qrCodeService;

@Mock
private EmailService emailService;

@InjectMocks
private InvitationServiceImpl invitationService;
```

### 3. MockMvc for Controller Testing
```java
@Test
void testGetInvitationById_Success() throws Exception {
    when(invitationService.getInvitationById(invitationId))
            .thenReturn(invitationDetailedResponseDto);

    mockMvc.perform(get("/api/v1/invitations/{invitationId}", invitationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId.toString()));
}
```

### 4. Exception Testing
```java
@Test
void testCreateInvitation_AlreadyExists_ThrowsException() {
    when(invitationRepository.findByEventIdAndGuestId(...))
            .thenReturn(Optional.of(invitation));

    assertThrows(IllegalArgumentException.class, 
        () -> invitationService.createInvitation(requestDto));
}
```

### 5. Verify Method Invocation
```java
verify(emailService, times(1))
        .sendInvitationEmail(anyString(), anyString(), anyString(), anyString());

verify(invitationRepository, never()).save(any());
```

---

## Troubleshooting

### Test Failures

#### "No qualifying bean of type..."
**Problem**: Spring context not loading properly
**Solution**: Add `@ExtendWith(MockitoExtension.class)` to test class

#### "NullPointerException"
**Problem**: Mock not initialized
**Solution**: Ensure `@Mock` and `@InjectMocks` are used correctly

#### "mockMvc is null"
**Problem**: MockMvc not setup
**Solution**: Add initialization in `@BeforeEach`:
```java
mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
```

#### "Invocation count did not match"
**Problem**: Mock method called different number of times
**Solution**: Use `verify()` with `atLeastOnce()`, `atMost()`, etc.

### Debugging Tests

1. **Add println statements**:
```java
System.out.println("Actual value: " + response);
```

2. **Use debugger breakpoints**: Set breakpoint and run test in debug mode

3. **Check mock behavior**:
```java
when(method()).thenReturn(value);
// verify it was called
verify(mock, times(1)).method();
```

### Common Issues

| Issue | Solution |
|-------|----------|
| Tests won't compile | Check imports, ensure JUnit 5 is in pom.xml |
| Email tests timeout | Mock the JavaMailSender |
| File parsing tests fail | Check file format, use byte arrays |
| Controller tests return 5xx | Verify mock setup matches method signature |

---

## Best Practices

1. **One assertion per test** (or related assertions)
2. **Clear test names**: `test[Method]_[Scenario]_[Expected]`
3. **Use constants for test data**
4. **Mock external dependencies**
5. **Test happy path AND error cases**
6. **Keep tests independent**: No test should depend on another
7. **Use `@BeforeEach`** for common setup

---

## Generating Coverage Report

```bash
# Run tests with JaCoCo coverage
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

**Coverage Goals**:
- Service Layer: 80%+
- Controller Layer: 70%+
- Utility Services: 85%+

---

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Run Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
      - run: mvn test
      - run: mvn jacoco:report
      - uses: codecov/codecov-action@v2
```

---

## Next Steps

1. Run tests locally: `mvn test`
2. Review coverage report: `mvn jacoco:report`
3. Add tests for new features before implementation (TDD)
4. Maintain >80% code coverage
5. Fix failing tests immediately

