"""Checks for the uncommitted changes. Run this before you call a code change done:

    python .agents/scripts/check_changes.py              # everything below
    python .agents/scripts/check_changes.py --no-gradle  # only the fast checks (lint, docs)

1. convention lint on the lines added to changed Java files (convention_lint.py),
2. docs test on the lines added to changed Markdown files (docs_test.py),
3. for every Gradle module with changed sources or build script, the documented commands:
   `spotlessApply`, then `test`, then `check` (Checkstyle, PMD, spotlessCheck), then
   `.build/code-convention-checks/check-custom-style <module dir>`.

Exit code 0 means green. The Gradle output is written to build/check-changes.log.
Gradle needs a JDK 25: JAVA_HOME when it is one, else the first JDK 25 in the usual folders.
"""
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

from convention_lint import lint
from docs_test import check_files as docs_findings

ROOT = Path(__file__).resolve().parents[2]
LOG = ROOT / "build" / "check-changes.log"
GRADLEW = str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
JDK_VERSION = "25"
JDK_DIRS = (Path.home() / ".jdks", Path(r"C:\Program Files\Java"),
            Path(r"C:\Program Files\Eclipse Adoptium"), Path(r"C:\Program Files\Microsoft"),
            Path(r"C:\Program Files\Zulu"), Path("/usr/lib/jvm"))
# sources, resources and build scripts: a change here needs the module's tests
CODE_RE = re.compile(r"(^|/)(src/(main|test|testFixtures)/|build\.gradle\.kts$)")


def changed_files():
    """Changed and untracked files relative to the repo, with forward slashes."""
    def git(*args):
        return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True,
                              encoding="utf-8").stdout.splitlines()
    files = git("diff", "--name-only", "HEAD") + git("ls-files", "--others", "--exclude-standard")
    return sorted({f for f in files if (ROOT / f).is_file()})


def module_of(path):
    """Directory of the nearest build.gradle.kts above a file ('game-app/game-core').

    Gradle projects are flat and named after that directory (':game-core'), see settings.gradle.kts.
    """
    parts = path.split("/")[:-1]
    while parts:
        if (ROOT.joinpath(*parts) / "build.gradle.kts").exists():
            return "/".join(parts)
        parts.pop()
    return None


def modules_of(paths):
    return sorted({m for m in (module_of(p) for p in paths if CODE_RE.search(p)) if m})


def find_jdk():
    """JAVA_HOME for Gradle: the current one when it is a JDK 25, else the first JDK 25 found."""
    candidates = [Path(os.environ["JAVA_HOME"])] if os.environ.get("JAVA_HOME") else []
    for d in JDK_DIRS:
        if d.is_dir():
            candidates += sorted(d.iterdir(), reverse=True)
    for jdk in candidates:
        try:
            release = (jdk / "release").read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        if re.search(rf'^JAVA_VERSION="{JDK_VERSION}[".]', release, re.M):
            return str(jdk)
    return None


def run_checks(modules):
    """Gradle steps and check-custom-style for the modules; stops at the first red step."""
    jdk = find_jdk()
    if not jdk:
        return {"result": "unavailable", "time": time.time(),
                "tail": "no JDK 25 found (JAVA_HOME, ~/.jdks, Program Files, /usr/lib/jvm)"}
    env = {**os.environ, "JAVA_HOME": jdk}
    projects = [":" + m.rsplit("/", 1)[-1] for m in modules]  # flat names, see settings.gradle.kts
    steps = [(f"{task}", [GRADLEW, *[f"{p}:{task}" for p in projects], "--console=plain", "-q"])
             for task in ("spotlessApply", "test", "check")]
    bash = shutil.which("bash")  # PATH order matters on Windows: Git Bash, not WSL's System32 bash
    steps += [(f"check-custom-style {m}", [bash, ".build/code-convention-checks/check-custom-style", m])
              for m in modules if bash]
    output, started = "", time.time()
    for name, cmd in steps:
        try:
            proc = subprocess.run(cmd, cwd=str(ROOT), env=env, capture_output=True, text=True,
                                  encoding="utf-8", errors="replace", timeout=1800)
            output += f"\n=== {name}\n" + (proc.stdout or "") + (proc.stderr or "")
            green = proc.returncode == 0
        except subprocess.TimeoutExpired:
            output, green = output + f"\n=== {name} timed out after 1800s", False
        if not green:
            break
    LOG.parent.mkdir(parents=True, exist_ok=True)
    LOG.write_text(output, encoding="utf-8")
    lines = [l for l in output.splitlines() if l.strip() and "[OK]" not in l]
    return {"result": "green" if green else "red", "step": name, "time": time.time(),
            "seconds": round(time.time() - started), "tail": "\n".join(lines[-30:])}


def main(args):
    files = changed_files()
    red = False
    for title, findings in (("convention lint", lint(files)),
                            ("docs test", docs_findings([f for f in files if f.endswith(".md")],
                                                        added_only=True))):
        print(f"[{'RED' if findings else 'OK'}] {title}")
        for finding in findings:
            print(f"  {finding}")
        red = red or bool(findings)
    modules = modules_of(files)
    if "--no-gradle" in args or not modules:
        print("[--] Gradle: " + ("skipped" if modules else "no module sources changed"))
        return 1 if red else 0
    check = run_checks(modules)
    print(f"[{check['result'].upper()}] Gradle {', '.join(modules)}"
          + (f", failed at: {check['step']}" if check["result"] == "red" else ""))
    if check["result"] != "green":
        print(check["tail"] + f"\n  full log: {LOG.relative_to(ROOT).as_posix()}")
    return 1 if red or check["result"] != "green" else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
