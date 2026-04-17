package app.sanctuary.api.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.sanctuary.api.calendar.model.LiturgicalAnchorKey;
import app.sanctuary.api.calendar.model.LiturgicalSeason;
import app.sanctuary.api.calendar.model.TransferredFeastKey;

class LiturgicalCalendarServiceTest {

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
}
