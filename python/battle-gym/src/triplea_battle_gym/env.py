"""Gymnasium environment backed by one TripleA server process."""

from __future__ import annotations

from collections.abc import Mapping, Sequence
from dataclasses import replace
from pathlib import Path
from typing import Any

import gymnasium as gym
import numpy as np
from gymnasium import spaces

from .actions import expand_legal_actions
from .client import BattleClient
from .encoding import ObservationEncoder
from .models import BattleAction, BattleObservation, BattleResetRequest


class TripleABattleEnv(gym.Env[dict[str, np.ndarray], int]):
    """Fixed-space adapter over TripleA's parameterized battle decisions."""

    metadata = {"render_modes": ["ansi"]}

    def __init__(
        self,
        *,
        server_command: Sequence[str] | None = None,
        reset_request: BattleResetRequest,
        client: BattleClient | None = None,
        cwd: str | Path | None = None,
        process_env: Mapping[str, str] | None = None,
        max_unit_groups_per_side: int = 64,
        max_actions: int = 4096,
    ) -> None:
        super().__init__()
        if client is None and not server_command:
            raise ValueError("server_command is required when client is not supplied")
        self._client = client or BattleClient(server_command or (), cwd=cwd, env=process_env)
        self._owns_client = client is None
        self._base_reset_request = reset_request
        self._max_actions = max_actions
        self._encoder = ObservationEncoder(
            max_unit_groups_per_side=max_unit_groups_per_side,
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
                "action_mask": spaces.Box(
                    low=0,
                    high=1,
                    shape=(max_actions,),
                    dtype=np.int8,
                ),
            }
        )
        self._observation: BattleObservation | None = None
        self._actions: tuple[BattleAction, ...] = ()

    @property
    def raw_observation(self) -> BattleObservation:
        if self._observation is None:
            raise RuntimeError("reset must be called before reading the observation")
        return self._observation

    @property
    def legal_actions(self) -> tuple[BattleAction, ...]:
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
        if options and "reset_request" in options:
            supplied = options["reset_request"]
            if not isinstance(supplied, BattleResetRequest):
                raise TypeError("options['reset_request'] must be a BattleResetRequest")
            request = supplied
        self._observation = self._client.reset(request)
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
        result = self._client.step(selected)
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
        return (
            f"{observation.territory}: round {observation.round}/{observation.max_rounds}, "
            f"offense={sum(item.count for item in observation.offense)}, "
            f"defense={sum(item.count for item in observation.defense)}, "
            f"decision={observation.decision.type}"
        )

    def close(self) -> None:
        if self._owns_client:
            self._client.close()

    def _refresh_actions(self) -> None:
        if self.raw_observation.over:
            self._clear_actions()
            return
        descriptors = self._client.legal_actions()
        self._actions = expand_legal_actions(
            self.raw_observation,
            descriptors,
            max_actions=self._max_actions,
        )

    def _clear_actions(self) -> None:
        self._actions = ()

    def _encoded(self) -> dict[str, np.ndarray]:
        return self._encoder.encode(self.raw_observation, self._actions)

    def _info(self) -> dict[str, Any]:
        return {
            "battleId": self.raw_observation.battle_id,
            "territory": self.raw_observation.territory,
            "decisionType": self.raw_observation.decision.type,
            "legalActions": [action.to_dict() for action in self._actions],
        }
