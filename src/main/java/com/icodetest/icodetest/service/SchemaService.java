package com.icodetest.icodetest.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.util.*;

@Service
public class SchemaService {

    @Autowired
    MongoTemplate mongoTemplate;
    private static final String COLLECTION_NAME = "organization";


    public void processSchema(List<String> columnNames, String organizationId) {

        if (!mongoTemplate.collectionExists(COLLECTION_NAME)) {
            mongoTemplate.createCollection(COLLECTION_NAME);
        }
        Document schema = new Document();
        for (String columnName : columnNames) {
            schema.put(columnName, "");
        }
        schema.put("organizationId", organizationId);
        mongoTemplate.getCollection(COLLECTION_NAME).insertOne(schema);
    }

    public void processUpdatedExcel(XSSFSheet sheet, String organizationId) {

        for (Row row : sheet) {
            if (row.getRowNum() == 0) {
                continue;
            }

            Cell objectIdCell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            String objectId = objectIdCell.getStringCellValue();

            Document document;

            Query query = new Query(Criteria.where("_id").is(objectId).and("organizationId").is(organizationId));
            List<Document> documents = mongoTemplate.find(query, Document.class, COLLECTION_NAME);
            if(!documents.isEmpty()){
                document = documents.get(0);
            } else{
                document = new Document();
                document.put("organizationId", organizationId);
            }

            for (int cellIndex = 1; cellIndex < row.getLastCellNum(); cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                String header = getHeaderFromColumnIndex(sheet, cellIndex);
                String cellValue = cell.getStringCellValue();
                document.put(header, cellValue);
            }
            mongoTemplate.save(document, COLLECTION_NAME);
        }
    }

    private String getHeaderFromColumnIndex(XSSFSheet sheet, int columnIndex) {
        // Assuming the header row is the first row in the sheet
        Row headerRow = sheet.getRow(0);
        Cell headerCell = headerRow.getCell(columnIndex);
        return headerCell.getStringCellValue();
    }

    public List<Document> getSchemaByOrganizationId(String organizationId) {
        Query query = new Query(Criteria.where("organizationId").is(organizationId));
        return mongoTemplate.find(query, Document.class, COLLECTION_NAME);
    }

    public XSSFWorkbook getWorkBookByOrganizationId(String organizationId) {

        List<Document> schemas = getSchemaByOrganizationId(organizationId);
        return createExcelWorkbook(schemas);
    }

    private static XSSFWorkbook createExcelWorkbook(List<Document> data) {
        XSSFWorkbook workbook = new XSSFWorkbook();

        if (!data.isEmpty()) {
            try {
                Sheet sheet = workbook.createSheet("User Data");

                // Create header row with dynamic column names
                Row headerRow = sheet.createRow(0);
                Set<String> allColumnNames = extractAllColumnNames(data);

                CellStyle nonEditableStyle = workbook.createCellStyle();
                nonEditableStyle.setLocked(true);
//                sheet.protectSheet("password");

                int columnIndex = 0;
                for (String columnName : allColumnNames) {
                    if (!Objects.equals(columnName, "organizationId")) {
                        Cell cell = headerRow.createCell(columnIndex);
                        cell.setCellValue(columnName);

                        if (Objects.equals(columnName, "_id")) {
                            for (int rowIndex = 1; rowIndex <= data.size(); rowIndex++) {
                                Row row = sheet.createRow(rowIndex);
                                Cell idCell = row.createCell(columnIndex);
                                idCell.setCellValue(data.get(rowIndex - 1).get("_id").toString());
                                idCell.setCellStyle(nonEditableStyle);
                            }
                        } else {
                            for (int rowIndex = 1; rowIndex <= data.size(); rowIndex++) {
                                Row row = sheet.getRow(rowIndex);
                                if (row == null) {
                                    row = sheet.createRow(rowIndex);
                                }
                                Cell dataCell = row.createCell(columnIndex);
                                if (data.get(rowIndex - 1).containsKey(columnName)) {
                                    dataCell.setCellValue(data.get(rowIndex - 1).get(columnName).toString());
                                } else {
                                    dataCell.setCellValue("");
                                }
                            }
                        }
                        columnIndex++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return workbook;
    }


    private static Set<String> extractAllColumnNames(List<Document> mongoData) {
        Set<String> allColumnNames = new LinkedHashSet<>();
        for (Document document : mongoData) {
            allColumnNames.addAll(document.keySet());
        }
        return allColumnNames;
    }

    public List<List<String>> getExcelDataFromSheet(XSSFSheet sheet, List<String> headers){
        List<List<String>> excelData = new ArrayList<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row dataRow = sheet.getRow(rowIndex);
            List<String> rowData = new ArrayList<>();
            for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                Cell cell = dataRow.getCell(cellIndex);
                rowData.add(cell.getStringCellValue());
            }
            excelData.add(rowData);
        }
        return excelData;
    }

    public List<String> getHeadersFromSheet(XSSFSheet sheet){
        Row headerRow = sheet.getRow(0);
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = cell.getStringCellValue();
                headers.add(headerValue);
            }
        }
        return headers;
    }
}
