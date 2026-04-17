package app.sanctuary.api.calendar.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import app.sanctuary.api.calendar.model.LiturgicalSeason;
import app.sanctuary.api.calendar.rules.CalendarMath;
import app.sanctuary.api.calendar.rules.GregorianComputus;

@Component
public class SeasonResolver {

    public LiturgicalSeason getSeason(LocalDate date) {
        int year = date.getYear();

        LocalDate advent1 = CalendarMath.firstSundayOfAdvent(year);
        LocalDate christmasCurrent = LocalDate.of(year, 12, 25);
        LocalDate christmasPrevious = LocalDate.of(year - 1, 12, 25);
        LocalDate baptismCurrent = CalendarMath.baptismOfTheLord(year);
        LocalDate baptismNext = CalendarMath.baptismOfTheLord(year + 1);

        if (!date.isBefore(advent1) && date.isBefore(christmasCurrent)) {
            return LiturgicalSeason.ADVENT;
        }
        if ((!date.isBefore(christmasPrevious) && !date.isAfter(baptismCurrent))
            || (!date.isBefore(christmasCurrent) && !date.isAfter(baptismNext))) {
            return LiturgicalSeason.CHRISTMAS;
        }

        LocalDate easter = GregorianComputus.easterSunday(year);
        LocalDate ashWednesday = easter.minusDays(46);
        LocalDate pentecost = easter.plusDays(49);
        if (!date.isBefore(ashWednesday) && date.isBefore(easter)) {
            return LiturgicalSeason.LENT;
        }
        if (!date.isBefore(easter) && !date.isAfter(pentecost)) {
            return LiturgicalSeason.EASTER;
        }
        return LiturgicalSeason.ORDINARY;
    }

    public String displayName(LiturgicalSeason season) {
        return switch (season) {
            case ADVENT -> "Advent";
            case CHRISTMAS -> "Christmas";
            case LENT -> "Lent";
            case EASTER -> "Easter";
            case ORDINARY -> "Ordinary Time";
        };
    }
}
