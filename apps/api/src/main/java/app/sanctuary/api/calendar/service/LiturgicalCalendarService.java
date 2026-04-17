package app.sanctuary.api.calendar.service;

import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import app.sanctuary.api.calendar.model.LiturgicalDayResult;
import app.sanctuary.api.calendar.model.LiturgicalSeason;
import app.sanctuary.api.calendar.model.RankType;
import app.sanctuary.api.calendar.model.TransferredFeastKey;
import app.sanctuary.api.calendar.rules.CalendarMath;
import app.sanctuary.api.calendar.rules.GregorianComputus;

@Service
public class LiturgicalCalendarService {

    private static final Map<RankType, Integer> RANK_PRIORITY = new EnumMap<>(RankType.class);

    static {
        RANK_PRIORITY.put(RankType.WEEKDAY, 0);
        RANK_PRIORITY.put(RankType.OPTIONAL_MEMORIAL, 1);
        RANK_PRIORITY.put(RankType.MEMORIAL, 2);
        RANK_PRIORITY.put(RankType.FEAST, 3);
        RANK_PRIORITY.put(RankType.SUNDAY, 4);
        RANK_PRIORITY.put(RankType.SOLEMNITY, 5);
        RANK_PRIORITY.put(RankType.TRIDUUM, 6);
    }

    private final SeasonResolver seasonResolver;
    private final TransferredFeastResolver transferredFeastResolver;

    public LiturgicalCalendarService(SeasonResolver seasonResolver, TransferredFeastResolver transferredFeastResolver) {
        this.seasonResolver = seasonResolver;
        this.transferredFeastResolver = transferredFeastResolver;
    }

    public LiturgicalDayResult getLiturgicalDay(LocalDate date) {
        Entry entry = resolveEntry(date);
        return new LiturgicalDayResult(
            date,
            entry.season(),
            entry.rank(),
            List.of(entry.rank()),
            URI.create(readingsUrl(date)),
            entry.rankType()
        );
    }

    public LiturgicalSeason getSeason(LocalDate date) {
        return seasonResolver.getSeason(date);
    }

    public String getPrimaryObservance(LocalDate date) {
        return resolveEntry(date).rank();
    }

    private Entry resolveEntry(LocalDate date) {
        int year = date.getYear();
        Entry entry = fallbackEntry(date);

        entry = higherPriority(entry, maybe(date, LocalDate.of(year, 1, 1), "Mary, the Holy Mother of God", LiturgicalSeason.CHRISTMAS, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, LocalDate.of(year, 1, 6), "Epiphany of the Lord", LiturgicalSeason.CHRISTMAS, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, CalendarMath.baptismOfTheLord(year), "Baptism of the Lord", LiturgicalSeason.CHRISTMAS, RankType.FEAST));
        entry = higherPriority(entry, maybe(date, transferredFeastResolver.resolve(TransferredFeastKey.ST_JOSEPH, year), "Saint Joseph, Spouse of the Blessed Virgin Mary", LiturgicalSeason.LENT, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, transferredFeastResolver.resolve(TransferredFeastKey.ANNUNCIATION, year), "Annunciation of the Lord", LiturgicalSeason.LENT, RankType.SOLEMNITY));

        LocalDate easter = GregorianComputus.easterSunday(year);
        LocalDate ashWednesday = easter.minusDays(46);
        LocalDate lent1 = ashWednesday.plusDays(4);
        LocalDate palmSunday = easter.minusDays(7);
        LocalDate holyThursday = easter.minusDays(3);
        LocalDate goodFriday = easter.minusDays(2);
        LocalDate holySaturday = easter.minusDays(1);
        LocalDate pentecost = easter.plusDays(49);
        LocalDate advent1 = CalendarMath.firstSundayOfAdvent(year);
        LocalDate christTheKing = advent1.minusDays(7);

        entry = higherPriority(entry, maybe(date, ashWednesday, "Ash Wednesday", LiturgicalSeason.LENT, RankType.WEEKDAY));
        entry = higherPriority(entry, maybe(date, lent1, "First Sunday of Lent", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, lent1.plusDays(7), "Second Sunday of Lent", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, lent1.plusDays(14), "Third Sunday of Lent", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, lent1.plusDays(21), "Fourth Sunday of Lent (Laetare Sunday)", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, lent1.plusDays(28), "Fifth Sunday of Lent", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, palmSunday, "Palm Sunday of the Passion of the Lord", LiturgicalSeason.LENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, holyThursday, "Holy Thursday (Evening Mass of the Lord’s Supper)", LiturgicalSeason.LENT, RankType.TRIDUUM));
        entry = higherPriority(entry, maybe(date, goodFriday, "Good Friday of the Passion of the Lord", LiturgicalSeason.LENT, RankType.TRIDUUM));
        entry = higherPriority(entry, maybe(date, holySaturday, "Holy Saturday", LiturgicalSeason.LENT, RankType.TRIDUUM));
        entry = higherPriority(entry, maybe(date, easter, "Easter Sunday of the Resurrection of the Lord", LiturgicalSeason.EASTER, RankType.SOLEMNITY));
        for (int offset = 1; offset <= 6; offset++) {
            entry = higherPriority(entry, maybe(date, easter.plusDays(offset), "Easter Octave (Day " + (offset + 1) + ")", LiturgicalSeason.EASTER, RankType.SOLEMNITY));
        }
        entry = higherPriority(entry, maybe(date, easter.plusDays(7), "Second Sunday of Easter (Divine Mercy Sunday)", LiturgicalSeason.EASTER, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(39), "Ascension of the Lord (Thursday)", LiturgicalSeason.EASTER, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(42), "Ascension of the Lord (Transferred to Sunday)", LiturgicalSeason.EASTER, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, pentecost, "Pentecost Sunday", LiturgicalSeason.EASTER, RankType.SOLEMNITY));

        LocalDate ordinaryPart1Start = CalendarMath.baptismOfTheLord(year).plusDays(1);
        LocalDate dayBeforeAsh = ashWednesday.minusDays(1);
        LocalDate otPart1FirstSunday = CalendarMath.sundayOnOrAfter(ordinaryPart1Start);
        LocalDate otPart1LastSunday = CalendarMath.sundayOnOrBefore(dayBeforeAsh);
        LocalDate sunday = otPart1FirstSunday;
        while (!sunday.isAfter(otPart1LastSunday)) {
            int week = 1 + (int) (ChronoUnit.DAYS.between(ordinaryPart1Start, sunday) / 7);
            int sundayNumber = week + 1;
            entry = higherPriority(entry, maybe(date, sunday, ordinal(sundayNumber) + " Sunday in Ordinary Time", LiturgicalSeason.ORDINARY, RankType.SUNDAY));
            sunday = sunday.plusDays(7);
        }

        LocalDate ordinaryPart2Start = pentecost.plusDays(1);
        int lastWeekBeforeLent = 1 + (int) (ChronoUnit.DAYS.between(ordinaryPart1Start, dayBeforeAsh) / 7);
        int ordinaryPart2BaseWeek = lastWeekBeforeLent + 1;
        sunday = CalendarMath.sundayOnOrAfter(ordinaryPart2Start);
        while (!sunday.isAfter(christTheKing)) {
            int week = ordinaryPart2BaseWeek + (int) (ChronoUnit.DAYS.between(ordinaryPart2Start, sunday) / 7);
            int sundayNumber = Math.min(34, week + 1);
            entry = higherPriority(entry, maybe(date, sunday, ordinal(sundayNumber) + " Sunday in Ordinary Time", LiturgicalSeason.ORDINARY, RankType.SUNDAY));
            sunday = sunday.plusDays(7);
        }

        entry = higherPriority(entry, maybe(date, easter.plusDays(56), "Trinity Sunday", LiturgicalSeason.ORDINARY, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(60), "The Most Holy Body and Blood of Christ (Corpus Christi) — Thursday", LiturgicalSeason.ORDINARY, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(63), "The Most Holy Body and Blood of Christ (Corpus Christi) — Sunday", LiturgicalSeason.ORDINARY, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(68), "Most Sacred Heart of Jesus", LiturgicalSeason.ORDINARY, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, easter.plusDays(69), "Immaculate Heart of Mary", LiturgicalSeason.ORDINARY, RankType.MEMORIAL));
        entry = higherPriority(entry, maybe(date, advent1, "First Sunday of Advent", LiturgicalSeason.ADVENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, advent1.plusDays(7), "Second Sunday of Advent", LiturgicalSeason.ADVENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, advent1.plusDays(14), "Third Sunday of Advent (Gaudete Sunday)", LiturgicalSeason.ADVENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, advent1.plusDays(21), "Fourth Sunday of Advent", LiturgicalSeason.ADVENT, RankType.SUNDAY));
        entry = higherPriority(entry, maybe(date, christTheKing, "Our Lord Jesus Christ, King of the Universe (Christ the King)", LiturgicalSeason.ORDINARY, RankType.SOLEMNITY));
        entry = higherPriority(entry, maybe(date, LocalDate.of(year, 12, 25), "The Nativity of the Lord (Christmas)", LiturgicalSeason.CHRISTMAS, RankType.SOLEMNITY));

        return entry;
    }

    private Entry fallbackEntry(LocalDate date) {
        LiturgicalSeason season = seasonResolver.getSeason(date);
        String weekday = switch (date.getDayOfWeek()) {
            case MONDAY -> "Monday";
            case TUESDAY -> "Tuesday";
            case WEDNESDAY -> "Wednesday";
            case THURSDAY -> "Thursday";
            case FRIDAY -> "Friday";
            case SATURDAY -> "Saturday";
            case SUNDAY -> "Sunday";
        };
        return new Entry(weekday + " of " + seasonResolver.displayName(season), season, RankType.WEEKDAY);
    }

    private Entry maybe(LocalDate requested, LocalDate match, String rank, LiturgicalSeason season, RankType rankType) {
        return requested.equals(match) ? new Entry(rank, season, rankType) : null;
    }

    private Entry higherPriority(Entry current, Entry candidate) {
        if (candidate == null) {
            return current;
        }
        int currentPriority = RANK_PRIORITY.getOrDefault(current.rankType(), 0);
        int candidatePriority = RANK_PRIORITY.getOrDefault(candidate.rankType(), 0);
        return candidatePriority >= currentPriority ? candidate : current;
    }

    private String readingsUrl(LocalDate date) {
        return String.format(
            "https://bible.usccb.org/bible/readings/%02d%02d%02d.cfm",
            date.getMonthValue(),
            date.getDayOfMonth(),
            date.getYear() % 100
        );
    }

    private String ordinal(int n) {
        String[] words = {
            "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth",
            "Tenth", "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth", "Sixteenth",
            "Seventeenth", "Eighteenth", "Nineteenth", "Twentieth", "Twenty-First", "Twenty-Second",
            "Twenty-Third", "Twenty-Fourth", "Twenty-Fifth", "Twenty-Sixth", "Twenty-Seventh",
            "Twenty-Eighth", "Twenty-Ninth", "Thirtieth", "Thirty-First", "Thirty-Second",
            "Thirty-Third", "Thirty-Fourth"
        };
        if (n >= 1 && n < words.length) {
            return words[n];
        }
        return n + "th";
    }

    private record Entry(String rank, LiturgicalSeason season, RankType rankType) {
    }
}
