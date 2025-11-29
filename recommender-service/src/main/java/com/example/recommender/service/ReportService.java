package com.example.recommender.service;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.client.EventClient;
import com.example.recommender.dto.ItemDto;
import com.example.recommender.dto.ItemStatDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventClient eventClient;
    private final CatalogClient catalogClient;

    public byte[] generateTopItemsDocx(String period) {
        List<ItemStatDto> stats = eventClient.getStatsByItem(period);
        List<Long> itemIds = stats.stream().map(ItemStatDto::getItemId).toList();
        Map<Long, Long> counts = stats.stream().collect(Collectors.toMap(ItemStatDto::getItemId, ItemStatDto::getCount));
        List<ItemDto> items = itemIds.isEmpty() ? List.of() : catalogClient.getItemsByIds(itemIds);
        Map<Long, ItemDto> byId = items.stream().collect(Collectors.toMap(ItemDto::getId, i -> i));

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = title.createRun();
            run.setText("Top Items Report (" + (period == null ? "ALL" : period.toUpperCase()) + ")");
            run.setBold(true);
            run.setFontSize(16);

            XWPFParagraph spacer = doc.createParagraph();
            spacer.createRun().addBreak();

            XWPFTable table = doc.createTable(Math.max(1, stats.size()) + 1, 4);
            table.getRow(0).getCell(0).setText("Rank");
            table.getRow(0).getCell(1).setText("Item ID");
            table.getRow(0).getCell(2).setText("Title");
            table.getRow(0).getCell(3).setText("Events");

            int rowIdx = 1;
            int rank = 1;
            for (ItemStatDto st : stats) {
                XWPFTableRow row = table.getRow(rowIdx++);
                ItemDto item = byId.get(st.getItemId());
                row.getCell(0).setText(String.valueOf(rank++));
                row.getCell(1).setText(String.valueOf(st.getItemId()));
                row.getCell(2).setText(item != null ? item.getTitle() : "(unknown)");
                row.getCell(3).setText(String.valueOf(counts.getOrDefault(st.getItemId(), 0L)));
            }

            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate report", e);
        }
    }
}

