"""Synchronous typed client for TripleA's NDJSON battle server."""

from __future__ import annotations

import contextlib
import json
import os
import subprocess
import threading
from collections import deque
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from .models import (
    BattleAction,
    BattleObservation,
    BattleResetRequest,
    BattleStepResult,
    JsonObject,
)


class BattleProtocolError(RuntimeError):
    """The server emitted malformed or unexpected protocol data."""


class BattleServerError(RuntimeError):
    """The server returned a structured application error."""


class BattleClient:
    """Owns one server process and serializes request/response access."""

    def __init__(
        self,
        command: Sequence[str],
        *,
        cwd: str | Path | None = None,
        env: Mapping[str, str] | None = None,
        max_diagnostic_lines: int = 100,
    ) -> None:
        if not command:
            raise ValueError("server command must not be empty")
        process_env = os.environ.copy()
        if env:
            process_env.update(env)
        self._process = subprocess.Popen(
            tuple(command),
            cwd=None if cwd is None else str(cwd),
            env=process_env,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
            bufsize=1,
        )
        if (
            self._process.stdin is None
            or self._process.stdout is None
            or self._process.stderr is None
        ):
            raise RuntimeError("failed to open battle server pipes")
        self._stdin = self._process.stdin
        self._stdout = self._process.stdout
        self._stderr = self._process.stderr
        self._lock = threading.Lock()
        self._diagnostics: deque[str] = deque(maxlen=max_diagnostic_lines)
        self._stderr_thread = threading.Thread(target=self._drain_stderr, daemon=True)
        self._stderr_thread.start()
        self._closed = False

    def __enter__(self) -> BattleClient:
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    @property
    def diagnostics(self) -> tuple[str, ...]:
        return tuple(self._diagnostics)

    def ping(self) -> JsonObject:
        return self._request("ping", {}, "pong")

    def schema(self) -> JsonObject:
        return self._request("schema", {}, "schema")

    def reset(self, request: BattleResetRequest) -> BattleObservation:
        payload = self._request("reset", request.to_dict(), "observation")
        observation = _require_mapping(payload.get("observation"), "reset observation")
        return BattleObservation.from_dict(observation)

    def legal_actions(self) -> tuple[BattleAction, ...]:
        payload = self._request("legalActions", {}, "legalActions")
        if not isinstance(payload, list):
            raise BattleProtocolError("legalActions response data must be an array")
        return tuple(
            BattleAction.from_dict(_require_mapping(item, "legal action")) for item in payload
        )

    def step(self, action: BattleAction) -> BattleStepResult:
        payload = self._request("step", action.to_dict(), "step")
        return BattleStepResult.from_dict(payload)

    def episode_log(self) -> JsonObject:
        return self._request("episodeLog", {}, "episodeLog")

    def replay(self, episode_log: Mapping[str, Any]) -> JsonObject:
        return self._request("replay", dict(episode_log), "replay")

    def batch(self, episode_logs: Sequence[Mapping[str, Any]], parallelism: int = 1) -> JsonObject:
        return self._request(
            "batch",
            {"episodes": [dict(item) for item in episode_logs], "parallelism": parallelism},
            "batch",
        )

    def close(self) -> None:
        if self._closed:
            return
        self._closed = True
        with contextlib.suppress(OSError):
            self._stdin.close()
        if self._process.poll() is None:
            self._process.terminate()
            try:
                self._process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self._process.kill()
                self._process.wait(timeout=5)
        self._stderr_thread.join(timeout=1)

    def _request(self, command: str, data: Mapping[str, Any], expected_type: str) -> JsonObject:
        with self._lock:
            self._ensure_running()
            request = json.dumps(
                {"command": command, "data": dict(data)},
                separators=(",", ":"),
                ensure_ascii=False,
            )
            try:
                self._stdin.write(request + "\n")
                self._stdin.flush()
            except (BrokenPipeError, OSError) as error:
                message = self._failure_message("server input pipe closed")
                raise BattleProtocolError(message) from error

            for _ in range(100):
                line = self._stdout.readline()
                if line == "":
                    raise BattleProtocolError(self._failure_message("server output pipe closed"))
                stripped = line.strip()
                if not stripped:
                    continue
                try:
                    response = json.loads(stripped)
                except json.JSONDecodeError:
                    self._diagnostics.append(f"stdout: {stripped}")
                    continue
                if not isinstance(response, dict) or "ok" not in response:
                    self._diagnostics.append(f"stdout: {stripped}")
                    continue
                if not response.get("ok", False):
                    raise BattleServerError(str(response.get("error", "unknown server error")))
                response_type = response.get("type")
                if response_type != expected_type:
                    raise BattleProtocolError(
                        f"expected response type {expected_type!r}, got {response_type!r}"
                    )
                value = response.get("data")
                if not isinstance(value, dict):
                    if expected_type == "legalActions" and isinstance(value, list):
                        return value  # type: ignore[return-value]
                    raise BattleProtocolError("response data must be an object")
                return value
            raise BattleProtocolError("server emitted too many non-protocol stdout lines")

    def _ensure_running(self) -> None:
        if self._closed:
            raise BattleProtocolError("battle client is closed")
        return_code = self._process.poll()
        if return_code is not None:
            raise BattleProtocolError(
                self._failure_message(f"server exited with status {return_code}")
            )

    def _drain_stderr(self) -> None:
        for line in self._stderr:
            stripped = line.rstrip()
            if stripped:
                self._diagnostics.append(f"stderr: {stripped}")

    def _failure_message(self, message: str) -> str:
        if not self._diagnostics:
            return message
        return message + "\n" + "\n".join(self._diagnostics)


def _require_mapping(value: object, field_name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise BattleProtocolError(f"{field_name} must be an object")
    return value
