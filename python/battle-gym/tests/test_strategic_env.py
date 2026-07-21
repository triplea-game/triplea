from __future__ import annotations

from typing import Any

import numpy as np
import pytest

from triplea_battle_gym.strategic_encoding import StrategicObservationEncoder
from triplea_battle_gym.strategic_env import TripleAStrategicEnv
from triplea_battle_gym.strategic_models import (
    StrategicAction,
    StrategicObservation,
    StrategicResetRequest,
    StrategicStepResult,
)


def make_env(
    server_command: tuple[str, ...],
    *,
    episode_seed_stride: int = 0,
) -> TripleAStrategicEnv:
    return TripleAStrategicEnv(
        server_command=server_command,
        reset_request=StrategicResetRequest("fixture.tsvg", 11, "Blue"),
        max_territories=4,
        max_actions=8,
        episode_seed_stride=episode_seed_stride,
    )


def test_gym_reset_mask_step_and_render(server_command: tuple[str, ...]) -> None:
    env = make_env(server_command)
    try:
        observation, info = env.reset(seed=11)

        assert observation["state"].dtype == np.float32
        assert observation["action_features"].shape == (
            8,
            StrategicObservationEncoder.ACTION_FEATURES,
        )
        assert observation["action_mask"].tolist() == [1, 1, 0, 0, 0, 0, 0, 0]
        assert info["seed"] == 11
        assert info["player"] == "Blue"
        assert info["phase"] == "COMBAT_MOVE"
        assert "1/2 territories visible" in env.render()

        # The first action is end_phase; the second is an uncertain Alpha -> Bravo move.
        end_phase = observation["action_features"][0]
        move = observation["action_features"][1]
        assert end_phase[0] == 1.0
        assert end_phase[23] == 1.0
        assert move[2] == 1.0
        assert move[6] == pytest.approx(1 / 4)
        assert move[7] == pytest.approx(2 / 4)
        assert move[11] == 1.0
        assert move[12] == 0.0

        next_observation, reward, terminated, truncated, step_info = env.step(0)

        assert reward == 2.0
        assert terminated
        assert not truncated
        assert next_observation["action_mask"].sum() == 0
        assert step_info["selectedAction"]["type"] == "end_phase"
        assert step_info["stepId"] == "1"
    finally:
        env.close()


def test_masked_action_is_rejected(server_command: tuple[str, ...]) -> None:
    env = make_env(server_command)
    try:
        env.reset()
        with pytest.raises(ValueError, match="masked out"):
            env.step(5)
    finally:
        env.close()


def test_fogged_territory_encodes_as_unknown_not_as_empty(
    server_command: tuple[str, ...],
) -> None:
    """The fog is pointless if an agent can tell an empty territory from a hidden one."""
    env = make_env(server_command)
    try:
        env.reset()
        encoder = StrategicObservationEncoder(max_territories=4, max_actions=8)
        state = env._encoded()["state"]
        stride = encoder.TERRITORY_FEATURES
        base = encoder.GLOBAL_FEATURES
        # Territories are slotted by name: Alpha is visible, Bravo is fogged.
        alpha = state[base : base + stride]
        bravo = state[base + stride : base + 2 * stride]

        assert alpha[1] == 1.0  # visible
        assert bravo[1] == 0.0
        # Owner, supply and unit counts are all sentinel on the fogged cell rather than zero.
        for index in (3, 4, 5, 6, 7, 8, 9):
            assert bravo[index] == encoder.UNKNOWN, f"feature {index} leaked through the fog"
        # Alpha reports its two visible friendly units; the map graph stays public on both.
        assert alpha[8] == 2.0
        assert bravo[10] == 1.0
    finally:
        env.close()


def test_implicit_resets_cycle_episode_seeds() -> None:
    client = RecordingStrategicClient()
    env = TripleAStrategicEnv(
        client=client,  # type: ignore[arg-type]
        reset_request=StrategicResetRequest("fixture.tsvg", 10, "Blue"),
        max_territories=4,
        max_actions=8,
        episode_seed_stride=3,
    )
    try:
        _, first = env.reset()
        _, second = env.reset()
        _, explicit = env.reset(seed=99)
        _, third = env.reset()

        assert [first["seed"], second["seed"], explicit["seed"], third["seed"]] == [
            10,
            13,
            99,
            16,
        ]
        assert [request.seed for request in client.requests] == [10, 13, 99, 16]
    finally:
        env.close()


def test_reset_request_rejects_empty_player() -> None:
    with pytest.raises(ValueError, match="player"):
        StrategicResetRequest("fixture.tsvg", 1, "")


class RecordingStrategicClient:
    def __init__(self) -> None:
        self.requests: list[StrategicResetRequest] = []
        self._observation = _recording_observation(seed=0, over=False)

    def strategic_reset(self, request: StrategicResetRequest) -> StrategicObservation:
        self.requests.append(request)
        self._observation = _recording_observation(seed=request.seed, over=False)
        return self._observation

    def strategic_legal_actions(self) -> tuple[StrategicAction, ...]:
        return (StrategicAction("end_phase", {"phase": "COMBAT_MOVE"}),)

    def strategic_step(self, action: StrategicAction) -> StrategicStepResult:
        self._observation = _recording_observation(seed=self._observation.seed, over=True)
        return StrategicStepResult(self._observation, 0.0, True, False, {})

    def close(self) -> None:
        pass


def _recording_observation(*, seed: int, over: bool) -> StrategicObservation:
    value: dict[str, Any] = {
        "schemaVersion": 2,
        "seed": seed,
        "round": 1,
        "player": "Blue",
        "sequenceStep": "BlueCombatMove",
        "phase": "COMPLETE" if over else "COMBAT_MOVE",
        "decisionDomain": "COMPLETE" if over else "STRATEGIC",
        "territories": [],
        "reinforcements": {
            "schemaVersion": 1,
            "player": "Blue",
            "currentRound": 1,
            "lastProcessedRound": 1,
            "pending": [],
            "scheduled": [],
        },
        "pendingBattles": [],
        "battle": None,
        "over": over,
    }
    return StrategicObservation.from_dict(value)
