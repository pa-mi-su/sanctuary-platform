ALTER TABLE novena_serving_rules
    ADD CONSTRAINT chk_novena_serving_rules_start_type
    CHECK (
        start_rule_type IS NULL
        OR start_rule_type IN ('fixed', 'anchor', 'relative', 'nth_weekday_after', 'before_feast')
    );

ALTER TABLE novena_serving_rules
    ADD CONSTRAINT chk_novena_serving_rules_feast_type
    CHECK (
        feast_rule_type IS NULL
        OR feast_rule_type IN ('fixed', 'anchor', 'relative', 'nth_weekday_after', 'before_feast')
    );

ALTER TABLE novena_serving_rules
    ADD CONSTRAINT chk_novena_serving_rules_start_weekday
    CHECK (start_rule_weekday IS NULL OR start_rule_weekday BETWEEN 0 AND 6);

ALTER TABLE novena_serving_rules
    ADD CONSTRAINT chk_novena_serving_rules_feast_weekday
    CHECK (feast_rule_weekday IS NULL OR feast_rule_weekday BETWEEN 0 AND 6);
