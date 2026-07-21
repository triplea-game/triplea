"""Gymnasium environment over TripleA's turn-level strategic decisions."""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from dataclasses import replace
from pathlib import Path
from typing import Any

import gymnasium as gym
import numpy as np
from gymnasium import spaces

from .client import BattleClient
from .strategic_actions import expand_strategic_actions
from .strategic_encoding import StrategicObservationEncoder
from .strategic_models import StrategicAction, StrategicObservation, StrategicResetRequest


class TripleAStrategicEnv(gym.Env[dict[str, np.ndarray], int]):
    """Fixed-space adapter over one strategic episode.

    With ``self_play=False`` the episode is one selected player's turn. With ``self_play=True`` the
    Java provider chains the complete game. Rewards are computed server-side from the true position;
    fog-filtered observations never expose hidden score state.

    Legal action indices are valid only for the current observation. ``action_features`` describes
    each current index and ``action_mask`` excludes unused padded slots.
    """

    metadata = {"render_modes": ["ansi"]}

    def __init__(
        self,
        *,
        server_command: Sequence[str] | None = None,
        reset_request: StrategicResetRequest,
        client: BattleClient | None = None,
        cwd: str | Path | None = None,
        process_env: Mapping[str, str] | None = None,
        max_territories: int = 64,
        max_actions: int = 512,
        episode_seed_stride: int = 0,
    ) -> None:
        super().__init__()
        if client is None and not server_command:
            raise ValueError("server_command is required when client is not supplied")
        if episode_seed_stride < 0:
            raise ValueError("episode_seed_stride must not be negative")
        self._client = client or BattleClient(server_command or (), cwd=cwd, env=process_env)
        self._owns_client = client is None
        self._base_reset_request = reset_request
        self._max_actions = max_actions
        self._episode_seed_stride = episode_seed_stride
        self._implicit_reset_count = 0
        self._encoder = StrategicObservationEncoder(
            max_territories=max_territories,
            max_actions=max_actions,
        )
        self.action_space: spaces.Discrete = spaces.Discrete(max_actions)
        self.observation_space = spaces.Dict(
            {
                "state": spaces.Box(
                    low=-np.inf,
                    high=np.inf,
                    shape=(self._encoder.state_size,),
                    dtype=np.float32,
                ),
                "action_features": spaces.Box(
                    low=-np.inf,
                    high=np.inf,
                    shape=(max_actions, self._encoder.ACTION_FEATURES),
                    dtype=np.float32,
                ),
                "action_mask": spaces.Box(
                    low=0,
                    high=1,
                    shape=(max_actions,),
                    dtype=np.int8,
                ),
            }
        )
        self._observation: StrategicObservation | None = None
        self._actions: tuple[StrategicAction, ...] = ()

    @property
    def raw_observation(self) -> StrategicObservation:
        if self._observation is None:
            raise RuntimeError("reset must be called before reading the observation")
        return self._observation

    @property
    def legal_actions(self) -> tuple[StrategicAction, ...]:
        return self._actions

    def action_masks(self) -> np.ndarray:
        mask = np.zeros(self._max_actions, dtype=np.bool_)
        mask[: len(self._actions)] = True
        return mask

    def reset(
        self,
        *,
        seed: int | None = None,
        options: dict[str, Any] | None = None,
    ) -> tuple[dict[str, np.ndarray], dict[str, Any]]:
        super().reset(seed=seed)
        request = self._base_reset_request
        if seed is not None:
            request = replace(request, seed=int(seed))
        elif self._episode_seed_stride > 0:
            request = replace(
                request,
                seed=request.seed + self._implicit_reset_count * self._episode_seed_stride,
            )
            self._implicit_reset_count += 1
        if options and "reset_request" in options:
            supplied = options["reset_request"]
            if not isinstance(supplied, StrategicResetRequest):
                raise TypeError("options['reset_request'] must be a StrategicResetRequest")
            request = supplied
        self._observation = self._client.strategic_reset(request)
        self._refresh_actions()
        return self._encoded(), self._info()

    def step(
        self,
        action: int,
    ) -> tuple[dict[str, np.ndarray], float, bool, bool, dict[str, Any]]:
        if self._observation is None:
            raise RuntimeError("reset must be called before step")
        if not self.action_space.contains(action):
            raise ValueError(f"action index is outside Discrete({self._max_actions}): {action}")
        if action >= len(self._actions):
            raise ValueError(f"action index {action} is masked out")
        selected = self._actions[action]
        result = self._client.strategic_step(selected)
        self._observation = result.observation
        if result.terminated or result.truncated:
            self._clear_actions()
        else:
            self._refresh_actions()
        info = self._info()
        info.update(result.info)
        info["selectedAction"] = selected.to_dict()
        return self._encoded(), result.reward, result.terminated, result.truncated, info

    def render(self) -> str:
        observation = self.raw_observation
        visible = sum(1 for territory in observation.territories if territory.visible)
        return (
            f"round {observation.round} {observation.player} {observation.phase}: "
            f"{visible}/{len(observation.territories)} territories visible, "
            f"{len(observation.pending_battles)} pending battles, "
            f"{len(self._actions)} legal actions"
        )

    def close(self) -> None:
        if self._owns_client:
            self._client.close()

    def _refresh_actions(self) -> None:
        if self.raw_observation.over:
            self._clear_actions()
            return
        self._actions = expand_strategic_actions(
            self.raw_observation,
            self._client.strategic_legal_actions(),
            max_actions=self._max_actions,
        )

    def _clear_actions(self) -> None:
        self._actions = ()

    def _encoded(self) -> dict[str, np.ndarray]:
        return self._encoder.encode(self.raw_observation, self._actions)

    def _info(self) -> dict[str, Any]:
        return {
            "seed": self.raw_observation.seed,
            "round": self.raw_observation.round,
            "player": self.raw_observation.player,
            "phase": self.raw_observation.phase,
            "decisionDomain": self.raw_observation.decision_domain,
            "legalActions": [action.to_dict() for action in self._actions],
        }
