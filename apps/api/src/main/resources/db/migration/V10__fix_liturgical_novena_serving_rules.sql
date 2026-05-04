UPDATE novena_serving_rules
SET
    start_rule_type = 'before_feast',
    start_rule_anchor = 'holy_family',
    start_rule_days_before = 9,
    feast_rule_type = 'anchor',
    feast_rule_anchor = 'holy_family',
    updated_at = NOW()
WHERE novena_id = 'holy_family';

UPDATE novena_serving_rules
SET
    start_rule_type = 'before_feast',
    start_rule_anchor = 'immaculate_heart',
    start_rule_days_before = 9,
    feast_rule_type = 'anchor',
    feast_rule_anchor = 'immaculate_heart',
    updated_at = NOW()
WHERE novena_id = 'immaculate_heart_of_mary';

UPDATE novena_serving_rules
SET
    start_rule_type = 'before_feast',
    start_rule_anchor = 'pentecost',
    start_rule_days_before = 9,
    feast_rule_type = 'anchor',
    feast_rule_anchor = 'pentecost',
    updated_at = NOW()
WHERE novena_id = 'mary_queen_of_the_apostles';
