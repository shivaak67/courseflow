package com.prioritize.model;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
@Entity @Table(name="canvas_calendar_feeds")
public class CanvasCalendarFeed {
    @Id @Column(name="user_id") private UUID userId;
    public UUID getUserId() { return userId; }
    public void setUserId(UUID value) { userId = value; }
    @Column(name="encrypted_url")
    private String encryptedUrl;
    @Column(name="host")
    private String host;
    @Column(name="timezone")
    private String timezone;
    @Column(name="last_attempt_at")
    private Instant lastAttemptAt;
    @Column(name="last_synced_at")
    private Instant lastSyncedAt;
    @Column(name="last_error")
    private String lastError;
    @Column(name="item_count")
    private int itemCount;
    public String getEncryptedUrl() { return encryptedUrl; }
    public void setEncryptedUrl(String value) { encryptedUrl = value; }
    public String getHost() { return host; }
    public void setHost(String value) { host = value; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String value) { timezone = value; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant value) { lastAttemptAt = value; }
    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant value) { lastSyncedAt = value; }
    public String getLastError() { return lastError; }
    public void setLastError(String value) { lastError = value; }
    public int getItemCount() { return itemCount; }
    public void setItemCount(int value) { itemCount = value; }
}
