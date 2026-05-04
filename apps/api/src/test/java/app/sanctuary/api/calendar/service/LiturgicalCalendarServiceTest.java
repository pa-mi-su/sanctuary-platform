package app.sanctuary.api.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.sanctuary.api.calendar.model.LiturgicalAnchorKey;
import app.sanctuary.api.calendar.model.LiturgicalSeason;
import app.sanctuary.api.calendar.model.TransferredFeastKey;

class LiturgicalCalendarServiceTest {

    private static final int MIN_SUPPORTED_YEAR = 1900;
    private static final int MAX_SUPPORTED_YEAR = 4099;

    private LiturgicalCalendarService liturgicalCalendarService;
    private LiturgicalAnchorService liturgicalAnchorService;
    private TransferredFeastResolver transferredFeastResolver;
    private SeasonResolver seasonResolver;

    @BeforeEach
    void setUp() {
        transferredFeastResolver = new TransferredFeastResolver();
        seasonResolver = new SeasonResolver();
        liturgicalAnchorService = new LiturgicalAnchorService(transferredFeastResolver);
        liturgicalCalendarService = new LiturgicalCalendarService(seasonResolver, transferredFeastResolver);
    }

    @Test
    void computesKnownEasterDates() {
        assertEquals(LocalDate.of(2026, 4, 5), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.EASTER, 2026));
        assertEquals(LocalDate.of(2027, 3, 28), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.EASTER, 2027));
        assertEquals(LocalDate.of(2000, 4, 23), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.EASTER, 2000));
    }

    @Test
    void resolvesSeasonsAcrossBoundaries() {
        assertEquals(LiturgicalSeason.LENT, seasonResolver.getSeason(LocalDate.of(2026, 3, 19)));
        assertEquals(LiturgicalSeason.EASTER, seasonResolver.getSeason(LocalDate.of(2026, 4, 5)));
        assertEquals(LiturgicalSeason.ADVENT, seasonResolver.getSeason(LocalDate.of(2026, 12, 1)));
        assertEquals(LiturgicalSeason.CHRISTMAS, seasonResolver.getSeason(LocalDate.of(2026, 12, 25)));
        assertEquals(LiturgicalSeason.CHRISTMAS, seasonResolver.getSeason(LocalDate.of(2027, 1, 10)));
    }

    @Test
    void transfersSaintJosephAndAnnunciationWhenRequired() {
        assertEquals(LocalDate.of(2027, 3, 19), transferredFeastResolver.resolve(TransferredFeastKey.ST_JOSEPH, 2027));
        assertEquals(LocalDate.of(2027, 4, 5), transferredFeastResolver.resolve(TransferredFeastKey.ANNUNCIATION, 2027));
    }

    @Test
    void computesExpectedAnchors() {
        assertEquals(LocalDate.of(2026, 11, 29), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.ADVENT_1, 2026));
        assertEquals(LocalDate.of(2026, 11, 22), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.CHRIST_KING, 2026));
        assertEquals(LocalDate.of(2026, 1, 11), liturgicalAnchorService.getAnchor(LiturgicalAnchorKey.BAPTISM_OF_THE_LORD, 2026));
    }

    @Test
    void returnsExpectedMajorObservances() {
        assertEquals(
            "Saint Joseph, Spouse of the Blessed Virgin Mary",
            liturgicalCalendarService.getLiturgicalDay(LocalDate.of(2026, 3, 19)).primaryRank()
        );
        assertEquals(
            "Easter Sunday of the Resurrection of the Lord",
            liturgicalCalendarService.getLiturgicalDay(LocalDate.of(2026, 4, 5)).primaryRank()
        );
    }

    @Test
    void resolvesFullYearMajorObservanceFixtures() {
        List<ExpectedObservance> fixtures = List.of(
            new ExpectedObservance("2025-01-01", "Mary, the Holy Mother of God", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2025-01-06", "Epiphany of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2025-01-12", "Baptism of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2025-03-05", "Ash Wednesday", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-03-09", "First Sunday of Lent", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-03-19", "Saint Joseph, Spouse of the Blessed Virgin Mary", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-03-25", "Annunciation of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-04-13", "Palm Sunday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-04-17", "Holy Thursday (Evening Mass of the Lord’s Supper)", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-04-18", "Good Friday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-04-19", "Holy Saturday", LiturgicalSeason.LENT),
            new ExpectedObservance("2025-04-20", "Easter Sunday of the Resurrection of the Lord", LiturgicalSeason.EASTER),
            new ExpectedObservance("2025-04-27", "Second Sunday of Easter (Divine Mercy Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2025-05-29", "Ascension of the Lord (Thursday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2025-06-01", "Ascension of the Lord (Transferred to Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2025-06-08", "Pentecost Sunday", LiturgicalSeason.EASTER),
            new ExpectedObservance("2025-06-15", "Trinity Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-06-19", "The Most Holy Body and Blood of Christ (Corpus Christi) — Thursday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-06-22", "The Most Holy Body and Blood of Christ (Corpus Christi) — Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-06-27", "Most Sacred Heart of Jesus", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-06-28", "Immaculate Heart of Mary", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-11-23", "Our Lord Jesus Christ, King of the Universe (Christ the King)", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2025-11-30", "First Sunday of Advent", LiturgicalSeason.ADVENT),
            new ExpectedObservance("2025-12-25", "The Nativity of the Lord (Christmas)", LiturgicalSeason.CHRISTMAS),

            new ExpectedObservance("2026-01-01", "Mary, the Holy Mother of God", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2026-01-06", "Epiphany of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2026-01-11", "Baptism of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2026-02-18", "Ash Wednesday", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-02-22", "First Sunday of Lent", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-03-19", "Saint Joseph, Spouse of the Blessed Virgin Mary", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-03-25", "Annunciation of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-03-29", "Palm Sunday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-04-02", "Holy Thursday (Evening Mass of the Lord’s Supper)", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-04-03", "Good Friday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-04-04", "Holy Saturday", LiturgicalSeason.LENT),
            new ExpectedObservance("2026-04-05", "Easter Sunday of the Resurrection of the Lord", LiturgicalSeason.EASTER),
            new ExpectedObservance("2026-04-12", "Second Sunday of Easter (Divine Mercy Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2026-05-14", "Ascension of the Lord (Thursday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2026-05-17", "Ascension of the Lord (Transferred to Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2026-05-24", "Pentecost Sunday", LiturgicalSeason.EASTER),
            new ExpectedObservance("2026-05-31", "Trinity Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-06-04", "The Most Holy Body and Blood of Christ (Corpus Christi) — Thursday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-06-07", "The Most Holy Body and Blood of Christ (Corpus Christi) — Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-06-12", "Most Sacred Heart of Jesus", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-06-13", "Immaculate Heart of Mary", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-11-22", "Our Lord Jesus Christ, King of the Universe (Christ the King)", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2026-11-29", "First Sunday of Advent", LiturgicalSeason.ADVENT),
            new ExpectedObservance("2026-12-25", "The Nativity of the Lord (Christmas)", LiturgicalSeason.CHRISTMAS),

            new ExpectedObservance("2027-01-01", "Mary, the Holy Mother of God", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2027-01-06", "Epiphany of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2027-01-10", "Baptism of the Lord", LiturgicalSeason.CHRISTMAS),
            new ExpectedObservance("2027-02-10", "Ash Wednesday", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-02-14", "First Sunday of Lent", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-19", "Saint Joseph, Spouse of the Blessed Virgin Mary", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-21", "Palm Sunday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-25", "Holy Thursday (Evening Mass of the Lord’s Supper)", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-26", "Good Friday of the Passion of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-27", "Holy Saturday", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-03-28", "Easter Sunday of the Resurrection of the Lord", LiturgicalSeason.EASTER),
            new ExpectedObservance("2027-04-04", "Second Sunday of Easter (Divine Mercy Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2027-04-05", "Annunciation of the Lord", LiturgicalSeason.LENT),
            new ExpectedObservance("2027-05-06", "Ascension of the Lord (Thursday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2027-05-09", "Ascension of the Lord (Transferred to Sunday)", LiturgicalSeason.EASTER),
            new ExpectedObservance("2027-05-16", "Pentecost Sunday", LiturgicalSeason.EASTER),
            new ExpectedObservance("2027-05-23", "Trinity Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-05-27", "The Most Holy Body and Blood of Christ (Corpus Christi) — Thursday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-05-30", "The Most Holy Body and Blood of Christ (Corpus Christi) — Sunday", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-06-04", "Most Sacred Heart of Jesus", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-06-05", "Immaculate Heart of Mary", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-11-21", "Our Lord Jesus Christ, King of the Universe (Christ the King)", LiturgicalSeason.ORDINARY),
            new ExpectedObservance("2027-11-28", "First Sunday of Advent", LiturgicalSeason.ADVENT),
            new ExpectedObservance("2027-12-25", "The Nativity of the Lord (Christmas)", LiturgicalSeason.CHRISTMAS)
        );

        for (ExpectedObservance fixture : fixtures) {
            var result = liturgicalCalendarService.getLiturgicalDay(LocalDate.parse(fixture.date()));
            assertEquals(fixture.rank(), result.primaryRank(), fixture.date());
            assertEquals(fixture.season(), result.season(), fixture.date());
            assertNotNull(result.readingsUrl(), fixture.date());
        }
    }

    @Test
    void resolvesMajorLiturgicalObservancesForEverySupportedYear() {
        for (int year = MIN_SUPPORTED_YEAR; year <= MAX_SUPPORTED_YEAR; year++) {
            Map<LiturgicalAnchorKey, LocalDate> anchors = liturgicalAnchorService.getAnchors(year);

            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.MARY_MOTHER_OF_GOD, "Mary, the Holy Mother of God");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.EPIPHANY, "Epiphany of the Lord");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.BAPTISM_OF_THE_LORD, "Baptism of the Lord");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ASH_WEDNESDAY, "Ash Wednesday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.PALM_SUNDAY, "Palm Sunday of the Passion of the Lord");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.HOLY_THURSDAY, "Holy Thursday (Evening Mass of the Lord’s Supper)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.GOOD_FRIDAY, "Good Friday of the Passion of the Lord");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.HOLY_SATURDAY, "Holy Saturday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.EASTER, "Easter Sunday of the Resurrection of the Lord");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.DIVINE_MERCY_SUNDAY, "Second Sunday of Easter (Divine Mercy Sunday)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ASCENSION_THURSDAY, "Ascension of the Lord (Thursday)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ASCENSION_SUNDAY, "Ascension of the Lord (Transferred to Sunday)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.PENTECOST, "Pentecost Sunday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.TRINITY_SUNDAY, "Trinity Sunday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.CORPUS_CHRISTI, "The Most Holy Body and Blood of Christ (Corpus Christi) — Thursday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.CORPUS_CHRISTI_SUNDAY, "The Most Holy Body and Blood of Christ (Corpus Christi) — Sunday");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.SACRED_HEART, "Most Sacred Heart of Jesus");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.IMMACULATE_HEART, "Immaculate Heart of Mary");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.CHRIST_KING, "Our Lord Jesus Christ, King of the Universe (Christ the King)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ADVENT_1, "First Sunday of Advent");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.CHRISTMAS, "The Nativity of the Lord (Christmas)");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ST_JOSEPH, "Saint Joseph, Spouse of the Blessed Virgin Mary");
            assertAnchoredObservance(year, anchors, LiturgicalAnchorKey.ANNUNCIATION, "Annunciation of the Lord");

            assertEquals(DayOfWeek.SUNDAY, anchors.get(LiturgicalAnchorKey.EASTER).getDayOfWeek(), "Easter " + year);
            assertEquals(DayOfWeek.WEDNESDAY, anchors.get(LiturgicalAnchorKey.ASH_WEDNESDAY).getDayOfWeek(), "Ash Wednesday " + year);
            assertEquals(DayOfWeek.SUNDAY, anchors.get(LiturgicalAnchorKey.PALM_SUNDAY).getDayOfWeek(), "Palm Sunday " + year);
            assertEquals(DayOfWeek.THURSDAY, anchors.get(LiturgicalAnchorKey.HOLY_THURSDAY).getDayOfWeek(), "Holy Thursday " + year);
            assertEquals(DayOfWeek.FRIDAY, anchors.get(LiturgicalAnchorKey.GOOD_FRIDAY).getDayOfWeek(), "Good Friday " + year);
            assertEquals(DayOfWeek.SATURDAY, anchors.get(LiturgicalAnchorKey.HOLY_SATURDAY).getDayOfWeek(), "Holy Saturday " + year);
            assertEquals(DayOfWeek.SUNDAY, anchors.get(LiturgicalAnchorKey.PENTECOST).getDayOfWeek(), "Pentecost " + year);
            assertEquals(DayOfWeek.SUNDAY, anchors.get(LiturgicalAnchorKey.ADVENT_1).getDayOfWeek(), "Advent 1 " + year);
            assertFalse(anchors.get(LiturgicalAnchorKey.EASTER).isBefore(LocalDate.of(year, 3, 22)), "Easter lower bound " + year);
            assertFalse(anchors.get(LiturgicalAnchorKey.EASTER).isAfter(LocalDate.of(year, 4, 25)), "Easter upper bound " + year);
            assertFalse(anchors.get(LiturgicalAnchorKey.ADVENT_1).isBefore(LocalDate.of(year, 11, 27)), "Advent lower bound " + year);
            assertFalse(anchors.get(LiturgicalAnchorKey.ADVENT_1).isAfter(LocalDate.of(year, 12, 3)), "Advent upper bound " + year);
        }
    }

    @Test
    void everyDateInSupportedRangeResolvesToADayAndSeason() {
        LocalDate cursor = LocalDate.of(MIN_SUPPORTED_YEAR, 1, 1);
        LocalDate end = LocalDate.of(MAX_SUPPORTED_YEAR, 12, 31);

        while (!cursor.isAfter(end)) {
            var result = liturgicalCalendarService.getLiturgicalDay(cursor);
            assertEquals(cursor, result.date());
            assertNotNull(result.season(), cursor.toString());
            assertNotNull(result.primaryRank(), cursor.toString());
            assertNotNull(result.rankType(), cursor.toString());
            assertNotNull(result.readingsUrl(), cursor.toString());
            cursor = cursor.plusDays(1);
        }
    }

    private void assertAnchoredObservance(
        int year,
        Map<LiturgicalAnchorKey, LocalDate> anchors,
        LiturgicalAnchorKey key,
        String expectedRank
    ) {
        LocalDate date = anchors.get(key);
        assertNotNull(date, key + " anchor " + year);
        assertTrue(date.getYear() == year || key == LiturgicalAnchorKey.ANNUNCIATION, key + " year " + year);
        assertEquals(expectedRank, liturgicalCalendarService.getLiturgicalDay(date).primaryRank(), key + " " + year);
    }

    private record ExpectedObservance(String date, String rank, LiturgicalSeason season) {
    }
}
