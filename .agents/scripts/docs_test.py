"""Tests the docs: do their links and documented Gradle commands still work?

    python .agents/scripts/docs_test.py                  # all tracked .md files, static checks
    python .agents/scripts/docs_test.py docs/x.md ...    # only these files
    python .agents/scripts/docs_test.py --gradle [...]   # also dry-run every documented gradlew command

Static checks (fast, no Gradle):
- relative Markdown links point to an existing file or folder,
- `gradlew` task paths name a real Gradle project (projects are flat: `:game-core`, never
  `:game-app:game-core`; see settings.gradle.kts).
With --gradle, every distinct `gradlew ...` line in a code block runs with `--dry-run`, which
fails on unknown projects, tasks or options without building anything.
Exit code 0 means no findings.
"""
import re
import shlex
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

from convention_lint import added_lines

ROOT = Path(__file__).resolve().parents[2]
LINK = re.compile(r"\[[^\]]*\]\(<?([^)\s>]+)>?(?:\s+\"[^\"]*\")?\)")
GRADLEW = re.compile(r"(?:\./|\.\\)?gradlew(?:\.bat)?\s+([^`#\n]+)")
FENCE = re.compile(r"^\s*(```|~~~)")


def projects():
    text = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    return {":"} | set(re.findall(r'include\("(:[^"]+)"\)', text))


def lines_with_fence(path):
    """(line number, text, inside a code block) for every line of the file."""
    inside, result = False, []
    for num, text in enumerate((ROOT / path).read_text(encoding="utf-8").splitlines(), 1):
        if FENCE.match(text):
            inside = not inside
            continue
        result.append((num, text, inside))
    return result


def task_paths(args):
    """Task selectors with a project part (':game-core:test') in a gradlew argument string."""
    try:
        tokens = shlex.split(args)
    except ValueError:
        tokens = args.split()
    skip_next = False
    for token in tokens:
        if skip_next or token.startswith("-"):
            skip_next = token in ("--tests", "-x", "--exclude-task")
            continue
        if ":" in token.strip(":") and not token.startswith("<"):
            yield token


def check_files(paths, added_only=False):
    known = projects()
    findings = []
    for path in paths:
        if not (ROOT / path).is_file():
            continue
        wanted = {num for num, _ in added_lines(path)} if added_only else None
        for num, text, in_code in lines_with_fence(path):
            if wanted is not None and num not in wanted:
                continue
            where = f"{path}:{num}"
            for match in GRADLEW.finditer(text):
                for task in task_paths(match.group(1)):
                    project = ":" + task.strip(":").rsplit(":", 1)[0]
                    if project not in known:
                        findings.append(f"{where}: `{task}` - no Gradle project `{project}` "
                                        "(projects are flat, e.g. :game-core)")
            if in_code:
                continue
            for target in LINK.findall(text):
                if re.match(r"^([a-z]+:|#|\{)", target):
                    continue
                file_part = unquote(target.split("#")[0].split("?")[0])
                base = ROOT if file_part.startswith("/") else (ROOT / path).parent
                if file_part and not (base / file_part.lstrip("/")).exists():
                    findings.append(f"{where}: broken link `{target}`")
    return findings


def gradle_commands(paths):
    commands = {}
    for path in paths:
        if not (ROOT / path).is_file():
            continue
        for num, text, in_code in lines_with_fence(path):
            match = GRADLEW.search(text) if in_code else None
            if match and "<" not in match.group(1):
                commands.setdefault(match.group(1).strip(), f"{path}:{num}")
    return commands


def dry_run(commands):
    from check_changes import GRADLEW as gradlew, find_jdk  # late import, check_changes imports us
    import os
    jdk = find_jdk()
    if not jdk:
        return ["--gradle skipped: no JDK 25 found"]
    findings = []
    for args, where in sorted(commands.items(), key=lambda c: c[1]):
        cmd = [gradlew, *shlex.split(args), "--dry-run", "-q", "--console=plain"]
        proc = subprocess.run(cmd, cwd=ROOT, env={**os.environ, "JAVA_HOME": jdk},
                              capture_output=True, text=True, encoding="utf-8", errors="replace")
        print(f"  {'ok ' if proc.returncode == 0 else 'ERR'} gradlew {args}", flush=True)
        if proc.returncode != 0:
            reason = next((l.strip() for l in proc.stderr.splitlines()
                           if "Cannot locate" in l or "not found" in l or "Unknown command-line" in l),
                          proc.stderr.strip().splitlines()[-1] if proc.stderr.strip() else "failed")
            findings.append(f"{where}: `gradlew {args}` fails: {reason}")
    return findings


def main(args):
    files = [a for a in args if not a.startswith("--")]
    if not files:
        files = subprocess.run(["git", "ls-files", "*.md"], cwd=ROOT, capture_output=True,
                               text=True, encoding="utf-8").stdout.splitlines()
    findings = check_files(files)
    if "--gradle" in args:
        findings += dry_run(gradle_commands(files))
    print("\n".join(findings) or f"docs ok ({len(files)} files)")
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
