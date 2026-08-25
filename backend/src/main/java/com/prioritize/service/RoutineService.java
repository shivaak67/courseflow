package com.prioritize.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prioritize.dto.OccurrenceResponse;
import com.prioritize.dto.RoutineRequest;
import com.prioritize.dto.RoutineResponse;
import com.prioritize.exception.ApiException;
import com.prioritize.exception.ResourceNotFoundException;
import com.prioritize.mapper.RoutineMapper;
import com.prioritize.model.RecurrenceType;
import com.prioritize.model.Routine;
import com.prioritize.repository.CategoryRepository;
import com.prioritize.repository.RoutineRepository;

@Service
@Transactional
public class RoutineService {

    private static final int MAX_OCCURRENCE_WINDOW_DAYS = 90;

    private final RoutineRepository routineRepository;
    private final CategoryRepository categoryRepository;
    private final RoutineMapper routineMapper;

    public RoutineService(
            RoutineRepository routineRepository,
            CategoryRepository categoryRepository,
            RoutineMapper routineMapper) {
        this.routineRepository = routineRepository;
        this.categoryRepository = categoryRepository;
        this.routineMapper = routineMapper;
    }

    @Transactional(readOnly = true)
    public List<RoutineResponse> list(UUID userId, Boolean active) {
        List<Routine> routines = active == null
                ? routineRepository.findByUserIdOrderByStartDateAsc(userId)
                : routineRepository.findByUserIdAndActiveOrderByStartDateAsc(userId, active);
        return routines.stream().map(routineMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoutineResponse get(UUID userId, UUID routineId) {
        return routineMapper.toResponse(requireOwned(userId, routineId));
    }

    public RoutineResponse create(UUID userId, RoutineRequest request) {
        validateRequest(request);
        requireOwnedCategoryIfPresent(userId, request.categoryId());
        Routine routine = new Routine();
        routine.setUserId(userId);
        routineMapper.applyCreate(routine, request);
        return routineMapper.toResponse(routineRepository.save(routine));
    }

    public RoutineResponse update(UUID userId, UUID routineId, RoutineRequest request) {
        Routine routine = requireOwned(userId, routineId);
        validateRequest(request);
        requireOwnedCategoryIfPresent(userId, request.categoryId());
        routineMapper.applyUpdate(routine, request);
        return routineMapper.toResponse(routineRepository.save(routine));
    }

    public void delete(UUID userId, UUID routineId) {
        Routine routine = requireOwned(userId, routineId);
        routineRepository.delete(routine);
    }

    @Transactional(readOnly = true)
    public List<OccurrenceResponse> occurrences(UUID userId, LocalDate from, LocalDate to) {
        validateOccurrenceWindow(from, to);
        List<Routine> routines = routineRepository.findByUserIdAndActiveOrderByStartDateAsc(userId, true);
        List<OccurrenceResponse> result = new ArrayList<>();
        for (Routine routine : routines) {
            for (LocalDate date : expandDates(routine, from, to)) {
                result.add(routineMapper.toOccurrence(routine, date));
            }
        }
        result.sort(Comparator
                .comparing(OccurrenceResponse::date)
                .thenComparing(OccurrenceResponse::startTime)
                .thenComparing(OccurrenceResponse::routineId));
        return result;
    }

    @Transactional(readOnly = true)
    public Routine requireOwned(UUID userId, UUID routineId) {
        return routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Routine not found"));
    }

    List<LocalDate> expandDates(Routine routine, LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = maxDate(from, routine.getStartDate());
        LocalDate effectiveTo = routine.getEndDate() == null ? to : minDate(to, routine.getEndDate());
        if (effectiveFrom.isAfter(effectiveTo)) {
            return List.of();
        }

        return switch (routine.getRecurrenceType()) {
            case DAILY -> expandDaily(routine, effectiveFrom, effectiveTo);
            case WEEKLY -> expandWeekly(routine, effectiveFrom, effectiveTo, true);
            case SELECTED_WEEKDAYS -> expandWeekly(routine, effectiveFrom, effectiveTo, false);
            case MONTHLY -> expandMonthly(routine, effectiveFrom, effectiveTo);
        };
    }

    private List<LocalDate> expandDaily(Routine routine, LocalDate from, LocalDate to) {
        int interval = Math.max(1, routine.getIntervalValue());
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = routine.getStartDate();
        long offset = ChronoUnit.DAYS.between(start, from);
        long remainder = Math.floorMod(offset, interval);
        LocalDate cursor = remainder == 0 ? from : from.plusDays(interval - remainder);
        while (!cursor.isAfter(to)) {
            dates.add(cursor);
            cursor = cursor.plusDays(interval);
        }
        return dates;
    }

    private List<LocalDate> expandWeekly(
            Routine routine, LocalDate from, LocalDate to, boolean respectInterval) {
        Set<DayOfWeek> days = parseDaysOfWeek(routine.getDaysOfWeek());
        if (days.isEmpty()) {
            return List.of();
        }
        int interval = respectInterval ? Math.max(1, routine.getIntervalValue()) : 1;
        LocalDate start = routine.getStartDate();
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate cursor = from; !cursor.isAfter(to); cursor = cursor.plusDays(1)) {
            if (!days.contains(cursor.getDayOfWeek())) {
                continue;
            }
            long weekIndex = ChronoUnit.DAYS.between(start, cursor) / 7;
            if (weekIndex % interval != 0) {
                continue;
            }
            dates.add(cursor);
        }
        return dates;
    }

    private List<LocalDate> expandMonthly(Routine routine, LocalDate from, LocalDate to) {
        int interval = Math.max(1, routine.getIntervalValue());
        int dayOfMonth = routine.getStartDate().getDayOfMonth();
        YearMonth startYm = YearMonth.from(routine.getStartDate());
        List<LocalDate> dates = new ArrayList<>();

        long monthsFromStartToWindow = ChronoUnit.MONTHS.between(startYm, YearMonth.from(from));
        long stepIndex = monthsFromStartToWindow <= 0
                ? 0
                : (monthsFromStartToWindow / interval);

        while (true) {
            YearMonth target = startYm.plusMonths(stepIndex * interval);
            if (target.isAfter(YearMonth.from(to))) {
                break;
            }
            if (dayOfMonth <= target.lengthOfMonth()) {
                LocalDate candidate = target.atDay(dayOfMonth);
                if (!candidate.isBefore(from) && !candidate.isAfter(to)
                        && !candidate.isBefore(routine.getStartDate())) {
                    dates.add(candidate);
                }
            }
            stepIndex++;
            if (stepIndex > 10_000) {
                break;
            }
        }
        return dates;
    }

    private void validateRequest(RoutineRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate");
        }
        if (request.endTime() != null && !request.endTime().isAfter(request.startTime())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endTime must be after startTime");
        }
        if (request.intervalValue() != null && request.intervalValue() < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "intervalValue must be at least 1");
        }

        RecurrenceType type = request.recurrenceType();
        if (type == RecurrenceType.WEEKLY || type == RecurrenceType.SELECTED_WEEKDAYS) {
            if (request.daysOfWeek() == null || request.daysOfWeek().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "daysOfWeek is required for " + type);
            }
            parseDaysOfWeek(request.daysOfWeek());
        }
    }

    private void validateOccurrenceWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from and to are required");
        }
        if (to.isBefore(from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "to must be on or after from");
        }
        long days = ChronoUnit.DAYS.between(from, to);
        if (days > MAX_OCCURRENCE_WINDOW_DAYS) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "occurrence window cannot exceed 90 days");
        }
    }

    private void requireOwnedCategoryIfPresent(UUID userId, UUID categoryId) {
        if (categoryId == null) {
            return;
        }
        categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    static Set<DayOfWeek> parseDaysOfWeek(String daysOfWeek) {
        if (daysOfWeek == null || daysOfWeek.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String part : daysOfWeek.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(token);
            } catch (NumberFormatException ex) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "daysOfWeek must contain digits 1-7 (Mon=1)");
            }
            if (value < 1 || value > 7) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "daysOfWeek must contain digits 1-7 (Mon=1)");
            }
            days.add(DayOfWeek.of(value));
        }
        if (days.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "daysOfWeek must contain digits 1-7 (Mon=1)");
        }
        return days;
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
