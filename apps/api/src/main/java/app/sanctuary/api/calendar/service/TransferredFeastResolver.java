package app.sanctuary.api.calendar.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import app.sanctuary.api.calendar.model.TransferredFeastKey;
import app.sanctuary.api.calendar.rules.GregorianComputus;

@Component
public class TransferredFeastResolver {

    public LocalDate resolve(TransferredFeastKey key, int year) {
        LocalDate easter = GregorianComputus.easterSunday(year);
        LocalDate palmSunday = easter.minusDays(7);
        return switch (key) {
            case ST_JOSEPH -> transferredSaintJoseph(year, palmSunday);
            case ANNUNCIATION -> transferredAnnunciation(year, palmSunday, easter);
        };
    }

    private LocalDate transferredSaintJoseph(int year, LocalDate palmSunday) {
        LocalDate base = LocalDate.of(year, 3, 19);
        LocalDate holyWeekEnd = palmSunday.plusDays(6);
        if (!base.isBefore(palmSunday) && !base.isAfter(holyWeekEnd)) {
            return palmSunday.minusDays(1);
        }
        if (base.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return base.plusDays(1);
        }
        return base;
    }

    private LocalDate transferredAnnunciation(int year, LocalDate palmSunday, LocalDate easter) {
        LocalDate base = LocalDate.of(year, 3, 25);
        LocalDate easterOctaveEnd = easter.plusDays(7);
        if (!base.isBefore(palmSunday) && !base.isAfter(easterOctaveEnd)) {
            return easter.plusDays(8);
        }
        if (base.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return base.plusDays(1);
        }
        return base;
    }
}
