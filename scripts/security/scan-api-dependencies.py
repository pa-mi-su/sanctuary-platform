#!/usr/bin/env python3
"""Fail on OSV findings, network errors, expired exceptions, or newly used affected features.

Input: mvn dependency:tree -DoutputType=json -DoutputFile=target/dependency-tree.json
Only public Maven coordinates/versions are sent to OSV; source and credentials stay local.
"""
import argparse
import datetime
import json
import pathlib
import re
import sys
import urllib.request


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("tree", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    args = parser.parse_args()
    repo = pathlib.Path(__file__).resolve().parents[2]
    exceptions = json.loads((repo / "docs/security/api-advisory-exceptions.json").read_text())
    coordinates = set()

    def walk(node):
        if node.get("groupId") != "app.sanctuary":
            coordinates.add((node["groupId"] + ":" + node["artifactId"], node["version"]))
        for child in node.get("children", []):
            walk(child)

    walk(json.loads(args.tree.read_text()))
    packages = sorted(coordinates)
    results = []
    for start in range(0, len(packages), 100):
        queries = [{"package": {"ecosystem": "Maven", "name": name}, "version": version}
                   for name, version in packages[start:start + 100]]
        request = urllib.request.Request("https://api.osv.dev/v1/querybatch",
            data=json.dumps({"queries": queries}).encode(), headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=45) as response:
            batch = json.load(response)["results"]
        if len(batch) != len(queries):
            raise RuntimeError("Incomplete OSV response")
        results.extend(batch)

    source = "\n".join(path.read_text() for path in (repo / "apps/api/src/main/java").rglob("*.java"))
    findings = []
    for (package, version), result in zip(packages, results):
        for vulnerability in result.get("vulns", []):
            identifier = vulnerability["id"]
            exception = exceptions.get(identifier)
            accepted = bool(exception and exception["package"] == package and exception["version"] == version
                and datetime.date.today() <= datetime.date.fromisoformat(exception["review_by"])
                and not re.search(exception["forbidden_source_pattern"], source))
            findings.append({"package": package, "version": version, "id": identifier,
                             "status": "not-applicable-reviewed" if accepted else "action-required",
                             "assessment": exception if accepted else None})
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps({"scanned_at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "packages_scanned": len(packages), "findings": findings}, indent=2) + "\n")
    outstanding = [finding for finding in findings if finding["status"] == "action-required"]
    print(f"OSV: {len(packages)} packages, {len(outstanding)} actionable findings, {len(findings)-len(outstanding)} reviewed exceptions")
    for finding in outstanding:
        print(f"  {finding['package']} {finding['version']}: {finding['id']}")
    return 1 if outstanding else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(f"Dependency security scan failed: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(2)
