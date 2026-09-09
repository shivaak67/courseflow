package com.prioritize.service;
import com.prioritize.model.*;
import com.prioritize.repository.*;
import com.prioritize.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class CanvasFeedIntegrationTest {
    @Autowired CanvasFeedService service;
    @Autowired CanvasCalendarFeedRepository feeds;
    @Autowired CalendarEventRepository events;
    @Autowired UserRepository users;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @MockitoBean CanvasFeedClient client;
    UUID owner,other; String token,otherToken;
    final String url="https://school.instructure.com/feeds/calendars/user_fixture.ics";
    @BeforeEach void setup() {
        owner=addUser();other=addUser(); token=jwt.generateToken(owner,owner+"@example.com");otherToken=jwt.generateToken(other,other+"@example.com");
        when(client.validate(anyString())).thenReturn(URI.create(url));
        when(client.fetch(any())).thenReturn(feed("Essay","20260915T235900Z"));
    }
    UUID addUser() {
        User u=new User();u.setId(UUID.randomUUID());u.setEmail(u.getId()+"@example.com");u.setFirstName("Canvas");u.setLastName("Test");u.setAuthProvider(AuthProvider.LOCAL);u.setPasswordHash("unused");u.setRole(Role.USER);return users.save(u).getId();
    }
    String feed(String title,String date) { return CanvasFeedParserTest.calendar(CanvasFeedParserTest.event("event-assignment-1",title,"DTSTART:"+date+"\r\nDTEND:"+date)); }
    void ready() { var f=feeds.findById(owner).orElseThrow();f.setLastAttemptAt(Instant.now().minusSeconds(1900));feeds.save(f); }
    @Test void refreshUpsertsSameItemAndKeepsAccountsAndManualEventsSeparate() {
        CalendarEvent manual=new CalendarEvent();manual.setUserId(owner);manual.setTitle("Personal plan");manual.setStartAt(Instant.now());manual.setEndAt(Instant.now().plusSeconds(60));events.save(manual);
        assertEquals(1,service.connect(owner,url,"America/Chicago").itemCount());
        UUID id=events.findByUserIdAndCanvasKeyIsNotNull(owner).getFirst().getId();
        assertFalse(service.status(other).connected()); assertFalse(feeds.findById(owner).orElseThrow().getEncryptedUrl().contains("user_fixture"));
        ready();when(client.fetch(any())).thenReturn(feed("Updated essay","20260916T235900Z"));service.sync(owner,false);
        var imported=events.findByUserIdAndCanvasKeyIsNotNull(owner);assertEquals(1,imported.size());assertEquals(id,imported.getFirst().getId());assertEquals("Updated essay",imported.getFirst().getTitle());
        assertTrue(events.findById(manual.getId()).isPresent());
        ready();when(client.fetch(any())).thenReturn(CanvasFeedParserTest.calendar(""));service.sync(owner,false);
        assertTrue(events.findByUserIdAndCanvasKeyIsNotNull(owner).isEmpty());assertTrue(events.findById(manual.getId()).isPresent());
    }
    @Test void failureKeepsLastSnapshotAndSuccessfulSyncClearsError() {
        service.connect(owner,url,"UTC");ready();when(client.fetch(any())).thenReturn("<html>Login</html>");
        assertNotNull(service.sync(owner,false).error());assertEquals(1,events.findByUserIdAndCanvasKeyIsNotNull(owner).size());
        ready();when(client.fetch(any())).thenReturn(feed("Essay","20260915T235900Z"));assertNull(service.sync(owner,true).error());
        assertThrows(RuntimeException.class,()->service.sync(owner,false));
    }
    @Test void endpointsHidePrivateUrlAndPreventEditingImportedRecords() throws Exception {
        mvc.perform(get("/api/integrations/canvas")).andExpect(status().isUnauthorized());
        service.connect(owner,url,"UTC");UUID id=events.findByUserIdAndCanvasKeyIsNotNull(owner).getFirst().getId();
        mvc.perform(get("/api/integrations/canvas").header("Authorization","Bearer "+token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.feedUrl").doesNotExist()).andExpect(jsonPath("$.encryptedUrl").doesNotExist()).andExpect(jsonPath("$.host").value("school.instructure.com"));
        mvc.perform(delete("/api/calendar-events/"+id).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
        mvc.perform(get("/api/calendar-events/"+id).header("Authorization","Bearer "+otherToken)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/integrations/canvas").header("Authorization","Bearer "+otherToken)).andExpect(status().isNoContent());
        assertEquals(1,events.findByUserIdAndCanvasKeyIsNotNull(owner).size());
        service.disconnect(owner);assertFalse(service.status(owner).connected());assertTrue(events.findByUserIdAndCanvasKeyIsNotNull(owner).isEmpty());
    }
}
