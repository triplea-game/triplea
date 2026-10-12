"""Checks the links and the Gradle project paths in the Markdown docs.

    python3 .build/docs-checks/check-docs.py                 # all tracked .md files
    python3 .build/docs-checks/check-docs.py docs/x.md ...   # only these (paths from the repo root)

Checks:
- relative inline links, `[text](target)`, point to an existing file or folder,
- task paths in `gradlew` commands name a project included in settings.gradle.kts.
Links in code blocks (toggled by ``` and ~~~ lines) and in `inline code` are not checked, nor
are reference-style links, HTML links and <targets with spaces>.
Exit code 0 means no findings.
"""
import re
import shlex
import subprocess
import sys
from pathlib import Path
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[2]
# the "](target)" part of a link, also the outer one of an image link "[![alt](img)](target)"
LINK = re.compile(r"\]\(<?([^)\s>]+)>?(?:\s+(?:\"[^\"]*\"|'[^']*'))?\)")
# arguments up to the end of the command ("&&", "|", ";", a comment or the end of inline code)
GRADLEW = re.compile(r"(?:\./|\.\\)?gradlew(?:\.bat)?\s+([^`#\n&|;]+)")
FENCE = re.compile(r"^\s*(```|~~~)")
INLINE_CODE = re.compile(r"`[^`]*`")
# each part starts with a letter, so "localhost:8080" in the prose after a command is no task
TASK_PATH = re.compile(r":?[a-zA-Z][\w.-]*(?::[a-zA-Z][\w.-]*)+")


def main(args):
    if args and args[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    options = [a for a in args if a.startswith("-")]
    if options:
        print(f"Unknown option {options[0]}, usage: check-docs.py [file.md ...]")
        return 2
    missing = [f"{a}: no such file" for a in args if not (ROOT / a).is_file()]
    files = args or tracked_markdown_files()
    findings = missing + check_files(files)
    if not findings:
        print(f"[OK] links and Gradle project paths in {len(files)} docs")
        return 0
    print("[FIX] docs check (.build/docs-checks/check-docs.py):", *findings, sep="\n")
    return 1


def tracked_markdown_files():
    output = subprocess.run(["git", "ls-files", "-z", "*.md"], cwd=ROOT, capture_output=True,
                            text=True, encoding="utf-8", check=True).stdout
    return [path for path in output.split("\0") if path]


def check_files(paths):
    projects = gradle_projects()
    findings = []
    for path in paths:
        for num, text, in_code in lines_with_fence(path):
            where = f"{path}:{num}"
            for match in GRADLEW.finditer(text):
                for task in task_paths(match.group(1)):
                    project = ":" + task.strip(":").rsplit(":", 1)[0]
                    if project not in projects:
                        findings.append(f"{where}: `{task}`: no Gradle project `{project}`")
            if not in_code:
                findings += [f"{where}: broken link `{target}`"
                             for target in LINK.findall(INLINE_CODE.sub("", text))
                             if is_broken(path, target)]
    return findings


def gradle_projects():
    text = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    return {":"} | set(re.findall(r'include\("(:[^"]+)"\)', text))


def lines_with_fence(path):
    """(line number, text, inside a code block) for every line except the fence lines themselves.

    A path that is not a file (eg: deleted but not yet committed) has no lines.
    """
    inside, result = False, []
    if not (ROOT / path).is_file():
        return result
    for num, text in enumerate((ROOT / path).read_text(encoding="utf-8").splitlines(), 1):
        if FENCE.match(text):
            inside = not inside
        else:
            result.append((num, text, inside))
    return result


def task_paths(args):
    """Task selectors with a project part, like ':game-core:test', in gradlew arguments."""
    try:
        tokens = shlex.split(args)
    except ValueError:
        tokens = args.split()
    skip_next = False
    for token in tokens:
        if skip_next or token.startswith("-"):
            # the task after -x / --exclude-task is checked like any other
            skip_next = token in ("--tests", "-P", "-D")
            continue
        # not placeholders like :<module>:test, file paths or URLs in the prose after a command
        if TASK_PATH.fullmatch(token):
            yield token


def is_broken(path, target):
    if re.match(r"^([a-z]+:|#|\{)", target):  # web links, anchors, templates
        return False
    file_part = unquote(target.split("#")[0].split("?")[0])
    base = ROOT if file_part.startswith("/") else (ROOT / path).parent
    return bool(file_part) and not (base / file_part.lstrip("/")).exists()


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
