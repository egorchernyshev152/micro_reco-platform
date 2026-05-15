package com.example.recommender.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RebuildProgressLogger {

    @EventListener
    public void onProgress(RebuildProgressEvent event) {
        log.info("Rebuild job {} -> {} / {} (status={})",
                event.logId(),
                event.processed(),
                event.total(),
                event.status());
    }
}
