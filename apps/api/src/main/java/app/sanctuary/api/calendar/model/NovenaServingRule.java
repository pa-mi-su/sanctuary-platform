package app.sanctuary.api.calendar.model;

public record NovenaServingRule(
    String novenaId,
    String startRuleType,
    Integer startRuleMonth,
    Integer startRuleDay,
    String startRuleAnchor,
    Integer startRuleOffsetDays,
    Integer startRuleWeekday,
    String startRuleWeekdayPolicy,
    Integer startRuleN,
    Integer startRuleDaysBefore,
    String feastRuleType,
    Integer feastRuleMonth,
    Integer feastRuleDay,
    String feastRuleAnchor,
    Integer feastRuleOffsetDays,
    Integer feastRuleWeekday,
    String feastRuleWeekdayPolicy,
    Integer feastRuleN,
    Integer feastRuleDaysBefore,
    Integer entryDurationDays,
    Integer sourceDurationDays
) {
}
