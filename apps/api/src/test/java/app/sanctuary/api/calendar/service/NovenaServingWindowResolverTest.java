package app.sanctuary.api.calendar.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import app.sanctuary.api.calendar.model.NovenaServingRule;

class NovenaServingWindowResolverTest {

    private NovenaServingWindowResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new NovenaServingWindowResolver(new LiturgicalAnchorService(new TransferredFeastResolver()));
    }

    @Test
    void resolvesBeforeFeastWindow() {
        NovenaServingRule rule = new NovenaServingRule(
            "generic_before_feast",
            "before_feast", null, null, "st_joseph", null, null, null, null, 9,
            "anchor", null, null, "st_joseph", null, null, null, null, null,
            9,
            9
        );

        var result = resolver.resolve(rule, 2026);
        assertEquals(LocalDate.of(2026, 3, 11), result.startDate());
        assertEquals(LocalDate.of(2026, 3, 19), result.feastDate());
        assertEquals(LocalDate.of(2026, 3, 18), result.endDate());
    }

    @Test
    void resolvesRelativeAnchorWindow() {
        NovenaServingRule rule = new NovenaServingRule(
            "divine_mercy",
            "relative", null, null, "good_friday", 0, null, null, null, null,
            "anchor", null, null, "divine_mercy_sunday", null, null, null, null, null,
            9,
            9
        );

        var result = resolver.resolve(rule, 2026);
        assertEquals(LocalDate.of(2026, 4, 3), result.startDate());
        assertEquals(LocalDate.of(2026, 4, 12), result.feastDate());
        assertEquals(LocalDate.of(2026, 4, 11), result.endDate());
    }

    @Test
    void resolvesTransferredFixedMarchFeastDates() {
        NovenaServingRule rule = new NovenaServingRule(
            "annunciation",
            "before_feast", null, null, "annunciation", null, null, null, null, 9,
            "fixed", 3, 25, null, null, null, null, null, null,
            9,
            9
        );

        var result = resolver.resolve(rule, 2027);
        assertEquals(LocalDate.of(2027, 4, 5), result.feastDate());
        assertEquals(LocalDate.of(2027, 3, 27), result.startDate());
        assertEquals(LocalDate.of(2027, 4, 4), result.endDate());
    }

    @Test
    void resolvesRelativeSundayWithLegacyZeroBasedWeekday() {
        NovenaServingRule rule = new NovenaServingRule(
            "holy_face_of_jesus",
            "relative", null, null, "shrove_tuesday", -10, 0, null, null, null,
            "anchor", null, null, "shrove_tuesday", null, null, null, null, null,
            9,
            9
        );

        var result = resolver.resolve(rule, 2026);
        assertEquals(LocalDate.of(2026, 2, 8), result.startDate());
        assertEquals(LocalDate.of(2026, 2, 17), result.feastDate());
        assertEquals(LocalDate.of(2026, 2, 16), result.endDate());
    }

    @Test
    void resolvesRelativeFridayWithLegacyZeroBasedWeekday() {
        NovenaServingRule rule = new NovenaServingRule(
            "sacred_heart",
            "relative", null, null, "pentecost", 10, null, null, null, null,
            "relative", null, null, "pentecost", 19, 5, null, null, null,
            9,
            9
        );

        var result = resolver.resolve(rule, 2026);
        assertEquals(LocalDate.of(2026, 6, 3), result.startDate());
        assertEquals(LocalDate.of(2026, 6, 12), result.feastDate());
        assertEquals(LocalDate.of(2026, 6, 11), result.endDate());
    }

    @Test
    void resolvesPreviouslyRawLiturgicalNovenaRules() {
        NovenaServingRule holyFamily = new NovenaServingRule(
            "holy_family",
            "before_feast", null, null, "holy_family", null, null, null, null, 9,
            "anchor", null, null, "holy_family", null, null, null, null, null,
            9,
            9
        );
        NovenaServingRule immaculateHeart = new NovenaServingRule(
            "immaculate_heart_of_mary",
            "before_feast", null, null, "immaculate_heart", null, null, null, null, 9,
            "anchor", null, null, "immaculate_heart", null, null, null, null, null,
            9,
            9
        );
        NovenaServingRule maryQueenOfTheApostles = new NovenaServingRule(
            "mary_queen_of_the_apostles",
            "before_feast", null, null, "pentecost", null, null, null, null, 9,
            "anchor", null, null, "pentecost", null, null, null, null, null,
            9,
            9
        );

        assertEquals(LocalDate.of(2026, 12, 27), resolver.resolve(holyFamily, 2026).feastDate());
        assertEquals(LocalDate.of(2026, 6, 13), resolver.resolve(immaculateHeart, 2026).feastDate());
        assertEquals(LocalDate.of(2026, 5, 24), resolver.resolve(maryQueenOfTheApostles, 2026).feastDate());
    }
}
