"""Single-learner training wrapper and deterministic opponent for strategic RL."""

from __future__ import annotations

from collections.abc import Callable, Sequence
from typing import Any

import gymnasium as gym
import numpy as np

from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicAction, StrategicObservation, TerritoryState

OpponentPolicy = Callable[[StrategicObservation, Sequence[StrategicAction]], int]


class SingleSideStrategicEnv(gym.Env[dict[str, np.ndarray], int]):
    """Expose one learner side while a fixed policy controls the opposing side.

    The underlying Java environment still runs a complete self-play game. This wrapper returns only
    learner decisions to PPO. After a learner action ends its turn, opponent decisions are executed
    internally until the learner acts again. Immediate zero-sum rewards from opponent actions are
    sign-flipped and accumulated into the learner transition.

    Small Front scenarios are two-sided. Encountering more than one distinct opponent is rejected
    instead of applying an invalid reward transformation to a multi-player game.
    """

    metadata = {"render_modes": ["ansi"]}

    def __init__(
        self,
        env: TripleAStrategicEnv,
        *,
        learner_player: str,
        opponent_policy: OpponentPolicy | None = None,
        max_automatic_steps: int = 4096,
    ) -> None:
        super().__init__()
        if not learner_player:
            raise ValueError("learner_player must not be empty")
        if max_automatic_steps < 1:
            raise ValueError("max_automatic_steps must be positive")
        self._env = env
        self._learner_player = learner_player
        self._opponent_policy = opponent_policy or scripted_opponent_action_index
        self._max_automatic_steps = max_automatic_steps
        self._opponents_seen: set[str] = set()
        self.action_space = env.action_space
        self.observation_space = env.observation_space

    @property
    def raw_observation(self) -> StrategicObservation:
        return self._env.raw_observation

    @property
    def legal_actions(self) -> tuple[StrategicAction, ...]:
        return self._env.legal_actions

    def action_masks(self) -> np.ndarray:
        return self._env.action_masks()

    def reset(
        self,
        *,
        seed: int | None = None,
        options: dict[str, Any] | None = None,
    ) -> tuple[dict[str, np.ndarray], dict[str, Any]]:
        observation, info = self._env.reset(seed=seed, options=options)
        self._opponents_seen.clear()
        warmup_steps = 0
        while self.raw_observation.player != self._learner_player:
            if self.raw_observation.over:
                raise RuntimeError(
                    f"game ended before learner player {self._learner_player!r} received a turn"
                )
            self._register_opponent(self.raw_observation.player)
            action = self._opponent_policy(self.raw_observation, self.legal_actions)
            observation, _, terminated, truncated, info = self._env.step(action)
            warmup_steps += 1
            if terminated or truncated:
                raise RuntimeError(
                    f"game ended before learner player {self._learner_player!r} received a turn"
                )
            if warmup_steps >= self._max_automatic_steps:
                raise RuntimeError("opponent warmup exceeded max_automatic_steps")
        result_info = dict(info)
        result_info["learnerPlayer"] = self._learner_player
        result_info["opponentWarmupSteps"] = warmup_steps
        return observation, result_info

    def step(
        self,
        action: int,
    ) -> tuple[dict[str, np.ndarray], float, bool, bool, dict[str, Any]]:
        if self.raw_observation.player != self._learner_player:
            raise RuntimeError(
                f"expected learner {self._learner_player}, got {self.raw_observation.player}"
            )

        observation, reward, terminated, truncated, learner_info = self._env.step(action)
        total_reward = float(reward)
        opponent_steps = 0
        opponent_actions: list[dict[str, Any]] = []

        while (
            not terminated and not truncated and self.raw_observation.player != self._learner_player
        ):
            opponent = self.raw_observation.player
            self._register_opponent(opponent)
            opponent_action = self._opponent_policy(self.raw_observation, self.legal_actions)
            selected = self.legal_actions[opponent_action]
            observation, opponent_reward, terminated, truncated, _ = self._env.step(opponent_action)
            # The Java reward is from the actor's perspective. In a two-sided zero-sum score margin,
            # the learner sees the opposite sign for an opponent transition.
            total_reward -= float(opponent_reward)
            opponent_steps += 1
            opponent_actions.append(
                {
                    "player": opponent,
                    "action": selected.to_dict(),
                    "rewardFromOpponentPerspective": float(opponent_reward),
                }
            )
            if opponent_steps >= self._max_automatic_steps:
                raise RuntimeError("opponent rollout exceeded max_automatic_steps")

        info = dict(learner_info)
        info["learnerPlayer"] = self._learner_player
        info["opponentSteps"] = opponent_steps
        info["opponentActions"] = opponent_actions
        return observation, total_reward, terminated, truncated, info

    def render(self) -> str:
        return self._env.render()

    def close(self) -> None:
        self._env.close()

    def _register_opponent(self, player: str) -> None:
        if player == self._learner_player:
            return
        self._opponents_seen.add(player)
        if len(self._opponents_seen) > 1:
            raise RuntimeError(
                "SingleSideStrategicEnv supports exactly two active players; "
                f"observed opponents: {sorted(self._opponents_seen)}"
            )


def scripted_opponent_action_index(
    observation: StrategicObservation,
    actions: Sequence[StrategicAction],
) -> int:
    """Choose a deterministic, supply-aware baseline action from the current legal mask."""

    if not actions:
        raise ValueError("opponent has no legal actions")

    end_phase = next(
        (index for index, action in enumerate(actions) if action.type == "end_phase"),
        None,
    )
    best_index = end_phase if end_phase is not None else 0
    best_score = 0 if end_phase is not None else -10_000
    territories = {territory.territory: territory for territory in observation.territories}

    for index, action in enumerate(actions):
        if action.type == "end_phase":
            continue
        score = _opponent_action_score(action, observation, territories)
        if score > best_score or (score == best_score and index < best_index):
            best_index = index
            best_score = score
    return best_index


def _opponent_action_score(
    action: StrategicAction,
    observation: StrategicObservation,
    territories: dict[str, TerritoryState],
) -> int:
    if action.type == "battle_decision":
        return 100

    parameters = action.parameters
    origin = territories.get(parameters.get("origin", ""))
    destination = territories.get(parameters.get("destination", ""))
    score = 10

    if action.type == "allocate_reinforcement":
        score += 20
    if action.type == "air_assignment":
        score += 20
        if destination is not None and destination.visible:
            own_control = (
                destination.air_control_status or ""
            ).upper() == "CONTROLLED" and destination.air_control_player == observation.player
            score += -10 if own_control else 40
    elif destination is not None and destination.visible:
        if destination.owner not in {None, observation.player}:
            score += 50
        elif destination.owner == observation.player:
            score += 5
        if destination.supplied is False:
            score -= 80

    if destination is not None and destination.visible and destination.supply_source:
        score += 15
    if parameters.get("uncertain") == "true":
        score -= 25
    if (
        origin is not None
        and origin.visible
        and origin.supply_source
        and _empties_matching_friendly_units(action, observation, origin)
    ):
        score -= 120
    return score


def _empties_matching_friendly_units(
    action: StrategicAction,
    observation: StrategicObservation,
    origin: TerritoryState,
) -> bool:
    unit_type = action.parameters.get("unitType", "")
    available = sum(
        group.count
        for group in origin.units
        if group.owner == observation.player and (not unit_type or group.unit_type == unit_type)
    )
    return available > 0 and _moving_count(action) >= available


def _moving_count(action: StrategicAction) -> int:
    unit_ids = action.parameters.get("unitIds", "")
    if unit_ids:
        return len([item for item in unit_ids.split(",") if item])
    try:
        return int(action.parameters.get("unitCount", "1"))
    except ValueError:
        return 1
