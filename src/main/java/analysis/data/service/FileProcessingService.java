package analysis.data.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class FileProcessingService {

    String cfInsider = "C:\\Users\\singh\\Desktop\\Data\\CF-Insider-Trading-equities-06-04-2024-to-06-07-2024.csv";
    String cfSastReg = "C:\\Users\\singh\\Desktop\\Data\\CF-SAST- Reg29-06-Jul-2024.csv";
    String cfSastPledged = "C:\\Users\\singh\\Desktop\\Data\\CF-SAST-Pledged-Data-06-Jul-2024.csv";
    String cfShareHolding = "C:\\Users\\singh\\Desktop\\Data\\CF-Shareholding-Pattern-equities-06-04-2024-to-06-07-2024.csv";
    String equityL = "C:\\Users\\singh\\Desktop\\Data\\EQUITY_L.csv";
    String secBhav = "C:\\Users\\singh\\Desktop\\Data\\sec_bhav.csv";

    private List<String[]> cfInsiderData;
    private List<String[]> cfSastRegData;
    private List<String[]> cfSastPledgedData;
    private List<String[]> cfShareHoldingData;
    private List<String[]> equityLData;
    private List<String[]> secBhavData;

    public void processFiles() {
        cfInsiderData = readCsv(cfInsider);
        cfSastRegData = readCsv(cfSastReg);
        cfSastPledgedData = readCsv(cfSastPledged);
        cfShareHoldingData = readCsv(cfShareHolding);
        equityLData = readCsv(equityL);
        secBhavData = readCsv(secBhav);

        List<List<String>> firstColumns = new ArrayList<>();
        firstColumns.add(getFirstColumn(cfInsiderData));
        firstColumns.add(getFirstColumn(cfSastRegData));
        firstColumns.add(getFirstColumn(cfSastPledgedData));
        firstColumns.add(getFirstColumn(cfShareHoldingData));
        firstColumns.add(getFirstColumn(equityLData));
        firstColumns.add(getFirstColumn(secBhavData));

        writeToExcel(firstColumns, "C:\\Users\\singh\\Desktop\\Data\\output.xlsx");
    }

    private List<String[]> readCsv(String filePath) {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = reader.readAll();

            if (!records.isEmpty()) {
                String[] headers = records.get(0); // First row contains column names
                System.out.println("Column Names for " + filePath + ": " + Arrays.toString(headers));
            } else {
                System.out.println("No data found in the CSV file: " + filePath);
            }

            return records;
        } catch (IOException | CsvException e) {
            e.printStackTrace();
            System.out.println("Error reading the CSV file: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    private List<String> getFirstColumn(List<String[]> data) {
        List<String> firstColumn = new ArrayList<>();
        if (data != null && !data.isEmpty()) {
            for (String[] row : data) {
                firstColumn.add(row[0]);
            }
        }
        return firstColumn;
    }

    private void writeToExcel(List<List<String>> columns, String filePath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("First Columns");

        int rowNum = 0;
        int maxRowSize = columns.stream().mapToInt(List::size).max().orElse(0);

        for (int i = 0; i < maxRowSize; i++) {
            Row row = sheet.createRow(rowNum++);
            for (int j = 0; j < columns.size(); j++) {
                Cell cell = row.createCell(j);
                if (i < columns.get(j).size()) {
                    cell.setCellValue(columns.get(j).get(i));
                } else {
                    cell.setCellValue(""); // Handle the case where the column has fewer rows
                }
            }
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
