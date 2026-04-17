package app.sanctuary.api.calendar.service;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import app.sanctuary.api.calendar.model.LiturgicalAnchorKey;
import app.sanctuary.api.calendar.model.NovenaServingRule;
import app.sanctuary.api.calendar.model.NovenaServingWindowResult;
import app.sanctuary.api.calendar.rules.CalendarMath;

@Service
public class NovenaServingWindowResolver {

    private final LiturgicalAnchorService liturgicalAnchorService;

    public NovenaServingWindowResolver(LiturgicalAnchorService liturgicalAnchorService) {
        this.liturgicalAnchorService = liturgicalAnchorService;
    }

    public NovenaServingWindowResult resolve(NovenaServingRule rule, int year) {
        Map<LiturgicalAnchorKey, LocalDate> anchors = liturgicalAnchorService.getAnchors(year);
        LocalDate feast = resolveRule(
            rule.feastRuleType(),
            rule.feastRuleMonth(),
            rule.feastRuleDay(),
            rule.feastRuleAnchor(),
            rule.feastRuleOffsetDays(),
            rule.feastRuleWeekday(),
            rule.feastRuleWeekdayPolicy(),
            rule.feastRuleN(),
            rule.feastRuleDaysBefore(),
            anchors,
            null
        );
        if (feast == null) {
            throw new IllegalArgumentException("Unable to resolve feast rule for novena " + rule.novenaId());
        }

        LocalDate start = resolveRule(
            rule.startRuleType(),
            rule.startRuleMonth(),
            rule.startRuleDay(),
            rule.startRuleAnchor(),
            rule.startRuleOffsetDays(),
            rule.startRuleWeekday(),
            rule.startRuleWeekdayPolicy(),
            rule.startRuleN(),
            rule.startRuleDaysBefore(),
            anchors,
            rule.feastRuleAnchor()
        );
        if (start == null) {
            start = feast;
        }

        if ("fixed".equals(rule.startRuleType()) && start.isAfter(feast)) {
            start = start.minusYears(1);
        }

        if ("st_joseph".equals(rule.novenaId()) || "annunciation".equals(rule.novenaId())) {
            start = feast.minusDays(9);
        }

        LocalDate feastMinusOne = feast.minusDays(1);
        LocalDate end = start.isAfter(feastMinusOne) ? start : feastMinusOne;
        int effectiveDurationDays = effectiveDurationDays(rule);
        if (effectiveDurationDays > 0) {
            LocalDate byDuration = start.plusDays(effectiveDurationDays - 1L);
            if (byDuration.isBefore(end)) {
                end = byDuration;
            }
        }

        return new NovenaServingWindowResult(rule.novenaId(), start, end, feast);
    }

    private LocalDate resolveRule(
        String type,
        Integer month,
        Integer day,
        String anchor,
        Integer offsetDays,
        Integer weekday,
        String weekdayPolicy,
        Integer n,
        Integer daysBefore,
        Map<LiturgicalAnchorKey, LocalDate> anchors,
        String fallbackFeastAnchor
    ) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "fixed" -> resolveFixedRule(month, day, anchors);
            case "anchor" -> anchor == null ? null : anchors.get(parseAnchor(anchor));
            case "relative" -> {
                if (anchor == null || offsetDays == null) {
                    yield null;
                }
                LocalDate base = anchors.get(parseAnchor(anchor));
                if (base == null) {
                    yield null;
                }
                LocalDate moved = base.plusDays(offsetDays);
                yield weekday == null ? moved : CalendarMath.alignToWeekday(moved, weekday, weekdayPolicy == null ? "onOrAfter" : weekdayPolicy);
            }
            case "nth_weekday_after" -> {
                if (anchor == null || weekday == null || n == null || n < 1) {
                    yield null;
                }
                LocalDate base = anchors.get(parseAnchor(anchor));
                if (base == null) {
                    yield null;
                }
                LocalDate date = base.plusDays(1);
                int count = 0;
                while (count < n) {
                    if (date.getDayOfWeek().getValue() % 7 == weekday) {
                        count++;
                        if (count == n) {
                            yield date;
                        }
                    }
                    date = date.plusDays(1);
                }
                yield null;
            }
            case "before_feast" -> {
                if (daysBefore == null || daysBefore < 1) {
                    yield null;
                }
                String effectiveAnchor = anchor != null ? anchor : fallbackFeastAnchor;
                if (effectiveAnchor == null) {
                    yield null;
                }
                LocalDate resolvedFeast = anchors.get(parseAnchor(effectiveAnchor));
                yield resolvedFeast == null ? null : resolvedFeast.minusDays(daysBefore - 1L);
            }
            default -> null;
        };
    }

    private LiturgicalAnchorKey parseAnchor(String raw) {
        return LiturgicalAnchorKey.valueOf(raw.toUpperCase(Locale.US));
    }

    private LocalDate resolveFixedRule(Integer month, Integer day, Map<LiturgicalAnchorKey, LocalDate> anchors) {
        if (month == null || day == null) {
            return null;
        }
        if (month == 3 && day == 19) {
            return anchors.get(LiturgicalAnchorKey.ST_JOSEPH);
        }
        if (month == 3 && day == 25) {
            return anchors.get(LiturgicalAnchorKey.ANNUNCIATION);
        }
        return LocalDate.of(anchors.get(LiturgicalAnchorKey.EASTER).getYear(), month, day);
    }

    private int effectiveDurationDays(NovenaServingRule rule) {
        if (rule.sourceDurationDays() != null && rule.sourceDurationDays() > 0) {
            return rule.sourceDurationDays();
        }
        if (rule.entryDurationDays() != null && rule.entryDurationDays() > 1) {
            return rule.entryDurationDays() - 1;
        }
        return 0;
    }
}
