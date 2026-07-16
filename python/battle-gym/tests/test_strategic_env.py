from __future__ import annotations

import numpy as np
import pytest

from triplea_battle_gym.strategic_encoding import StrategicObservationEncoder
from triplea_battle_gym.strategic_env import TripleAStrategicEnv
from triplea_battle_gym.strategic_models import StrategicResetRequest


def make_env(server_command: tuple[str, ...]) -> TripleAStrategicEnv:
    return TripleAStrategicEnv(
        server_command=server_command,
        reset_request=StrategicResetRequest("fixture.tsvg", 11, "Blue"),
        max_territories=4,
        max_actions=8,
    )


def test_gym_reset_mask_step_and_render(server_command: tuple[str, ...]) -> None:
    env = make_env(server_command)
    try:
        observation, info = env.reset(seed=11)

        assert observation["state"].dtype == np.float32
        assert observation["action_mask"].tolist() == [1, 1, 0, 0, 0, 0, 0, 0]
        assert info["player"] == "Blue"
        assert info["phase"] == "COMBAT_MOVE"
        assert "1/2 territories visible" in env.render()

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


def test_fogged_territory_encodes_as_unknown_not_as_empty(server_command: tuple[str, ...]) -> None:
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


def test_reset_request_rejects_empty_player() -> None:
    with pytest.raises(ValueError, match="player"):
        StrategicResetRequest("fixture.tsvg", 1, "")
