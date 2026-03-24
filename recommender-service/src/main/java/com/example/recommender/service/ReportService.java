package com.example.recommender.service;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.client.EventClient;
import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.MovieStatDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventClient eventClient;
    private final CatalogClient catalogClient;

    public byte[] generateTopMoviesDocx(String period) {
        List<MovieStatDto> stats = eventClient.getStatsByMovie(period);
        List<Long> movieIds = stats.stream().map(MovieStatDto::getMovieId).toList();
        Map<Long, Long> counts = stats.stream()
                .collect(Collectors.toMap(MovieStatDto::getMovieId, MovieStatDto::getCount));
        List<MovieDto> movies = movieIds.isEmpty() ? List.of() : catalogClient.getMoviesByIds(movieIds);
        Map<Long, MovieDto> byId = movies.stream().collect(Collectors.toMap(MovieDto::getId, m -> m));

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = title.createRun();
            run.setText("Top Movies Report (" + (period == null ? "ALL" : period.toUpperCase()) + ")");
            run.setBold(true);
            run.setFontSize(16);

            XWPFParagraph spacer = doc.createParagraph();
            spacer.createRun().addBreak();

            XWPFTable table = doc.createTable(Math.max(1, stats.size()) + 1, 4);
            table.getRow(0).getCell(0).setText("Rank");
            table.getRow(0).getCell(1).setText("Movie ID");
            table.getRow(0).getCell(2).setText("Title");
            table.getRow(0).getCell(3).setText("Events");

            int rowIdx = 1;
            int rank = 1;
            for (MovieStatDto stat : stats) {
                XWPFTableRow row = table.getRow(rowIdx++);
                MovieDto movie = byId.get(stat.getMovieId());
                row.getCell(0).setText(String.valueOf(rank++));
                row.getCell(1).setText(String.valueOf(stat.getMovieId()));
                row.getCell(2).setText(movie != null ? movie.getTitle() : "(unknown)");
                row.getCell(3).setText(String.valueOf(counts.getOrDefault(stat.getMovieId(), 0L)));
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}
