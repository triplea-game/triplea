"""Look at and drive a desktop window (Windows only, needs Pillow): for the app-test skill.

    python .agents/scripts/desktop.py wait TripleA [--timeout 180]   # wait until the window exists
    python .agents/scripts/desktop.py windows                        # visible windows and their bounds
    python .agents/scripts/desktop.py shot [TripleA]                 # screenshot, prints the PNG path
    python .agents/scripts/desktop.py click X Y [--double|--right]   # X Y in the last screenshot
    python .agents/scripts/desktop.py type "some text"
    python .agents/scripts/desktop.py key enter | esc | tab | alt+f4 | ctrl+s ...
    python .agents/scripts/desktop.py scroll X Y -3                  # wheel clicks, negative = down

`shot` brings the window to the front, captures it, scales it down to at most 1280 px wide and
remembers origin and scale in build/app-test/last.json, so `click` takes coordinates straight
from the picture.
"""
import ctypes
import json
import sys
import time
from ctypes import wintypes
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build" / "app-test"
LAST = OUT / "last.json"
MAX_WIDTH = 1280

user32 = ctypes.windll.user32
ctypes.windll.shcore.SetProcessDpiAwareness(2)  # physical pixels, same as the screenshot

KEYS = {"enter": 0x0D, "esc": 0x1B, "tab": 0x09, "space": 0x20, "backspace": 0x08,
        "delete": 0x2E, "up": 0x26, "down": 0x28, "left": 0x25, "right": 0x27, "home": 0x24,
        "end": 0x23, "pageup": 0x21, "pagedown": 0x22, "shift": 0x10, "ctrl": 0x11, "alt": 0x12,
        **{f"f{i}": 0x6F + i for i in range(1, 13)}}


def windows():
    found = []

    @ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    def collect(hwnd, _):
        length = user32.GetWindowTextLengthW(hwnd)
        if user32.IsWindowVisible(hwnd) and length:
            title = ctypes.create_unicode_buffer(length + 1)
            user32.GetWindowTextW(hwnd, title, length + 1)
            rect = wintypes.RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            if rect.right - rect.left > 50 and rect.bottom - rect.top > 50:
                found.append((hwnd, title.value, (rect.left, rect.top, rect.right, rect.bottom)))
        return True

    user32.EnumWindows(collect, 0)
    return found


def find(title):
    matches = [w for w in windows() if title.lower() in w[1].lower()]
    return matches[0] if matches else None


def shot(title=None):
    from PIL import ImageGrab
    bbox = None
    if title:
        window = find(title)
        if not window:
            sys.exit(f"no visible window with '{title}' in its title, see `windows`")
        user32.SetForegroundWindow(window[0])
        time.sleep(0.4)
        bbox = window[2]
    image = ImageGrab.grab(bbox=bbox, all_screens=True)
    origin = bbox[:2] if bbox else (0, 0)
    scale = min(1.0, MAX_WIDTH / image.width)
    if scale < 1.0:
        image = image.resize((round(image.width * scale), round(image.height * scale)))
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / f"shot-{time.strftime('%H%M%S')}.png"
    image.save(path)
    LAST.write_text(json.dumps({"origin": origin, "scale": scale}))
    print(f"{path}  ({image.width}x{image.height}, click coordinates refer to this image)")


def to_screen(x, y):
    last = json.loads(LAST.read_text()) if LAST.exists() else {"origin": (0, 0), "scale": 1.0}
    return (round(last["origin"][0] + x / last["scale"]),
            round(last["origin"][1] + y / last["scale"]))


def click(x, y, flags):
    sx, sy = to_screen(x, y)
    user32.SetCursorPos(sx, sy)
    down, up = (0x0008, 0x0010) if "--right" in flags else (0x0002, 0x0004)
    for _ in range(2 if "--double" in flags else 1):
        user32.mouse_event(down, 0, 0, 0, 0)
        user32.mouse_event(up, 0, 0, 0, 0)
        time.sleep(0.05)


def scroll(x, y, clicks):
    user32.SetCursorPos(*to_screen(x, y))
    user32.mouse_event(0x0800, 0, 0, clicks * 120, 0)


def key(combo):
    codes = [KEYS.get(k) or ord(k.upper()) for k in combo.lower().split("+")]
    for code in codes:
        user32.keybd_event(code, 0, 0, 0)
    for code in reversed(codes):
        user32.keybd_event(code, 0, 0x0002, 0)


def type_text(text):
    for char in text:
        user32.keybd_event(0, ord(char), 0x0004, 0)          # KEYEVENTF_UNICODE
        user32.keybd_event(0, ord(char), 0x0004 | 0x0002, 0)
        time.sleep(0.01)


def wait(title, timeout):
    end = time.time() + timeout
    while time.time() < end:
        if find(title):
            print(f"window '{title}' is up")
            return
        time.sleep(2)
    sys.exit(f"no window '{title}' after {timeout}s")


def main(args):
    if not args:
        sys.exit(__doc__)
    sys.stdout.reconfigure(encoding="utf-8")  # window titles contain any character
    command, rest = args[0], args[1:]
    if command == "windows":
        for _, title, rect in windows():
            print(f"{rect}  {title}")
    elif command == "shot":
        shot(rest[0] if rest else None)
    elif command == "click":
        click(float(rest[0]), float(rest[1]), rest[2:])
    elif command == "scroll":
        scroll(float(rest[0]), float(rest[1]), int(rest[2]))
    elif command == "key":
        key(rest[0])
    elif command == "type":
        type_text(rest[0])
    elif command == "wait":
        timeout = int(rest[rest.index("--timeout") + 1]) if "--timeout" in rest else 180
        wait(rest[0], timeout)
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
