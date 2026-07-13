from __future__ import annotations

import numpy as np

from triplea_battle_gym.env import TripleABattleEnv
from triplea_battle_gym.models import BattleResetRequest


def test_gym_reset_mask_step_and_render(server_command: tuple[str, ...]) -> None:
    env = TripleABattleEnv(
        server_command=server_command,
        reset_request=BattleResetRequest("fixture.tsvg", 7),
        max_unit_groups_per_side=4,
        max_actions=8,
    )
    try:
        observation, info = env.reset(seed=11)

        assert observation["state"].dtype == np.float32
        assert observation["action_mask"].tolist() == [1, 1, 0, 0, 0, 0, 0, 0]
        assert info["decisionType"] == "RETREAT"
        assert env.action_masks().tolist() == [True, True, False, False, False, False, False, False]
        assert "Test Territory" in env.render()

        next_observation, reward, terminated, truncated, step_info = env.step(0)
        assert reward == 1.25
        assert terminated
        assert not truncated
        assert next_observation["action_mask"].sum() == 0
        assert step_info["selectedAction"]["type"] == "continue"
    finally:
        env.close()
