package com.prioritize.service;
import com.prioritize.repository.CanvasCalendarFeedRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class CanvasFeedScheduler {
    private final CanvasCalendarFeedRepository feeds;
    private final CanvasFeedService service;
    public CanvasFeedScheduler(CanvasCalendarFeedRepository feeds,CanvasFeedService service) { this.feeds=feeds; this.service=service; }
    @Scheduled(fixedDelayString="${app.canvas.poll-ms:60000}",initialDelay=60000,scheduler="canvasFeedTaskScheduler")
    public void refreshDueFeeds() {
        for (var feed:feeds.findAll()) {
            if (feed.getLastAttemptAt()!=null && feed.getLastAttemptAt().isAfter(java.time.Instant.now().minusSeconds(1800))) continue;
            try { service.sync(feed.getUserId(),true); }
            catch (RuntimeException ignored) { /* One account's failure must not prevent other accounts from refreshing. */ }
        }
    }
}
