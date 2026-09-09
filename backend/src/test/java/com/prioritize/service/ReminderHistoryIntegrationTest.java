package com.prioritize.service;

import com.prioritize.model.*;
import com.prioritize.repository.*;
import com.prioritize.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ReminderHistoryIntegrationTest {
    @Autowired ReminderRepository reminders;
    @Autowired UserRepository users;
    @Autowired ReminderService service;
    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    UUID user() {
        User u = new User(); u.setId(UUID.randomUUID()); u.setEmail(u.getId()+"@example.com");
        u.setFirstName("History"); u.setLastName("Test"); u.setAuthProvider(AuthProvider.LOCAL);
        u.setPasswordHash("unused"); u.setRole(Role.USER); return users.save(u).getId();
    }
    void add(UUID owner, ReminderStatus status) {
        Reminder r = new Reminder(); r.setUserId(owner); r.setStatus(status); r.setChannel(NotificationChannel.EMAIL);
        r.setRelatedEntityType(ReminderEntityType.TASK); r.setRelatedEntityId(UUID.randomUUID());
        r.setReminderAt(Instant.now().plusSeconds(3600)); reminders.save(r);
    }
    @Test void clearIsAuthenticatedScopedPersistentAndPreservesActiveAndDeliveryRecords() throws Exception {
        UUID owner = user(), other = user();
        for (ReminderStatus status : ReminderStatus.values()) { add(owner, status); add(other, status); }
        mvc.perform(delete("/api/reminders/history")).andExpect(status().isUnauthorized());
        String token = jwt.generateToken(owner, owner+"@example.com");
        mvc.perform(delete("/api/reminders/history").header("Authorization", "Bearer "+token)).andExpect(status().isNoContent());
        assertEquals(2, service.list(owner, null).size());
        assertTrue(service.list(owner, null).stream().allMatch(r -> r.status() == ReminderStatus.PENDING || r.status() == ReminderStatus.PROCESSING));
        assertEquals(5, service.list(other, null).size());
        assertEquals(5, reminders.findByUserIdOrderByReminderAtDesc(owner).size());
        assertTrue(service.list(owner, ReminderStatus.SENT).isEmpty());
        service.clearHistory(owner); assertEquals(2, service.list(owner, null).size());
        add(owner, ReminderStatus.SENT); assertEquals(3, service.list(owner, null).size());
    }
}
