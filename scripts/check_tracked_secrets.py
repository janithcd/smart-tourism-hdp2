#!/usr/bin/env python3
"""Small CI guard for accidental credentials in tracked project files.

Firebase's Android google-services.json is a public client configuration. Its
API key is intentionally excluded from the Google-key check, not from other
checks. This is a guard for new commits, not a history or entropy scanner.
"""

import os
import re
import subprocess
import sys
from pathlib import Path


PATTERNS = {
    "Google API key": re.compile(rb"AIza[0-9A-Za-z_-]{35}"),
    "AWS access key ID": re.compile(rb"(?:AKIA|ASIA)[0-9A-Z]{16}"),
    "GitHub token": re.compile(
        rb"(?:ghp_|gho_|ghu_|ghs_|ghr_)[A-Za-z0-9_]{30,}"
        rb"|github_pat_[A-Za-z0-9_]{70,}"
    ),
    "private key": re.compile(
        rb"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"
    ),
}


def is_local_credential(path: Path) -> bool:
    name = path.name.lower()
    return (
        name in {"local.properties", "secrets.properties", ".env"}
        or (name.startswith(".env.") and name != ".env.example")
        or (name.endswith(".json") and ("service-account" in name or "credentials" in name))
        or path.suffix.lower() in {".jks", ".keystore", ".p12", ".pfx", ".pem"}
    )


def main() -> int:
    paths = subprocess.check_output(["git", "ls-files", "-z"]).split(b"\0")
    findings = []
    for raw_path in filter(None, paths):
        path = Path(os.fsdecode(raw_path))
        if is_local_credential(path):
            findings.append(f"{path}: local credential file is tracked")
            continue
        if not path.is_file() or path.stat().st_size > 2_000_000:
            continue
        data = path.read_bytes()
        if b"\0" in data[:4096]:
            continue
        for label, pattern in PATTERNS.items():
            if label == "Google API key" and path.name == "google-services.json":
                continue
            for match in pattern.finditer(data):
                line = data.count(b"\n", 0, match.start()) + 1
                findings.append(f"{path}:{line}: possible {label}")

    if findings:
        print("Potential tracked credentials (values suppressed):", file=sys.stderr)
        print("\n".join(findings), file=sys.stderr)
        return 1
    print("No tracked credential files or known key patterns found.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
