#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
API_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

ensure_java_21() {
  local current_java_home="${JAVA_HOME:-}"
  local homebrew_java_21="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  local discovered_java_home=""
  local discovered_version=""

  determine_java_major_version() {
    local java_home_candidate="$1"
    "${java_home_candidate}/bin/java" -version 2>&1 | awk -F[\".] '/version/ {print $2; exit}'
  }

  if [[ -n "${current_java_home}" && -x "${current_java_home}/bin/java" ]]; then
    local current_version
    current_version="$(determine_java_major_version "${current_java_home}")"
    if [[ "${current_version}" == "21" ]]; then
      export JAVA_HOME="${current_java_home}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return
    fi
  fi

  if [[ -x "${homebrew_java_21}/bin/java" ]]; then
    discovered_version="$(determine_java_major_version "${homebrew_java_21}")"
    if [[ "${discovered_version}" == "21" ]]; then
      export JAVA_HOME="${homebrew_java_21}"
      export PATH="${JAVA_HOME}/bin:${PATH}"
      return
    fi
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    discovered_java_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "${discovered_java_home}" && -x "${discovered_java_home}/bin/java" ]]; then
      discovered_version="$(determine_java_major_version "${discovered_java_home}")"
      if [[ "${discovered_version}" == "21" ]]; then
        export JAVA_HOME="${discovered_java_home}"
        export PATH="${JAVA_HOME}/bin:${PATH}"
        return
      fi
    fi
  fi

  echo "Java 21 is required to run Sanctuary API."
  echo "The runner could not find a valid Java 21 home."
  echo "If you installed Homebrew openjdk@21, make sure this path exists:"
  echo "  /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
  echo "If you want macOS to register it system-wide, run:"
  echo "  sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk"
  echo "Install a Java 21 JDK and retry. Example: brew install openjdk@21"
  exit 1
}

ensure_java_21

if [[ ! -f "${ROOT_DIR}/.env" ]]; then
  echo "Missing ${ROOT_DIR}/.env"
  echo "Copy .env.example to .env and fill in local values first."
  exit 1
fi

set -a
source "${ROOT_DIR}/.env"
set +a

required_vars=(
  SANCTUARY_DB_URL
  SANCTUARY_DB_USERNAME
  SANCTUARY_DB_PASSWORD
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required environment variable: ${var_name}"
    echo "Update ${ROOT_DIR}/.env before running the API."
    exit 1
  fi
done

cd "${API_DIR}"
exec mvn spring-boot:run -Dspring-boot.run.profiles=local
