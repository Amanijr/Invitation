package com.InvitationSystem.InvitationSystem.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class ExcelParserService {

    /**
     * Parse Excel file and extract guest data
     * @param file The Excel file to parse
     * @return List of maps containing guest data (email, phone, name, etc.)
     */
    public List<Map<String, String>> parseExcelFile(MultipartFile file) {
        List<Map<String, String>> guests = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Get header row to determine column mapping
            Map<Integer, String> columnHeaders = new HashMap<>();
            if (rows.hasNext()) {
                Row headerRow = rows.next();
                for (Cell cell : headerRow) {
                    columnHeaders.put(cell.getColumnIndex(), cell.getStringCellValue().toLowerCase());
                }
            }

            // Parse data rows
            int rowNumber = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                Map<String, String> guestData = new HashMap<>();

                boolean hasData = false;
                for (Cell cell : row) {
                    String columnHeader = columnHeaders.getOrDefault(cell.getColumnIndex(), "column_" + cell.getColumnIndex());
                    String cellValue = getCellValueAsString(cell);
                    
                    if (cellValue != null && !cellValue.isEmpty()) {
                        guestData.put(columnHeader, cellValue);
                        hasData = true;
                    }
                }

                if (hasData) {
                    validateGuestData(guestData, rowNumber);
                    guests.add(guestData);
                }
                rowNumber++;
            }

            return guests;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse Excel file from byte array
     * @param fileBytes The Excel file bytes
     * @return List of maps containing guest data
     */
    public List<Map<String, String>> parseExcelBytes(byte[] fileBytes) {
        List<Map<String, String>> guests = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(fileBytes))) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // Get header row
            Map<Integer, String> columnHeaders = new HashMap<>();
            if (rows.hasNext()) {
                Row headerRow = rows.next();
                for (Cell cell : headerRow) {
                    columnHeaders.put(cell.getColumnIndex(), cell.getStringCellValue().toLowerCase());
                }
            }

            // Parse data rows
            int rowNumber = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                Map<String, String> guestData = new HashMap<>();

                boolean hasData = false;
                for (Cell cell : row) {
                    String columnHeader = columnHeaders.getOrDefault(cell.getColumnIndex(), "column_" + cell.getColumnIndex());
                    String cellValue = getCellValueAsString(cell);
                    
                    if (cellValue != null && !cellValue.isEmpty()) {
                        guestData.put(columnHeader, cellValue);
                        hasData = true;
                    }
                }

                if (hasData) {
                    validateGuestData(guestData, rowNumber);
                    guests.add(guestData);
                }
                rowNumber++;
            }

            return guests;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Get cell value as string, handling different cell types
     */
    private String getCellValueAsString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    /**
     * Validate guest data has required fields
     */
    private void validateGuestData(Map<String, String> guestData, int rowNumber) {
        List<String> requiredFields = Arrays.asList("email", "phone");
        List<String> missingFields = new ArrayList<>();

        for (String field : requiredFields) {
            if (!guestData.containsKey(field) || guestData.get(field).isEmpty()) {
                missingFields.add(field);
            }
        }

        if (!missingFields.isEmpty()) {
            throw new IllegalArgumentException("Row " + rowNumber + " is missing required fields: " + String.join(", ", missingFields));
        }
    }

    /**
     * Get expected column headers
     */
    public List<String> getExpectedHeaders() {
        return Arrays.asList("email", "phone", "name", "company", "notes");
    }
}
