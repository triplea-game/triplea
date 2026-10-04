"""Convention lint on the lines added to Java files (not the whole file).

Covers the conventions that cannot be switched on repo-wide in check-custom-style because of
existing violations, plus a few that no tool checks. Run it on changed files:
`python .agents/scripts/convention_lint.py <file>...`
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TEST_RE = re.compile(r"(^|/)src/(test|testFixtures)/")

INCLUDE = ROOT / ".build" / "code-convention-checks" / "style.include"
COLLECTION_CALLS = [l.strip() for l in (INCLUDE / "disallowed-collection-calls").read_text().splitlines()
                    if l.strip()]
CONCRETE_TYPES = [re.compile(l.strip()) for l in (INCLUDE / "concrete-collection-types").read_text().splitlines()
                  if l.strip()]

TODO = re.compile(r"\bTODO\b(?!: (Project|Issue|Topic)#\d+)")
OPTIONAL_PARAM = re.compile(r"[(,]\s*(final\s+)?Optional<[^>]+>\s+\w+\s*[,)]")
SIZE_CHECK = re.compile(r"size\(\) (==|!=|<=|>=|<|>) [01]\b")
JAVA_LOGGING = re.compile(r"@Log\b|^import java\.util\.logging")
JUNIT_ASSERT = re.compile(r"\b(assertEquals|assertNotEquals|assertTrue|assertFalse|assertNull|"
                          r"assertNotNull|assertSame|assertNotSame)\s*\(|org\.hamcrest")


def added_lines(path):
    """(line number, text) of every line added to the file compared with HEAD."""
    tracked = subprocess.run(["git", "ls-files", "--error-unmatch", path], cwd=ROOT,
                             capture_output=True).returncode == 0
    if not tracked:
        try:
            return list(enumerate((ROOT / path).read_text(encoding="utf-8").splitlines(), 1))
        except OSError:
            return []
    diff = subprocess.run(["git", "diff", "-U0", "HEAD", "--", path], cwd=ROOT, capture_output=True,
                          text=True, encoding="utf-8").stdout
    lines, num = [], 0
    for line in diff.splitlines():
        hunk = re.match(r"^@@ -\S+ \+(\d+)", line)
        if hunk:
            num = int(hunk.group(1))
        elif line.startswith("+") and not line.startswith("+++"):
            lines.append((num, line[1:]))
            num += 1
    return lines


def check_line(text, is_test):
    code = text.split("//")[0]
    if any(call in code for call in COLLECTION_CALLS) or "Arrays.asList" in code:
        yield "use List.of / Set.of / Map.of instead of Collections.* / Arrays.asList"
    if any(p.search(code) for p in CONCRETE_TYPES):
        yield "declare collections by their interface type (List, Map, Set), not the implementation"
    if TODO.search(text):
        yield "TODO needs a tracking token: `TODO: Issue#1234` (or Project#/Topic#)"
    if OPTIONAL_PARAM.search(code):
        yield "no Optional parameters, add an overload without the parameter instead"
    if SIZE_CHECK.search(code):
        yield "use isEmpty() / !isEmpty() instead of comparing size() with 0 or 1"
    if JAVA_LOGGING.search(code):
        yield "use Lombok @Slf4j, not @Log / java.util.logging"
    if is_test and JUNIT_ASSERT.search(code):
        yield "new test code uses AssertJ assertThat, not JUnit asserts or Hamcrest"


def lint(paths):
    findings = []
    for path in paths:
        if not path.endswith(".java"):
            continue
        is_test = bool(TEST_RE.search(path))
        for num, text in added_lines(path):
            findings += [f"{path}:{num}: {msg}" for msg in check_line(text, is_test)]
    return findings


if __name__ == "__main__":
    print("\n".join(lint(sys.argv[1:])) or "clean")
