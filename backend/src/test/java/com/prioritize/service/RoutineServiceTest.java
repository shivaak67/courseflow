package com.prioritize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.prioritize.dto.OccurrenceResponse;
import com.prioritize.dto.RoutineRequest;
import com.prioritize.dto.RoutineResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.RoutineMapper;
import com.prioritize.model.Category;
import com.prioritize.model.RecurrenceType;
import com.prioritize.model.Routine;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.RoutineRepository;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    private static final UUID USER_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ROUTINE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID CATEGORY_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private RoutineService routineService;

    @BeforeEach
    void setUp() {
        routineService = new RoutineService(routineRepository, categoryRepository, new RoutineMapper());
    }

    @Test
    void createPersistsOwnedRoutine() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A))
                .thenReturn(Optional.of(new Category()));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> {
            Routine routine = invocation.getArgument(0);
            if (routine.getId() == null) {
                routine.setId(ROUTINE_ID);
            }
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            if (routine.getCreatedAt() == null) {
                routine.setCreatedAt(now);
                routine.setUpdatedAt(now);
            }
            return routine;
        });

        RoutineResponse response = routineService.create(
                USER_A,
                new RoutineRequest(
                        "Morning run",
                        CATEGORY_ID,
                        RecurrenceType.DAILY,
                        null,
                        1,
                        LocalTime.of(7, 0),
                        LocalTime.of(8, 0),
                        LocalDate.of(2026, 3, 1),
                        null,
                        null));

        ArgumentCaptor<Routine> captor = ArgumentCaptor.forClass(Routine.class);
        verify(routineRepository).save(captor.capture());
        Routine saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_A);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getDaysOfWeek()).isNull();
        assertThat(response.id()).isEqualTo(ROUTINE_ID);
        assertThat(response.title()).isEqualTo("Morning run");
    }

    @Test
    void createReturns404WhenCategoryNotOwned() {
        when(categoryRepository.findByIdAndUserId(CATEGORY_ID, USER_A)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.create(
                        USER_A,
                        new RoutineRequest(
                                "Gym",
                                CATEGORY_ID,
                                RecurrenceType.WEEKLY,
                                "1,3,5",
                                1,
                                LocalTime.of(18, 0),
                                null,
                                LocalDate.of(2026, 3, 1),
                                null,
                                true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");

        verify(routineRepository, never()).save(any());
    }

    @Test
    void getReturns404WhenOwnedByAnotherUser() {
        when(routineRepository.findByIdAndUserId(ROUTINE_ID, USER_B)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routineService.get(USER_B, ROUTINE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Routine not found");
    }

    @Test
    void createRejectsWeeklyWithoutDaysOfWeek() {
        assertThatThrownBy(() -> routineService.create(
                        USER_A,
                        new RoutineRequest(
                                "Study",
                                null,
                                RecurrenceType.WEEKLY,
                                "  ",
                                1,
                                LocalTime.of(9, 0),
                                null,
                                LocalDate.of(2026, 3, 1),
                                null,
                                true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("daysOfWeek");

        verify(routineRepository, never()).save(any());
    }

    @Test
    void createRejectsEndTimeNotAfterStartTime() {
        assertThatThrownBy(() -> routineService.create(
                        USER_A,
                        new RoutineRequest(
                                "Nap",
                                null,
                                RecurrenceType.DAILY,
                                null,
                                1,
                                LocalTime.of(14, 0),
                                LocalTime.of(14, 0),
                                LocalDate.of(2026, 3, 1),
                                null,
                                true)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("endTime must be after startTime");
    }

    @Test
    void expandDailyRespectsInterval() {
        Routine routine = baseRoutine(RecurrenceType.DAILY, null, 2);
        routine.setStartDate(LocalDate.of(2026, 3, 1));

        List<LocalDate> dates = routineService.expandDates(
                routine, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 3),
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 7),
                LocalDate.of(2026, 3, 9));
    }

    @Test
    void expandWeeklyRespectsDaysAndInterval() {
        // Start Sunday 2026-03-01; Mon=1,Wed=3; interval 2 weeks
        Routine routine = baseRoutine(RecurrenceType.WEEKLY, "1,3", 2);
        routine.setStartDate(LocalDate.of(2026, 3, 1));

        List<LocalDate> dates = routineService.expandDates(
                routine, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 4),
                LocalDate.of(2026, 3, 16),
                LocalDate.of(2026, 3, 18),
                LocalDate.of(2026, 3, 30));
    }

    @Test
    void expandSelectedWeekdaysIgnoresInterval() {
        Routine routine = baseRoutine(RecurrenceType.SELECTED_WEEKDAYS, "5", 3);
        routine.setStartDate(LocalDate.of(2026, 3, 1));

        List<LocalDate> dates = routineService.expandDates(
                routine, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 21));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 3, 6),
                LocalDate.of(2026, 3, 13),
                LocalDate.of(2026, 3, 20));
    }

    @Test
    void expandMonthlySkipsInvalidDayOfMonth() {
        Routine routine = baseRoutine(RecurrenceType.MONTHLY, null, 1);
        routine.setStartDate(LocalDate.of(2026, 1, 31));

        List<LocalDate> dates = routineService.expandDates(
                routine, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 31));
    }

    @Test
    void occurrencesReturnsOnlyActiveRoutinesInWindow() {
        Routine active = baseRoutine(RecurrenceType.DAILY, null, 1);
        active.setId(ROUTINE_ID);
        active.setTitle("Active habit");
        active.setStartDate(LocalDate.of(2026, 3, 1));
        active.setActive(true);

        when(routineRepository.findByUserIdAndActiveOrderByStartDateAsc(USER_A, true))
                .thenReturn(List.of(active));

        List<OccurrenceResponse> occurrences = routineService.occurrences(
                USER_A, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3));

        assertThat(occurrences).hasSize(3);
        assertThat(occurrences.getFirst().routineId()).isEqualTo(ROUTINE_ID);
        assertThat(occurrences.getFirst().title()).isEqualTo("Active habit");
        assertThat(occurrences.getFirst().date()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void occurrencesRejectsWindowLargerThan90Days() {
        assertThatThrownBy(() -> routineService.occurrences(
                        USER_A, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 2)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessage("occurrence window cannot exceed 90 days");
    }

    private Routine baseRoutine(RecurrenceType type, String daysOfWeek, int interval) {
        Routine routine = new Routine();
        routine.setUserId(USER_A);
        routine.setTitle("Habit");
        routine.setRecurrenceType(type);
        routine.setDaysOfWeek(daysOfWeek);
        routine.setIntervalValue(interval);
        routine.setStartTime(LocalTime.of(7, 0));
        routine.setEndTime(LocalTime.of(8, 0));
        routine.setActive(true);
        return routine;
    }
}
