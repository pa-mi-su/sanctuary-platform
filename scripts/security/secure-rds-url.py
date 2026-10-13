#!/usr/bin/env python3
"""Write a verified-TLS JDBC URL to GitHub's environment file; never handle credentials."""
import os
import urllib.parse

url = os.environ["SANCTUARY_DB_URL"].strip()
if not url.startswith("jdbc:postgresql://"):
    raise SystemExit("Expected a PostgreSQL JDBC URL")
parsed = urllib.parse.urlsplit(url[5:])
if not parsed.hostname or parsed.username or parsed.password or parsed.fragment:
    raise SystemExit("Database URL must contain a host and no embedded credentials or fragment")
parameters = [(key, value) for key, value in urllib.parse.parse_qsl(parsed.query, keep_blank_values=True)
              if not key.lower().startswith("ssl")]
if any(key.lower() in {"user", "password"} for key, _ in parameters):
    raise SystemExit("Pass database credentials through Secrets Manager, not the URL")
parameters.extend([("sslmode", "verify-full"), ("sslrootcert", "/app/rds-ca.pem")])
secured = "jdbc:" + urllib.parse.urlunsplit(parsed._replace(query=urllib.parse.urlencode(parameters)))
print("SANCTUARY_DB_URL=" + secured)
