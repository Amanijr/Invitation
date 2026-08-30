package com.InvitationSystem.InvitationSystem.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class ExcelParserService {

    /**
     * Parse uploaded file (CSV, XLSX, XLS) and extract guest data without throwing on missing fields.
     */
    public List<Map<String, String>> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }
        try (InputStream is = file.getInputStream()) {
            return parseSpreadsheet(is, file.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    /**
     * Parse byte array (CSV, XLSX, XLS) and extract guest data.
     */
    public List<Map<String, String>> parseBytes(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("File byte array is empty");
        }
        try (InputStream is = new ByteArrayInputStream(fileBytes)) {
            return parseSpreadsheet(is, fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy parse Excel file
     */
    public List<Map<String, String>> parseExcelFile(MultipartFile file) {
        return parseFile(file);
    }

    /**
     * Legacy parse Excel bytes
     */
    public List<Map<String, String>> parseExcelBytes(byte[] fileBytes) {
        return parseBytes(fileBytes, "sample.xlsx");
    }

    public List<Map<String, String>> parseSpreadsheet(InputStream inputStream, String fileName) throws IOException {
        String lowerName = (fileName != null) ? fileName.toLowerCase() : "";

        if (lowerName.endsWith(".csv")) {
            return parseCsv(inputStream);
        } else if (lowerName.endsWith(".xls")) {
            try (Workbook workbook = new HSSFWorkbook(inputStream)) {
                return parseWorkbook(workbook);
            }
        } else {
            try (Workbook workbook = new XSSFWorkbook(inputStream)) {
                return parseWorkbook(workbook);
            }
        }
    }

    private List<Map<String, String>> parseCsv(InputStream inputStream) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            List<String> headers = null;
            int rowNum = 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] tokens = parseCsvLine(line);
                rowNum++;

                if (headers == null) {
                    headers = new ArrayList<>();
                    for (String t : tokens) {
                        headers.add(normalizeHeader(t));
                    }
                } else {
                    Map<String, String> rowMap = new HashMap<>();
                    boolean hasContent = false;
                    for (int i = 0; i < Math.min(tokens.length, headers.size()); i++) {
                        String val = tokens[i].trim();
                        if (!val.isEmpty()) {
                            rowMap.put(headers.get(i), val);
                            hasContent = true;
                        }
                    }
                    if (hasContent) {
                        rowMap.put("_rowNumber", String.valueOf(rowNum));
                        rows.add(rowMap);
                    }
                }
            }
        }
        return rows;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString().trim());
        return values.toArray(new String[0]);
    }

    private List<Map<String, String>> parseWorkbook(Workbook workbook) {
        List<Map<String, String>> rowsData = new ArrayList<>();
        if (workbook.getNumberOfSheets() == 0) return rowsData;

        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.iterator();

        Map<Integer, String> columnHeaders = new HashMap<>();
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            for (Cell cell : headerRow) {
                String headerVal = getCellValueAsString(cell).trim();
                if (!headerVal.isEmpty()) {
                    columnHeaders.put(cell.getColumnIndex(), normalizeHeader(headerVal));
                }
            }
        }

        int rowNum = 1;
        while (rowIterator.hasNext()) {
            rowNum++;
            Row row = rowIterator.next();
            Map<String, String> rowMap = new HashMap<>();
            boolean hasContent = false;

            for (Cell cell : row) {
                String headerKey = columnHeaders.getOrDefault(cell.getColumnIndex(), "col_" + cell.getColumnIndex());
                String cellVal = getCellValueAsString(cell).trim();

                if (!cellVal.isEmpty()) {
                    rowMap.put(headerKey, cellVal);
                    hasContent = true;
                }
            }

            if (hasContent) {
                rowMap.put("_rowNumber", String.valueOf(rowNum));
                rowsData.add(rowMap);
            }
        }

        return rowsData;
    }

    private String normalizeHeader(String rawHeader) {
        if (rawHeader == null) return "";
        String clean = rawHeader.trim().toLowerCase().replaceAll("[^a-z0-9]", "");

        if (clean.contains("fullname") || clean.contains("guestname") || clean.equals("name")) {
            return "fullName";
        }
        if (clean.contains("phone") || clean.contains("mobile") || clean.contains("whatsapp") || clean.contains("sms")) {
            return "phone";
        }
        if (clean.contains("email") || clean.contains("mail")) {
            return "email";
        }
        return rawHeader.trim();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                long val = (long) cell.getNumericCellValue();
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    public List<String> getExpectedHeaders() {
        return Arrays.asList("fullName", "phone", "email");
    }
}
