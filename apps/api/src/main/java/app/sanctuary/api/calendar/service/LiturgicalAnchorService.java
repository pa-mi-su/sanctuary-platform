package app.sanctuary.api.calendar.service;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import app.sanctuary.api.calendar.model.LiturgicalAnchorKey;
import app.sanctuary.api.calendar.model.TransferredFeastKey;
import app.sanctuary.api.calendar.rules.CalendarMath;
import app.sanctuary.api.calendar.rules.GregorianComputus;

@Service
public class LiturgicalAnchorService {

    private final TransferredFeastResolver transferredFeastResolver;

    public LiturgicalAnchorService(TransferredFeastResolver transferredFeastResolver) {
        this.transferredFeastResolver = transferredFeastResolver;
    }

    public Map<LiturgicalAnchorKey, LocalDate> getAnchors(int year) {
        LocalDate easter = GregorianComputus.easterSunday(year);
        EnumMap<LiturgicalAnchorKey, LocalDate> anchors = new EnumMap<>(LiturgicalAnchorKey.class);

        anchors.put(LiturgicalAnchorKey.EASTER, easter);
        anchors.put(LiturgicalAnchorKey.ASH_WEDNESDAY, easter.minusDays(46));
        anchors.put(LiturgicalAnchorKey.SHROVE_TUESDAY, easter.minusDays(47));
        anchors.put(LiturgicalAnchorKey.PALM_SUNDAY, easter.minusDays(7));
        anchors.put(LiturgicalAnchorKey.HOLY_THURSDAY, easter.minusDays(3));
        anchors.put(LiturgicalAnchorKey.GOOD_FRIDAY, easter.minusDays(2));
        anchors.put(LiturgicalAnchorKey.HOLY_SATURDAY, easter.minusDays(1));
        anchors.put(LiturgicalAnchorKey.DIVINE_MERCY_SUNDAY, easter.plusDays(7));
        anchors.put(LiturgicalAnchorKey.ASCENSION_THURSDAY, easter.plusDays(39));
        anchors.put(LiturgicalAnchorKey.ASCENSION_SUNDAY, easter.plusDays(42));
        anchors.put(LiturgicalAnchorKey.PENTECOST, easter.plusDays(49));
        anchors.put(LiturgicalAnchorKey.TRINITY_SUNDAY, easter.plusDays(56));
        anchors.put(LiturgicalAnchorKey.CORPUS_CHRISTI, easter.plusDays(60));
        anchors.put(LiturgicalAnchorKey.CORPUS_CHRISTI_SUNDAY, easter.plusDays(63));
        anchors.put(LiturgicalAnchorKey.SACRED_HEART, easter.plusDays(68));
        anchors.put(LiturgicalAnchorKey.IMMACULATE_HEART, easter.plusDays(69));
        anchors.put(LiturgicalAnchorKey.CHRISTMAS, LocalDate.of(year, 12, 25));
        anchors.put(LiturgicalAnchorKey.CHRISTMAS_EVE, LocalDate.of(year, 12, 24));
        anchors.put(LiturgicalAnchorKey.NEW_YEARS_EVE, LocalDate.of(year, 12, 31));
        anchors.put(LiturgicalAnchorKey.MARY_MOTHER_OF_GOD, LocalDate.of(year, 1, 1));
        anchors.put(LiturgicalAnchorKey.EPIPHANY, LocalDate.of(year, 1, 6));
        anchors.put(LiturgicalAnchorKey.BAPTISM_OF_THE_LORD, CalendarMath.baptismOfTheLord(year));
        anchors.put(LiturgicalAnchorKey.HOLY_FAMILY, CalendarMath.holyFamilyDate(year));
        anchors.put(LiturgicalAnchorKey.ADVENT_1, CalendarMath.firstSundayOfAdvent(year));
        anchors.put(LiturgicalAnchorKey.CHRIST_KING, CalendarMath.firstSundayOfAdvent(year).minusDays(7));
        anchors.put(LiturgicalAnchorKey.ANNUNCIATION, transferredFeastResolver.resolve(TransferredFeastKey.ANNUNCIATION, year));
        anchors.put(LiturgicalAnchorKey.ST_JOSEPH, transferredFeastResolver.resolve(TransferredFeastKey.ST_JOSEPH, year));
        anchors.put(LiturgicalAnchorKey.ASSUMPTION, LocalDate.of(year, 8, 15));
        anchors.put(LiturgicalAnchorKey.ALL_SAINTS, LocalDate.of(year, 11, 1));
        anchors.put(LiturgicalAnchorKey.IMMACULATE_CONCEPTION, LocalDate.of(year, 12, 8));
        return anchors;
    }

    public LocalDate getAnchor(LiturgicalAnchorKey key, int year) {
        return getAnchors(year).get(key);
    }
}
