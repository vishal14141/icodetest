package com.icodetest.icodetest.controller;

import com.icodetest.icodetest.service.SchemaService;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
public class SchemaRequest {

    @Autowired
    SchemaService schemaService;


    @GetMapping(value = "/home")
    public String abc(){
        return "home";
    }

    @PostMapping("/generateSchema")
    public String createSchema(@RequestParam(name = "columnName") List<String> columnNames, HttpServletRequest request, HttpSession session){
        if (columnNames.isEmpty()) {
            return "schema";
        }
        try {
            String organizationId;
            if(session.getAttribute("organizationId") == null ||session.getAttribute("organizationId").toString() == ""){
                organizationId = UUID.randomUUID().toString();
            } else{
                organizationId = session.getAttribute("organizationId").toString();
            }

            schemaService.processSchema(columnNames, organizationId);
            session.setAttribute("organizationId", organizationId);

        } catch (Exception e) {
            return "redirect:/home";
        }
        return "schema";
    }

    @PostMapping("/processTemplate")
    public String processTemplateData(@RequestParam("file") MultipartFile file, HttpSession session, Model model) {
        try {
            if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xls") && !file.getOriginalFilename().endsWith(".xlsx")) {
                return "Invalid file format. Please upload an Excel file.";
            }
            if(session.getAttribute("organizationId") == null){
                return "home";
            }
            XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
            XSSFSheet sheet = workbook.getSheetAt(0);

            String organizationId = session.getAttribute("organizationId").toString();
            schemaService.processUpdatedExcel(sheet, organizationId);

            List<String> headers = schemaService.getHeadersFromSheet(sheet);
            List<List<String>> excelData = schemaService.getExcelDataFromSheet(sheet, headers);

            model.addAttribute("headers", headers);
            model.addAttribute("excelData", excelData);
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "schema";
    }

    @GetMapping("/download/excel")
    public void downloadSchema(HttpServletResponse res, HttpSession session){
        try {

            if(session.getAttribute("organizationId") == null){
                return;
            }
            res.setHeader("Content-Disposition", "attachment; filename=template_data.xlsx");
            res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            String orgId = session.getAttribute("organizationId").toString();
            XSSFWorkbook workbook =  schemaService.getWorkBookByOrganizationId(orgId);
            if(workbook == null) {
                return;
            }
            workbook.write(res.getOutputStream());
            workbook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
