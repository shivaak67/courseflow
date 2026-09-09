package com.prioritize.service;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
class CanvasFeedParserTest {
    private final CanvasFeedParser parser=new CanvasFeedParser();
    private final URI feed=URI.create("https://school.instructure.com/feeds/calendars/user_fixture.ics");
    static String calendar(String event) { return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Canvas//EN\r\n"+event+"END:VCALENDAR\r\n"; }
    static String event(String uid,String title,String dates) {
        return "BEGIN:VEVENT\r\nUID:"+uid+"\r\nSUMMARY:"+title+"\r\n"+dates+"\r\nEND:VEVENT\r\n";
    }
    @Test void readsCanvasUtcDeadlinesUnfoldingAndEscapedText() {
        var items=parser.parse(calendar(event("event-assignment-1","Essay\\, draft\r\n  one [ENG]",
            "DTSTART;TZID=UTC:20260915T235900\r\nDTEND;TZID=UTC:20260915T235900\r\nDESCRIPTION:Read\\nWrite\r\nURL:https://school.instructure.com/calendar#assignment_1")),feed,ZoneId.of("America/Chicago"));
        var item=items.getFirst();
        assertEquals("Essay, draft one [ENG]",item.title());
        assertEquals("Read\nWrite",item.description().replace("\r\n", "\n"));
        assertEquals(Instant.parse("2026-09-15T23:59:00Z"),item.start());
        assertEquals(item.start().plusMillis(1),item.end()); assertEquals("DEADLINE",item.kind());
        assertNotNull(item.url());
    }
    @Test void keepsDateOnlyEventsAcrossDaylightSavingAndFloatingTimes() {
        var items=parser.parse(calendar(event("event-calendar-event-1","All-day",
            "DTSTART;VALUE=DATE:20261101\r\nDTEND;VALUE=DATE:20261103")+
            event("event-calendar-event-2","Office hours","DTSTART:20260915T090000\r\nDTEND:20260915T100000")),feed,ZoneId.of("America/Chicago"));
        assertEquals(LocalDate.of(2026,11,1),items.get(0).startDate());
        assertEquals(LocalDate.of(2026,11,3),items.get(0).endDate());
        assertEquals(49,Duration.between(items.get(0).start(),items.get(0).end()).toHours());
        assertEquals(Instant.parse("2026-09-15T14:00:00Z"),items.get(1).start());
    }
    @Test void cancelledEventsDisappearAndUnsafeLinksAreNotExposed() {
        var items=parser.parse(calendar(event("event-assignment-1","Cancelled","STATUS:CANCELLED")+
            event("event-calendar-event-2","Office hours","DTSTART:20260915T090000Z\r\nURL:javascript:alert(1)")),feed,ZoneOffset.UTC);
        assertEquals(1,items.size()); assertNull(items.getFirst().url());
    }
    @Test void rejectsPartialHtmlDuplicateAndUnsupportedRecurrenceFeeds() {
        String e=event("event-1","Office hours","DTSTART:20260915T090000Z");
        for(String text: new String[]{"<html>Log in</html>",calendar(e).replace("END:VCALENDAR",""),calendar(e+e),
                calendar(event("event-1","Office hours","DTSTART:bad")),
                calendar(event("event-1","Office hours","DTSTART;TZID=Unknown/Nowhere:20260915T090000")),
                calendar(event("event-1","Office hours","DTSTART:20260915T090000Z\r\nRRULE:FREQ=DAILY"))})
            assertThrows(RuntimeException.class,()->parser.parse(text,feed,ZoneOffset.UTC));
        assertTrue(parser.parse(calendar(""),feed,ZoneOffset.UTC).isEmpty());
    }
}
