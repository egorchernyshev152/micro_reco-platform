package com.example.recommender.controller;

import com.example.recommender.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/top-items")
    @Operation(summary = "Generate top items report (DOCX)")
    public ResponseEntity<byte[]> topItems(@RequestParam(value = "period", required = false) String period) {
        // генерируем docx отчет с популярными товарами за период
        byte[] data = reportService.generateTopItemsDocx(period);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("top-items-report.docx").build());
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
