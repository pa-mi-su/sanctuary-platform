#!/usr/bin/env python3

from __future__ import annotations

import argparse
import csv
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any
from urllib.parse import quote


DEFAULT_DB_URL = "postgresql://sanctuary:change-me-now@localhost:5432/sanctuary"
DEFAULT_PLATFORM_ROOT = Path("/Users/pms/repos/sanctuary-platform")
DEFAULT_NORMALIZED_NOVENAS = Path("/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json")
DEFAULT_NOVENA_INDEX = Path("/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas_index.json")
DEFAULT_LEGACY_NOVENA_DIR = Path("/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas")
NULL_TOKEN = "__SANCTUARY_NULL__"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="One-time external import of active novenas into PostgreSQL."
    )
    parser.add_argument(
        "--platform-root",
        type=Path,
        default=DEFAULT_PLATFORM_ROOT,
        help="Root of the sanctuary-platform repo. Used to locate .env by default.",
    )
    parser.add_argument(
        "--normalized-novenas",
        type=Path,
        default=DEFAULT_NORMALIZED_NOVENAS,
        help="Path to normalized novenas.json.",
    )
    parser.add_argument(
        "--novena-index",
        type=Path,
        default=DEFAULT_NOVENA_INDEX,
        help="Path to novenas_index.json.",
    )
    parser.add_argument(
        "--legacy-novena-dir",
        type=Path,
        default=DEFAULT_LEGACY_NOVENA_DIR,
        help="Directory containing legacy novena JSON documents.",
    )
    parser.add_argument(
        "--database-url",
        default=None,
        help="PostgreSQL connection URL. Defaults to SANCTUARY_DB_URL from .env or local default.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Validate source data and print row counts without touching the database.",
    )
    return parser.parse_args()


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def load_dotenv_values(env_path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not env_path.exists():
        return values

    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip()
    return values


def detect_database_url(platform_root: Path, explicit_url: str | None) -> str:
    if explicit_url:
        return to_psql_connection_url(explicit_url, platform_root)
    if os.getenv("SANCTUARY_DB_URL"):
        return to_psql_connection_url(os.environ["SANCTUARY_DB_URL"], platform_root)

    env_values = load_dotenv_values(platform_root / ".env")
    return to_psql_connection_url(env_values.get("SANCTUARY_DB_URL", DEFAULT_DB_URL), platform_root)


def to_psql_connection_url(url: str, platform_root: Path) -> str:
    if url.startswith("postgresql://") or url.startswith("postgres://"):
        return url

    if not url.startswith("jdbc:postgresql://"):
        return url

    env_values = load_dotenv_values(platform_root / ".env")
    username = os.getenv("SANCTUARY_DB_USERNAME") or env_values.get("SANCTUARY_DB_USERNAME", "sanctuary")
    password = os.getenv("SANCTUARY_DB_PASSWORD") or env_values.get("SANCTUARY_DB_PASSWORD", "change-me-now")

    jdbc_prefix = "jdbc:postgresql://"
    remainder = url[len(jdbc_prefix):]
    return f"postgresql://{quote(username)}:{quote(password)}@{remainder}"


def localized_value(mapping: dict[str, Any] | None, locale: str) -> str:
    if not mapping:
        return ""
    value = mapping.get(locale)
    if isinstance(value, str):
        return value
    return ""


def trim(value: Any) -> str:
    if isinstance(value, str):
        return value.strip()
    return ""


def json_text(value: Any) -> str:
    if value is None:
        return ""
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"))


def sql_literal(value: Any) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, int):
        return str(value)
    text = str(value).replace("'", "''")
    return f"'{text}'"


def load_legacy_docs(legacy_dir: Path) -> dict[str, dict[str, Any]]:
    docs: dict[str, dict[str, Any]] = {}
    for path in sorted(legacy_dir.glob("*.json")):
        docs[path.stem] = load_json(path)
    return docs


def build_rows(
    normalized_novenas: list[dict[str, Any]],
    novena_index: list[dict[str, Any]],
    legacy_docs: dict[str, dict[str, Any]],
) -> dict[str, list[list[Any]]]:
    indexed_ids = [entry["id"] for entry in novena_index]
    indexed_id_set = set(indexed_ids)

    normalized_by_id = {entry["id"]: entry for entry in normalized_novenas}
    missing_in_normalized = sorted(indexed_id_set - set(normalized_by_id))
    if missing_in_normalized:
        raise SystemExit(
            f"Indexed novenas missing from normalized novenas.json: {missing_in_normalized[:10]}"
        )

    novenas_rows: list[list[Any]] = []
    novena_tags_rows: list[list[Any]] = []
    novena_days_rows: list[list[Any]] = []
    novena_intentions_rows: list[list[Any]] = []
    novena_serving_rules_rows: list[list[Any]] = []

    total_days = 0
    total_tags = 0
    total_intentions = 0

    for index_entry in novena_index:
        novena_id = index_entry["id"]
        normalized = normalized_by_id[novena_id]
        legacy = legacy_docs.get(novena_id, {})

        novenas_rows.append(
            [
                normalized["id"],
                normalized["slug"],
                localized_value(normalized.get("titleByLocale"), "en"),
                localized_value(normalized.get("titleByLocale"), "es"),
                localized_value(normalized.get("titleByLocale"), "pl"),
                localized_value(normalized.get("descriptionByLocale"), "en"),
                localized_value(normalized.get("descriptionByLocale"), "es"),
                localized_value(normalized.get("descriptionByLocale"), "pl"),
                normalized["durationDays"],
                trim(normalized.get("imageURL")),
            ]
        )

        for tag in normalized.get("tags", []):
            novena_tags_rows.append([novena_id, trim(tag)])
            total_tags += 1

        for day in normalized.get("days", []):
            novena_days_rows.append(
                [
                    novena_id,
                    day["dayNumber"],
                    localized_value(day.get("titleByLocale"), "en"),
                    localized_value(day.get("titleByLocale"), "es"),
                    localized_value(day.get("titleByLocale"), "pl"),
                    localized_value(day.get("scriptureByLocale"), "en"),
                    localized_value(day.get("scriptureByLocale"), "es"),
                    localized_value(day.get("scriptureByLocale"), "pl"),
                    localized_value(day.get("prayerByLocale"), "en"),
                    localized_value(day.get("prayerByLocale"), "es"),
                    localized_value(day.get("prayerByLocale"), "pl"),
                    localized_value(day.get("reflectionByLocale"), "en"),
                    localized_value(day.get("reflectionByLocale"), "es"),
                    localized_value(day.get("reflectionByLocale"), "pl"),
                    localized_value(day.get("bodyByLocale"), "en"),
                    localized_value(day.get("bodyByLocale"), "es"),
                    localized_value(day.get("bodyByLocale"), "pl"),
                ]
            )
            total_days += 1

        for locale_key, locale in (("intentions", "en"), ("intentions_es", "es"), ("intentions_pl", "pl")):
            for sort_order, intention in enumerate(legacy.get(locale_key, []) or []):
                text = trim(intention)
                if not text:
                    continue
                novena_intentions_rows.append([novena_id, locale, text, sort_order])
                total_intentions += 1

        start_rule = index_entry.get("startRule") or {}
        feast_rule = index_entry.get("feastRule") or {}
        novena_serving_rules_rows.append(
            [
                novena_id,
                trim(start_rule.get("type")),
                start_rule.get("month"),
                start_rule.get("day"),
                trim(start_rule.get("anchor")),
                start_rule.get("offsetDays"),
                start_rule.get("weekday"),
                trim(start_rule.get("weekdayPolicy")),
                start_rule.get("n"),
                start_rule.get("daysBefore"),
                trim(feast_rule.get("type")),
                feast_rule.get("month"),
                feast_rule.get("day"),
                trim(feast_rule.get("anchor")),
                feast_rule.get("offsetDays"),
                feast_rule.get("weekday"),
                trim(feast_rule.get("weekdayPolicy")),
                feast_rule.get("n"),
                feast_rule.get("daysBefore"),
                index_entry.get("durationDays"),
                trim(index_entry.get("category")),
                trim(index_entry.get("notes")),
                json_text(index_entry.get("patronage")),
                json_text(index_entry.get("source")),
            ]
        )

    return {
        "novenas": novenas_rows,
        "novena_tags": novena_tags_rows,
        "novena_days": novena_days_rows,
        "novena_intentions": novena_intentions_rows,
        "novena_serving_rules": novena_serving_rules_rows,
        "counts": [
            ["active_novenas", len(novenas_rows)],
            ["novena_tags", total_tags],
            ["novena_days", total_days],
            ["novena_intentions", total_intentions],
            ["novena_serving_rules", len(novena_serving_rules_rows)],
        ],
    }


def write_csv(path: Path, rows: list[list[Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, quoting=csv.QUOTE_ALL)
        for row in rows:
            writer.writerow([NULL_TOKEN if value is None else value for value in row])


def build_sql_file(temp_dir: Path) -> Path:
    sql_path = temp_dir / "import_novenas.sql"
    sql_path.write_text(
        "\n".join(
            [
                "\\set ON_ERROR_STOP on",
                "BEGIN;",
                "TRUNCATE TABLE novena_intentions, novena_days, novena_tags, novena_serving_rules, novenas RESTART IDENTITY;",
                "\\copy novenas (id, slug, title_en, title_es, title_pl, description_en, description_es, description_pl, duration_days, image_url) FROM '"
                + str(temp_dir / "novenas.csv")
                + "' WITH (FORMAT csv, NULL '"
                + NULL_TOKEN
                + "')",
                "\\copy novena_tags (novena_id, tag) FROM '"
                + str(temp_dir / "novena_tags.csv")
                + "' WITH (FORMAT csv, NULL '"
                + NULL_TOKEN
                + "')",
                "\\copy novena_days (novena_id, day_number, title_en, title_es, title_pl, scripture_en, scripture_es, scripture_pl, prayer_en, prayer_es, prayer_pl, reflection_en, reflection_es, reflection_pl, body_en, body_es, body_pl) FROM '"
                + str(temp_dir / "novena_days.csv")
                + "' WITH (FORMAT csv, NULL '"
                + NULL_TOKEN
                + "')",
                "\\copy novena_intentions (novena_id, locale, intention_text, sort_order) FROM '"
                + str(temp_dir / "novena_intentions.csv")
                + "' WITH (FORMAT csv, NULL '"
                + NULL_TOKEN
                + "')",
                "",
            ]
        ),
        encoding="utf-8",
    )
    return sql_path


def build_serving_rules_insert(rows: list[list[Any]]) -> str:
    columns = [
        "novena_id",
        "start_rule_type",
        "start_rule_month",
        "start_rule_day",
        "start_rule_anchor",
        "start_rule_offset_days",
        "start_rule_weekday",
        "start_rule_weekday_policy",
        "start_rule_n",
        "start_rule_days_before",
        "feast_rule_type",
        "feast_rule_month",
        "feast_rule_day",
        "feast_rule_anchor",
        "feast_rule_offset_days",
        "feast_rule_weekday",
        "feast_rule_weekday_policy",
        "feast_rule_n",
        "feast_rule_days_before",
        "entry_duration_days",
        "category",
        "notes",
        "patronage",
        "source",
    ]
    values_sql = ",\n".join(
        "(" + ", ".join(sql_literal(value) for value in row) + ")" for row in rows
    )
    return (
        "INSERT INTO novena_serving_rules ("
        + ", ".join(columns)
        + ")\nVALUES\n"
        + values_sql
        + ";\n"
    )


def run_import(database_url: str, rows_by_table: dict[str, list[list[Any]]]) -> None:
    with tempfile.TemporaryDirectory(prefix="sanctuary_novena_import_") as temp_dir_raw:
        temp_dir = Path(temp_dir_raw)

        write_csv(temp_dir / "novenas.csv", rows_by_table["novenas"])
        write_csv(temp_dir / "novena_tags.csv", rows_by_table["novena_tags"])
        write_csv(temp_dir / "novena_days.csv", rows_by_table["novena_days"])
        write_csv(temp_dir / "novena_intentions.csv", rows_by_table["novena_intentions"])
        sql_path = build_sql_file(temp_dir)
        with sql_path.open("a", encoding="utf-8") as handle:
            handle.write(build_serving_rules_insert(rows_by_table["novena_serving_rules"]))
            handle.write("COMMIT;\n")

        subprocess.run(
            ["psql", database_url, "-v", "ON_ERROR_STOP=1", "-f", str(sql_path)],
            check=True,
        )


def main() -> int:
    args = parse_args()
    database_url = detect_database_url(args.platform_root, args.database_url)

    normalized_novenas = load_json(args.normalized_novenas)
    novena_index = load_json(args.novena_index)
    legacy_docs = load_legacy_docs(args.legacy_novena_dir)
    rows_by_table = build_rows(normalized_novenas, novena_index, legacy_docs)

    print("Prepared novena import rows:")
    for name, count in rows_by_table["counts"]:
        print(f"  - {name}: {count}")

    if args.dry_run:
        print("Dry run only. No database changes were made.")
        return 0

    run_import(database_url, rows_by_table)
    print("Novena import completed successfully.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except subprocess.CalledProcessError as exc:
        print(f"Novena import failed while running psql: {exc}", file=sys.stderr)
        raise
