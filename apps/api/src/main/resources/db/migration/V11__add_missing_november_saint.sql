INSERT INTO saints (
    id,
    slug,
    name,
    name_en,
    name_es,
    name_pl,
    feast_month,
    feast_day,
    image_url,
    feast_label_en,
    feast_label_es,
    feast_label_pl,
    summary_en,
    summary_es,
    summary_pl,
    biography_en,
    biography_es,
    biography_pl
) VALUES (
    '11-29_saint_saturninus_of_toulouse',
    '11-29_saint_saturninus_of_toulouse',
    'Saint Saturninus of Toulouse',
    'Saint Saturninus of Toulouse',
    'San Saturnino de Toulouse',
    'Święty Saturnin z Tuluzy',
    11,
    29,
    NULL,
    'Feast day: November 29',
    'Fiesta: 29 de noviembre',
    'Wspomnienie: 29 listopada',
    'A bishop and martyr remembered as an early shepherd of the Church in Toulouse.',
    'Obispo y mártir, recordado como uno de los primeros pastores de la Iglesia en Toulouse.',
    'Biskup i męczennik, wspominany jako jeden z pierwszych pasterzy Kościoła w Tuluzie.',
    'Saint Saturninus, also called Sernin, is remembered as an early bishop of Toulouse and a missionary witness in Gaul. Tradition holds that he refused to take part in pagan sacrifice and was killed for his fidelity to Christ. His memory remains closely tied to the Christian roots of Toulouse.',
    'San Saturnino, también llamado Sernin, es recordado como un antiguo obispo de Toulouse y testigo misionero en la Galia. La tradición cuenta que se negó a participar en sacrificios paganos y murió por su fidelidad a Cristo. Su memoria sigue unida a las raíces cristianas de Toulouse.',
    'Święty Saturnin, zwany także Serninem, jest pamiętany jako dawny biskup Tuluzy i misyjny świadek w Galii. Tradycja mówi, że odmówił udziału w pogańskiej ofierze i poniósł śmierć za wierność Chrystusowi. Jego pamięć pozostaje związana z chrześcijańskimi korzeniami Tuluzy.'
) ON CONFLICT (id) DO UPDATE
SET
    slug = EXCLUDED.slug,
    name = EXCLUDED.name,
    name_en = EXCLUDED.name_en,
    name_es = EXCLUDED.name_es,
    name_pl = EXCLUDED.name_pl,
    feast_month = EXCLUDED.feast_month,
    feast_day = EXCLUDED.feast_day,
    image_url = EXCLUDED.image_url,
    feast_label_en = EXCLUDED.feast_label_en,
    feast_label_es = EXCLUDED.feast_label_es,
    feast_label_pl = EXCLUDED.feast_label_pl,
    summary_en = EXCLUDED.summary_en,
    summary_es = EXCLUDED.summary_es,
    summary_pl = EXCLUDED.summary_pl,
    biography_en = EXCLUDED.biography_en,
    biography_es = EXCLUDED.biography_es,
    biography_pl = EXCLUDED.biography_pl,
    updated_at = NOW();

INSERT INTO saint_tags (saint_id, tag)
VALUES
    ('11-29_saint_saturninus_of_toulouse', 'Bishop'),
    ('11-29_saint_saturninus_of_toulouse', 'Martyr'),
    ('11-29_saint_saturninus_of_toulouse', 'Missionary')
ON CONFLICT DO NOTHING;

INSERT INTO saint_patronages (saint_id, patronage)
VALUES
    ('11-29_saint_saturninus_of_toulouse', 'Toulouse')
ON CONFLICT DO NOTHING;

INSERT INTO saint_sources (saint_id, source_text, source_url, sort_order)
SELECT
    '11-29_saint_saturninus_of_toulouse',
    'Vatican News Saint of the Day: St. Saturnin, Martyr',
    'https://www.vaticannews.va/en/saints/11/29.html',
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM saint_sources
    WHERE saint_id = '11-29_saint_saturninus_of_toulouse'
      AND source_url = 'https://www.vaticannews.va/en/saints/11/29.html'
);
