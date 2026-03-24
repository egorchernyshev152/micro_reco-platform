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

    @GetMapping("/top-movies")
    @Operation(summary = "Generate top movies report (DOCX)")
    public ResponseEntity<byte[]> topMovies(@RequestParam(value = "period", required = false) String period) {
        byte[] data = reportService.generateTopMoviesDocx(period);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("top-movies-report.docx").build());
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
