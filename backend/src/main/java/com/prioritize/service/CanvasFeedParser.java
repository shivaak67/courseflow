package com.prioritize.service;

import biweekly.io.text.ICalReader;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import com.prioritize.exception.ApiException;

@Component
public class CanvasFeedParser {
    public record Item(String key, String title, String description, Instant start, Instant end,
            LocalDate startDate, LocalDate endDate, String kind, String url) {}

    public List<Item> parse(String text, URI feed, ZoneId zone) {
        try {
            String cleaned = text.strip().replace("\uFEFF", "");
            if (cleaned.length() > CanvasFeedClient.MAX_BYTES || !cleaned.startsWith("BEGIN:VCALENDAR")
                    || !cleaned.endsWith("END:VCALENDAR")) throw new IllegalArgumentException();
            try (ICalReader reader = new ICalReader(cleaned)) {
                reader.setDefaultTimezone(TimeZone.getTimeZone(zone));
                reader.setGlobalTimezoneIdResolver(id -> TimeZone.getTimeZone(ZoneId.of(id.startsWith("/") ? id.substring(1) : id)));
                ICalendar calendar = reader.readNext();
                // Warning 37 means a TZID was resolved using the strict IANA resolver above
                // instead of an embedded VTIMEZONE. Other parse warnings can mean dropped data.
                if (calendar == null || reader.getWarnings().stream().anyMatch(w -> !Integer.valueOf(37).equals(w.getCode())) || reader.readNext() != null
                        || calendar.getEvents().size() > 5000) throw new IllegalArgumentException();
                Map<String,Item> items = new LinkedHashMap<>();
                for (VEvent event : calendar.getEvents()) {
                    if (event.getUid()==null || event.getUid().getValue().isBlank()) throw new IllegalArgumentException();
                    // Canvas exports each repeated calendar occurrence as its own VEVENT.
                    // Reject foreign recurrence rules rather than silently omitting occurrences.
                    if (event.getRecurrenceRule()!=null || !event.getRecurrenceDates().isEmpty()) throw new IllegalArgumentException();
                    if (event.getStatus()!=null && "CANCELLED".equalsIgnoreCase(event.getStatus().getValue())) continue;
                    if (event.getDateStart()==null || event.getDateStart().getValue()==null
                            || event.getSummary()==null || event.getSummary().getValue().isBlank()) throw new IllegalArgumentException();
                    String uid=event.getUid().getValue();
                    boolean allDay=!event.getDateStart().getValue().hasTime();
                    Instant start=event.getDateStart().getValue().toInstant();
                    LocalDate startDate=allDay ? dateOnly(event.getDateStart().getValue()) : null;
                    Instant end=event.getDateEnd()==null ? start : event.getDateEnd().getValue().toInstant();
                    if (event.getDuration()!=null && event.getDateEnd()==null)
                        end=event.getDuration().getValue().add(java.util.Date.from(start)).toInstant();
                    LocalDate endDate=allDay ? (event.getDateEnd()==null ? startDate.plusDays(1) : dateOnly(event.getDateEnd().getValue())) : null;
                    if (allDay && !endDate.isAfter(startDate)) throw new IllegalArgumentException();
                    if (allDay) { start=startDate.atStartOfDay(zone).toInstant(); end=endDate.atStartOfDay(zone).toInstant(); }
                    if (end.isBefore(start)) throw new IllegalArgumentException();
                    if (end.equals(start)) end=start.plusMillis(1); // Canvas assignment deadlines are instants, not work blocks.
                    if (Duration.between(start,end).toDays()>400) throw new IllegalArgumentException();
                    String key=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(uid.getBytes(StandardCharsets.UTF_8)));
                    String url=safeLink(event.getUrl()==null ? null : event.getUrl().getValue(),feed);
                    String kind=uid.contains("assignment") || (url!=null && url.contains("#assignment_")) ? "DEADLINE" : "EVENT";
                    String title=event.getSummary().getValue();
                    String description=event.getDescription()==null ? null : event.getDescription().getValue();
                    Item item=new Item(key,limit(title,255),limit(description,20000),start,end,startDate,endDate,kind,url);
                    if (items.putIfAbsent(key,item)!=null) throw new IllegalArgumentException();
                }
                return List.copyOf(items.values());
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This feed could not be read completely. Use a Canvas Calendar Feed link. No imported items were changed.");
        }
    }
    static String safeLink(String value, URI feed) {
        if (value==null || value.length()>2048) return null;
        try {
            URI uri=URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && feed.getHost().equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo()==null && (uri.getPort()==-1 || uri.getPort()==443)
                    && !uri.getPath().startsWith("/feeds/") ? value : null;
        } catch (Exception e) { return null; }
    }
    private LocalDate dateOnly(biweekly.util.ICalDate value) {
        // DATE values have no timezone. The library's Date wrapper uses the JVM zone;
        // use original calendar components instead of converting that wrapper's instant.
        var raw=value.getRawComponents();
        return LocalDate.of(raw.getYear(),raw.getMonth(),raw.getDate());
    }
    private String limit(String value,int length) { return value==null ? null : value.substring(0,Math.min(value.length(),length)); }
}
