from __future__ import annotations

import random

from triplea_battle_gym.models import BattleAction
from triplea_battle_gym.policies import random_action_index, scripted_action_index


def test_scripted_policy_declines_retreat() -> None:
    actions = (
        BattleAction("retreat", {"territory": "Rear"}),
        BattleAction("continue"),
    )

    assert scripted_action_index(actions) == 1


def test_random_policy_returns_valid_index() -> None:
    actions = (BattleAction("continue"), BattleAction("retreat", {"territory": "Rear"}))

    assert random_action_index(actions, random.Random(3)) in {0, 1}
