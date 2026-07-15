from __future__ import annotations

from triplea_battle_gym.models import BattleAction, BattleResetRequest


def test_action_parameters_are_canonicalized() -> None:
    action = BattleAction("retreat", {"z": "2", "a": "1"})

    assert list(action.parameters) == ["a", "z"]
    assert action.to_dict() == {
        "type": "retreat",
        "parameters": {"a": "1", "z": "2"},
    }


def test_reset_request_omits_empty_selectors() -> None:
    request = BattleResetRequest("game.tsvg", 9)

    assert request.to_dict() == {"scenarioPath": "game.tsvg", "seed": 9}
