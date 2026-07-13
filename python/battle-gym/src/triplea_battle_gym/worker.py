"""Process-level vector worker launcher."""

from __future__ import annotations

from collections.abc import Mapping
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np

from .env import TripleABattleEnv
from .models import BattleResetRequest


@dataclass(frozen=True, slots=True)
class BattleWorkerSpec:
    server_command: tuple[str, ...]
    reset_request: BattleResetRequest
    cwd: str | Path | None = None
    process_env: Mapping[str, str] | None = None
    max_unit_groups_per_side: int = 64
    max_actions: int = 4096


class BattleWorkerPool:
    """Runs independent Java battle servers concurrently and preserves worker order."""

    def __init__(self, specs: tuple[BattleWorkerSpec, ...]) -> None:
        if not specs:
            raise ValueError("at least one worker spec is required")
        self._envs = tuple(
            TripleABattleEnv(
                server_command=spec.server_command,
                reset_request=spec.reset_request,
                cwd=spec.cwd,
                process_env=spec.process_env,
                max_unit_groups_per_side=spec.max_unit_groups_per_side,
                max_actions=spec.max_actions,
            )
            for spec in specs
        )
        self._executor = ThreadPoolExecutor(max_workers=len(self._envs))
        self._closed = False

    def __enter__(self) -> BattleWorkerPool:
        return self

    def __exit__(self, *_: object) -> None:
        self.close()

    @property
    def worker_count(self) -> int:
        return len(self._envs)

    @property
    def legal_actions(self) -> tuple[tuple[Any, ...], ...]:
        return tuple(env.legal_actions for env in self._envs)

    def reset(
        self,
        seeds: tuple[int | None, ...] | None = None,
    ) -> tuple[tuple[dict[str, np.ndarray], dict[str, Any]], ...]:
        self._ensure_open()
        resolved = seeds or tuple(None for _ in self._envs)
        if len(resolved) != len(self._envs):
            raise ValueError("seed count must match worker count")
        futures = [
            self._executor.submit(env.reset, seed=seed)
            for env, seed in zip(self._envs, resolved, strict=True)
        ]
        return tuple(future.result() for future in futures)

    def step(
        self,
        actions: tuple[int, ...],
    ) -> tuple[tuple[dict[str, np.ndarray], float, bool, bool, dict[str, Any]], ...]:
        self._ensure_open()
        if len(actions) != len(self._envs):
            raise ValueError("action count must match worker count")
        futures = [
            self._executor.submit(env.step, action)
            for env, action in zip(self._envs, actions, strict=True)
        ]
        return tuple(future.result() for future in futures)

    def close(self) -> None:
        if self._closed:
            return
        self._closed = True
        for env in self._envs:
            env.close()
        self._executor.shutdown(wait=True, cancel_futures=True)

    def _ensure_open(self) -> None:
        if self._closed:
            raise RuntimeError("worker pool is closed")
