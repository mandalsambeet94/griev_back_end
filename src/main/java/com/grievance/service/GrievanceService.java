package com.grievance.service;

import com.grievance.dto.AttachmentDTO;
import com.grievance.dto.GrievanceDTO;
import com.grievance.dto.GrievanceFilter;
import com.grievance.dto.GrievanceRequest;
import com.grievance.entity.Attachment;
import com.grievance.entity.Grievance;
import com.grievance.entity.User;
import com.grievance.exception.ResourceNotFoundException;
import com.grievance.exception.UnauthorizedException;
import com.grievance.repository.AttachmentRepository;
import com.grievance.repository.GrievanceRepository;
import com.grievance.repository.GrievanceSpecification;

import com.grievance.utility.DateFormatter;
import com.lowagie.text.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// POI (Excel)
/*import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;*/

// OpenPDF (PDF)
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.util.StringUtils;
/*import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;

import java.awt.Color;*/
import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final AttachmentRepository attachmentRepository;
    private final AuthService authService;
    private final S3Service s3Service;

    @Transactional
    public GrievanceDTO saveGrievance(GrievanceRequest request) {
        User currentUser = authService.getCurrentUser();

        Grievance grievance = new Grievance();
        mapRequestToEntity(request, grievance);

        // Set collected by current user
        grievance.setCollectedBy(currentUser);
        //grievance.setAgentName(currentUser.getName());

        // Set default status
        if (grievance.getStatus() == null) {
            grievance.setStatus(Grievance.GrievanceStatus.PENDING);
        }

        if (!StringUtils.hasText(grievance.getGp())) {
            grievance.setGp("N/A");
        }

        // Save grievance
        Grievance savedGrievance = grievanceRepository.save(grievance);

        // Save attachments if provided
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            saveAttachments(savedGrievance, request.getAttachments());
        }

        return GrievanceDTO.fromEntity(savedGrievance);
    }

    @Transactional
    public GrievanceDTO updateGrievance(Long grievanceId, GrievanceRequest request) {
        User currentUser = authService.getCurrentUser();

        // Check if user is admin
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can update grievances");
        }

        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        mapRequestToEntity(request, grievance);

        // Update timestamp
        grievance.setUpdatedAt(LocalDateTime.now());

        Grievance updatedGrievance = grievanceRepository.save(grievance);

        // Update attachments if provided
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            // Delete existing attachments
            attachmentRepository.deleteByGrievance_GrievanceId(grievanceId);
            // Save new attachments
            saveAttachments(updatedGrievance, request.getAttachments());
        }

        return GrievanceDTO.fromEntity(updatedGrievance);
    }

    public GrievanceDTO getGrievanceById(Long grievanceId) {
        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance not found"));

        // Load attachments
        List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(grievanceId);
        grievance.setAttachments(attachments);

        return GrievanceDTO.fromEntity(grievance);
    }

    public Page<GrievanceDTO> getAllGrievances(Pageable pageable) {
        User currentUser = authService.getCurrentUser();

        Page<Grievance> grievances;

        // If user is agent, only show their grievances
        if (currentUser.getRole().equals(User.Role.AGENT)) {
            grievances = grievanceRepository.findByCollectedBy(currentUser, pageable);
        } else {
            // Admin can see all
            grievances = grievanceRepository.findAll(pageable);
        }

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(
                    grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.map(GrievanceDTO::fromEntity);
    }

    /*public List<GrievanceDTO> getGrievancesByFilters(String block, String gp,
                                                     String villageSahi, String name) {
        List<Grievance> grievances = grievanceRepository.findByFilters(block, gp, villageSahi, name);

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments = attachmentRepository.findByGrievance_GrievanceId(
                    grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList());
    }*/

    public List<GrievanceDTO> getGrievancesByFilters(
            String block,
            String gp,
            String villageSahi,
            String wardNo,
            LocalDate startDate,
            LocalDate endDate,
            Grievance.GrievanceStatus status,
            String name, String contact, String fatherSpouseName) {

        List<Grievance> grievances ;
        LocalDateTime startOfDay = LocalDateTime.of(1970, 1, 1, 0, 0);;
        LocalDateTime endOfDay =LocalDateTime.of(3000, 1, 1, 0, 0);;


        if (null !=  startDate && null != endDate) {
            startOfDay = startDate.atStartOfDay();
            endOfDay = endDate.atStartOfDay();
        }

        if (status == null) {
            grievances = grievanceRepository.findWithoutStatus(
                    block, gp, villageSahi, wardNo,startOfDay,endOfDay,contact,fatherSpouseName, name
            );
        } else {
            grievances = grievanceRepository.findWithStatus(
                    block, gp, villageSahi, wardNo, startOfDay,endOfDay,contact,fatherSpouseName, status, name
            );
        }

        // Load attachments for each grievance
        grievances.forEach(grievance -> {
            List<Attachment> attachments =
                    attachmentRepository.findByGrievance_GrievanceId(
                            grievance.getGrievanceId());
            grievance.setAttachments(attachments);
        });

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .collect(Collectors.toList());
    }


    public List<GrievanceDTO> getGrievances(GrievanceFilter filter) {
        List<Grievance> grievances =
                grievanceRepository.findAll(
                        GrievanceSpecification.filter(filter)
                );

        return grievances.stream()
                .map(GrievanceDTO::fromEntity)
                .toList();
    }


    @Transactional
    public void deleteGrievances(List<Long> grievanceIds) {

        User currentUser = authService.getCurrentUser();

        // Role check
        if (!currentUser.getRole().equals(User.Role.ADMIN) &&
                !currentUser.getRole().equals(User.Role.SUPER_ADMIN)) {
            throw new UnauthorizedException("Only admin can delete grievances");
        }

        if (grievanceIds == null || grievanceIds.isEmpty()) {
            throw new IllegalArgumentException("Grievance ID list cannot be empty");
        }

        // Fetch all grievances at once
        List<Grievance> grievances = grievanceRepository.findAllById(grievanceIds);

        if (grievances.size() != grievanceIds.size()) {
            throw new ResourceNotFoundException("One or more grievances not found");
        }

        // Collect all S3 keys first
        List<String> s3Keys = grievances.stream()
                .filter(g -> g.getAttachments() != null)
                .flatMap(g -> g.getAttachments().stream())
                .map(Attachment::getS3Key)
                .toList();

        // Delete from S3
        for (String key : s3Keys) {
            s3Service.deleteFile(key);
        }

        // Delete attachments in bulk
        attachmentRepository.deleteByGrievance_GrievanceIdIn(grievanceIds);

        // Delete grievances in bulk
        grievanceRepository.deleteAllById(grievanceIds);
    }

    private void mapRequestToEntity(GrievanceRequest request, Grievance grievance) {
        grievance.setBlock(request.getBlock());
        grievance.setGp(request.getGp());
        grievance.setVillageSahi(request.getVillageSahi());
        grievance.setAddress(request.getAddress());
        grievance.setWardNo(request.getWardNo());
        grievance.setName(request.getName());
        grievance.setFatherSpouseName(request.getFatherSpouseName());
        grievance.setContact(request.getContact());
        grievance.setTopic1(request.getTopic1());
        grievance.setTopic2(request.getTopic2());
        grievance.setTopic3(request.getTopic3());
        grievance.setTopic4(request.getTopic4());
        grievance.setTopic5(request.getTopic5());
        grievance.setGrievanceDetails(request.getGrievanceDetails());
        grievance.setAgentRemarks(request.getAgentRemarks());
        grievance.setAgentName(request.getAgentName());
        grievance.setWorkGivenTo(request.getWorkGivenTo());

        if (request.getStatus() != null) {
            grievance.setStatus(Grievance.GrievanceStatus.valueOf(request.getStatus().toUpperCase()));
        }

        grievance.setAdminDate(request.getAdminDate());
        grievance.setAdminRemarks(request.getAdminRemarks());
    }

    private void saveAttachments(Grievance grievance, List<AttachmentDTO> attachmentDTOs) {
        for (AttachmentDTO dto : attachmentDTOs) {
            Attachment attachment = new Attachment();
            attachment.setGrievance(grievance);
            attachment.setS3Url(dto.getS3Url());
            attachment.setS3Key(dto.getS3Key());
            attachment.setFileType(dto.getFileType());
            attachment.setFileName(dto.getFileName());
            attachment.setFileSize(dto.getFileSize());

            attachmentRepository.save(attachment);
        }
    }

    public byte[] exportToCsv(List<Long> ids) throws Exception {
        List<Grievance> grievances =
                grievanceRepository.findAllWithAttachmentsByIdIn(ids);
        return generateCsv(grievances);
    }


    /*public byte[] exportToExcel(List<Long> ids) throws Exception {
        List<Grievance> grievances =
                grievanceRepository.findAllWithAttachmentsByIdIn(ids);
        return generateExcel(grievances);
    }*/

    public byte[] exportToPdf(List<Long> ids) throws Exception {
        List<Grievance> grievances =
                grievanceRepository.findAllWithAttachmentsByIdIn(ids);
        return generatePdf(grievances);
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private void addLabelValue(Document document, String label, Object value, Font font) throws Exception {
        Paragraph paragraph = new Paragraph(label + ": " + nullSafe(value), font);
        paragraph.setSpacingAfter(4f);
        document.add(paragraph);
    }

    private void addIfNotNull(Document document, Object value, Font font) throws Exception {
        if (value != null && !value.toString().trim().isEmpty()) {
            Paragraph paragraph = new Paragraph("- " + value.toString(), font);
            paragraph.setSpacingAfter(3f);
            document.add(paragraph);
        }
    }

    private String escapeCsv(Object value) {

        if (value == null) return "";

        String str = value.toString();

        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            str = str.replace("\"", "\"\"");
            return "\"" + str + "\"";
        }

        return str;
    }



    private byte[] generateCsv(List<Grievance> grievances) throws Exception {

        StringBuilder sb = new StringBuilder();

        // ===== Find max attachments count =====
        int maxAttachments = grievances.stream()
                .mapToInt(g -> g.getAttachments() == null ? 0 : g.getAttachments().size())
                .max()
                .orElse(0);

        // ===== Headers =====
        List<String> headers = new ArrayList<>(List.of(
                "Block", "GP", "Village/Sahi", "Address", "Ward No",
                "Name", "Father/Spouse Name", "Contact",
                "Topic1", "Topic2", "Topic3", "Topic4", "Topic5",
                "Grievance Details", "Agent Remarks", "Agent Name",
                "Work Given To", "Admin Date", "Admin Remarks",
                "Created At"
        ));

        for (int i = 1; i <= maxAttachments; i++) {
            headers.add("Attachment " + i);
        }

        sb.append(String.join(",", headers)).append("\n");

        // ===== Data Rows =====
        for (Grievance g : grievances) {

            List<String> row = new ArrayList<>();

            row.add(escapeCsv(g.getBlock()));
            row.add(escapeCsv(g.getGp()));
            row.add(escapeCsv(g.getVillageSahi()));
            row.add(escapeCsv(g.getAddress()));
            row.add(escapeCsv(g.getWardNo()));
            row.add(escapeCsv(g.getName()));
            row.add(escapeCsv(g.getFatherSpouseName()));
            row.add(escapeCsv(g.getContact()));

            row.add(escapeCsv(g.getTopic1()));
            row.add(escapeCsv(g.getTopic2()));
            row.add(escapeCsv(g.getTopic3()));
            row.add(escapeCsv(g.getTopic4()));
            row.add(escapeCsv(g.getTopic5()));

            row.add(escapeCsv(g.getGrievanceDetails()));
            row.add(escapeCsv(g.getAgentRemarks()));
            row.add(escapeCsv(g.getAgentName()));
            row.add(escapeCsv(g.getWorkGivenTo()));
            row.add(escapeCsv(DateFormatter.formatToDDMMYY(g.getAdminDate())));
            row.add(escapeCsv(g.getAdminRemarks()));
            row.add(escapeCsv(DateFormatter.formatToDDMMYY(g.getCreatedAt())));

            if (g.getAttachments() != null) {
                for (Attachment att : g.getAttachments()) {
                    row.add(escapeCsv(att.getS3Url()));
                }
            }

            sb.append(String.join(",", row)).append("\n");
        }

        return ("\uFEFF" + sb.toString()).getBytes(StandardCharsets.UTF_8);

    }




    /*private byte[] generateExcel(List<Grievance> grievances) throws Exception {

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        SXSSFSheet sheet = workbook.createSheet("Grievances");



        CreationHelper creationHelper = workbook.getCreationHelper();

        // ===== Find max attachment count =====
        int maxAttachments = grievances.stream()
                .mapToInt(g -> g.getAttachments() == null ? 0 : g.getAttachments().size())
                .max()
                .orElse(0);

        List<String> headers = new ArrayList<>(List.of(
                "Block", "GP", "Village/Sahi", "Address", "Ward No",
                "Name", "Father/Spouse Name", "Contact",
                "Topic1", "Topic2", "Topic3", "Topic4", "Topic5",
                "Grievance Details", "Agent Remarks", "Agent Name",
                "Work Given To", "Admin Date", "Admin Remarks",
                "Created At"
        ));

        for (int i = 1; i <= maxAttachments; i++) {
            headers.add("Attachment " + i);
        }

        // ===== Header Style =====
        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font poiHeaderFont = workbook.createFont();
        poiHeaderFont.setBold(true);

        headerStyle.setFont(poiHeaderFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        CellStyle linkStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font linkFont = workbook.createFont();
        linkFont.setUnderline(org.apache.poi.ss.usermodel.Font.U_SINGLE);
        linkFont.setColor(IndexedColors.BLUE.getIndex());
        linkStyle.setFont(linkFont);


        int rowIdx = 1;

        for (Grievance g : grievances) {

            Row row = sheet.createRow(rowIdx++);
            int col = 0;

            row.createCell(col++).setCellValue(nullSafe(g.getBlock()));
            row.createCell(col++).setCellValue(nullSafe(g.getGp()));
            row.createCell(col++).setCellValue(nullSafe(g.getVillageSahi()));
            row.createCell(col++).setCellValue(nullSafe(g.getAddress()));
            row.createCell(col++).setCellValue(nullSafe(g.getWardNo()));
            row.createCell(col++).setCellValue(nullSafe(g.getName()));
            row.createCell(col++).setCellValue(nullSafe(g.getFatherSpouseName()));
            row.createCell(col++).setCellValue(nullSafe(g.getContact()));

            row.createCell(col++).setCellValue(nullSafe(g.getTopic1()));
            row.createCell(col++).setCellValue(nullSafe(g.getTopic2()));
            row.createCell(col++).setCellValue(nullSafe(g.getTopic3()));
            row.createCell(col++).setCellValue(nullSafe(g.getTopic4()));
            row.createCell(col++).setCellValue(nullSafe(g.getTopic5()));

            row.createCell(col++).setCellValue(nullSafe(g.getGrievanceDetails()));
            row.createCell(col++).setCellValue(nullSafe(g.getAgentRemarks()));
            row.createCell(col++).setCellValue(nullSafe(g.getAgentName()));
            row.createCell(col++).setCellValue(nullSafe(g.getWorkGivenTo()));
            row.createCell(col++).setCellValue(nullSafe(g.getAdminDate()));
            row.createCell(col++).setCellValue(nullSafe(g.getAdminRemarks()));
            row.createCell(col++).setCellValue(nullSafe(g.getCreatedAt()));

            // Attachments with clickable links
            if (g.getAttachments() != null) {
                for (Attachment att : g.getAttachments()) {

                    Cell cell = row.createCell(col++);
                    String url = nullSafe(att.getS3Url());

                    cell.setCellValue(url);

                    if (!url.isEmpty()) {
                        Hyperlink link = creationHelper.createHyperlink(HyperlinkType.URL);
                        link.setAddress(url);
                        cell.setHyperlink(link);
                        cell.setCellStyle(linkStyle);
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.size(); i++) {
            sheet.trackAllColumnsForAutoSizing();
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.dispose();

        return out.toByteArray();
    }*/

    private byte[] generatePdf(List<Grievance> grievances) throws Exception {

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font bodyFont = new Font(Font.HELVETICA, 10);

        for (int i = 0; i < grievances.size(); i++) {

            Grievance g = grievances.get(i);

            // ===== Title =====
            Paragraph title = new Paragraph("Grievance Details", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15f);
            document.add(title);

            // ===== Basic Info Section =====
            addLabelValue(document, "Block", g.getBlock(), bodyFont);
            addLabelValue(document, "GP", g.getGp(), bodyFont);
            addLabelValue(document, "Village/Sahi", g.getVillageSahi(), bodyFont);
            addLabelValue(document, "Address", g.getAddress(), bodyFont);
            addLabelValue(document, "Ward No", g.getWardNo(), bodyFont);
            addLabelValue(document, "Name", g.getName(), bodyFont);
            addLabelValue(document, "Father/Spouse Name", g.getFatherSpouseName(), bodyFont);
            addLabelValue(document, "Contact", g.getContact(), bodyFont);

            document.add(new Paragraph(" "));

            // ===== Topics Section =====
            Paragraph topicHeader = new Paragraph("Topics:", headerFont);
            topicHeader.setSpacingBefore(5f);
            topicHeader.setSpacingAfter(5f);
            document.add(topicHeader);

            addIfNotNull(document, g.getTopic1(), bodyFont);
            addIfNotNull(document, g.getTopic2(), bodyFont);
            addIfNotNull(document, g.getTopic3(), bodyFont);
            addIfNotNull(document, g.getTopic4(), bodyFont);
            addIfNotNull(document, g.getTopic5(), bodyFont);

            document.add(new Paragraph(" "));

            // ===== Grievance Details =====
            Paragraph grievanceHeader = new Paragraph("Grievance Description:", headerFont);
            grievanceHeader.setSpacingBefore(5f);
            grievanceHeader.setSpacingAfter(5f);
            document.add(grievanceHeader);

            document.add(new Paragraph(nullSafe(g.getGrievanceDetails()), bodyFont));

            document.add(new Paragraph(" "));

            // ===== Admin Section =====
            Paragraph adminHeader = new Paragraph("Admin Details:", headerFont);
            adminHeader.setSpacingBefore(10f);
            adminHeader.setSpacingAfter(5f);
            document.add(adminHeader);

            addLabelValue(document, "Agent Name", g.getAgentName(), bodyFont);
            addLabelValue(document, "Agent Remarks", g.getAgentRemarks(), bodyFont);
            addLabelValue(document, "Work Given To", g.getWorkGivenTo(), bodyFont);
            addLabelValue(document, "Admin Date", g.getAdminDate(), bodyFont);
            addLabelValue(document, "Admin Remarks", g.getAdminRemarks(), bodyFont);
            addLabelValue(document, "Created At", g.getCreatedAt(), bodyFont);

            document.add(new Paragraph(" "));

            // ===== Attachments =====
            Paragraph attachmentHeader = new Paragraph("Attachments:", headerFont);
            attachmentHeader.setSpacingBefore(10f);
            attachmentHeader.setSpacingAfter(5f);
            document.add(attachmentHeader);

            if (g.getAttachments() != null && !g.getAttachments().isEmpty()) {

                for (Attachment att : g.getAttachments()) {

                    String url = nullSafe(att.getS3Url());

                    if (!url.isEmpty()) {

                        Chunk link = new Chunk("Open Attachment", bodyFont);
                        link.setAnchor(url);

                        Paragraph linkParagraph = new Paragraph(link);
                        linkParagraph.setSpacingAfter(5f);
                        document.add(linkParagraph);
                    }
                }
            } else {
                document.add(new Paragraph("No attachments available.", bodyFont));
            }

            // Add new page except for last record
            if (i < grievances.size() - 1) {
                document.newPage();
            }
        }

        document.close();
        return out.toByteArray();
    }




}