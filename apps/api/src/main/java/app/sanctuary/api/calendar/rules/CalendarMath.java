package app.sanctuary.api.calendar.rules;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class CalendarMath {

    private CalendarMath() {
    }

    public static LocalDate sundayOnOrAfter(LocalDate date) {
        return nextWeekday(date, DayOfWeek.SUNDAY, true);
    }

    public static LocalDate sundayOnOrBefore(LocalDate date) {
        return previousWeekday(date, DayOfWeek.SUNDAY, true);
    }

    public static LocalDate nextWeekday(LocalDate date, DayOfWeek target, boolean includeSameDay) {
        if (includeSameDay && date.getDayOfWeek() == target) {
            return date;
        }
        return date.with(TemporalAdjusters.next(target));
    }

    public static LocalDate previousWeekday(LocalDate date, DayOfWeek target, boolean includeSameDay) {
        if (includeSameDay && date.getDayOfWeek() == target) {
            return date;
        }
        return date.with(TemporalAdjusters.previous(target));
    }

    public static LocalDate firstSundayOfAdvent(int year) {
        return sundayOnOrAfter(LocalDate.of(year, 11, 27));
    }

    public static LocalDate baptismOfTheLord(int year) {
        return nextWeekday(LocalDate.of(year, 1, 6), DayOfWeek.SUNDAY, false);
    }

    public static LocalDate holyFamilyDate(int year) {
        LocalDate dec26 = LocalDate.of(year, 12, 26);
        for (int i = 0; i <= 5; i++) {
            LocalDate candidate = dec26.plusDays(i);
            if (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                return candidate;
            }
        }
        return LocalDate.of(year, 12, 30);
    }

    public static LocalDate alignToWeekday(LocalDate base, int targetWeekdayZeroBased, String policy) {
        DayOfWeek target = DayOfWeek.of(targetWeekdayZeroBased + 1);
        LocalDate candidate = base;
        if ("onOrBefore".equals(policy)) {
            while (candidate.getDayOfWeek() != target) {
                candidate = candidate.minusDays(1);
            }
            return candidate;
        }
        while (candidate.getDayOfWeek() != target) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
