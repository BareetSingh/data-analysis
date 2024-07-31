package analysis.data.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileProcessingService2 {

    private static final String INSIDER_FILE_PREFIX = "CF-Insider";
    private static final String SAST_REG_FILE_PREFIX = "CF-SAST- Reg";
    private static final String SAST_PLEDGED_FILE_PREFIX = "CF-SAST-Pledged";
    private static final String SHAREHOLDING_FILE_PREFIX = "CF-Shareholding-";
    private static final String NAME_LOOKUP_FILE_PREFIX = "EQUITY_L";
    private static final String BHAV_FILE_PREFIX = "sec_bhav";

    public void processFiles() throws IOException {
        Map<String, String> files = checkForFiles();

        List<Map<String, String>> insiderData = readCsv(files.get(INSIDER_FILE_PREFIX));
        List<Map<String, String>> bhavData = readCsv(files.get(BHAV_FILE_PREFIX));

        // Process data according to the logic
        List<Map<String, Object>> processedData = processInsiderData(insiderData, bhavData);

        // Write to Excel
        writeToExcel(processedData);
    }

    private Map<String, String> checkForFiles() throws FileNotFoundException {
        String userDir = System.getProperty("user.dir"); // Get user.dir property
        File folder = new File(userDir);
        File[] listOfFiles = folder.listFiles();
        Map<String, String> files = new HashMap<>();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String fileName = file.getName();
                System.out.println("Detected file: " + fileName);  // Debug print statement

                if (fileName.startsWith(INSIDER_FILE_PREFIX)) {
                    files.put(INSIDER_FILE_PREFIX, fileName);
                } else if (fileName.startsWith(SAST_REG_FILE_PREFIX)) {
                    files.put(SAST_REG_FILE_PREFIX, fileName);
                } else if (fileName.startsWith(SAST_PLEDGED_FILE_PREFIX)) {
                    files.put(SAST_PLEDGED_FILE_PREFIX, fileName);
                } else if (fileName.startsWith(SHAREHOLDING_FILE_PREFIX)) {
                    files.put(SHAREHOLDING_FILE_PREFIX, fileName);
                } else if (fileName.startsWith(NAME_LOOKUP_FILE_PREFIX)) {
                    files.put(NAME_LOOKUP_FILE_PREFIX, fileName);
                } else if (fileName.startsWith(BHAV_FILE_PREFIX)) {
                    files.put(BHAV_FILE_PREFIX, fileName);
                }
            }
        }

        System.out.println("Total files matched: " + files.size());  // Debug print statement

        if (files.size() != 6) {
            throw new FileNotFoundException("One or more required files are missing.");
        }

        return files;
    }



    private List<Map<String, String>> readCsv(String fileName) throws IOException {
        List<Map<String, String>> data = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(Paths.get(fileName));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord csvRecord : csvParser) {
                Map<String, String> record = csvRecord.toMap();
                data.add(record);
            }
        }
        return data;
    }

    private List<Map<String, Object>> processInsiderData(List<Map<String, String>> insiderData, List<Map<String, String>> bhavData) {
        // Filter and transform insider data as per the original Python script logic
        List<Map<String, String>> filteredData = insiderData.stream()
                .filter(record -> "Promoters".equals(record.get("CATEGORY OF PERSON")) || "Promoter Group".equals(record.get("CATEGORY OF PERSON")))
                .filter(record -> "Market Purchase".equals(record.get("MODE OF ACQUISITION")))
                .collect(Collectors.toList());

        Map<String, Double> symbolToTotalValue = filteredData.stream()
                .collect(Collectors.groupingBy(record -> record.get("SYMBOL"),
                        Collectors.summingDouble(record -> Double.parseDouble(record.get("VALUE OF SECURITY (ACQUIRED/DISPLOSED)")))));

        List<Map<String, Object>> processedData = new ArrayList<>();

        for (String symbol : symbolToTotalValue.keySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("SYMBOL", symbol);
            row.put("VALUE OF SECURITY (ACQUIRED/DISPLOSED)", symbolToTotalValue.get(symbol));

            Optional<Map<String, String>> bhavRecord = bhavData.stream().filter(record -> symbol.equals(record.get("SYMBOL"))).findFirst();
            if (bhavRecord.isPresent()) {
                String dateStr = bhavRecord.get().get(" DATE1").trim();
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
                row.put("DATE", date);
                row.put("CLOSE_PRICE", Double.parseDouble(bhavRecord.get().get("CLOSE_PRICE")));
            } else {
                row.put("DATE", null);
                row.put("CLOSE_PRICE", null);
            }

            // Add further processing as needed
            processedData.add(row);
        }

        return processedData;
    }

    private void writeToExcel(List<Map<String, Object>> data) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Processed Data");

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"SYMBOL", "VALUE OF SECURITY (ACQUIRED/DISPLOSED)", "DATE", "CLOSE_PRICE"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        for (Map<String, Object> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue((String) rowData.get("SYMBOL"));
            row.createCell(1).setCellValue((Double) rowData.get("VALUE OF SECURITY (ACQUIRED/DISPLOSED)"));
            if (rowData.get("DATE") != null) {
                row.createCell(2).setCellValue(((LocalDate) rowData.get("DATE")).toString());
            }
            if (rowData.get("CLOSE_PRICE") != null) {
                row.createCell(3).setCellValue((Double) rowData.get("CLOSE_PRICE"));
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream("swing_trading_output.xlsx")) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
