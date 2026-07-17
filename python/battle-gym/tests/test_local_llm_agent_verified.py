from __future__ import annotations

from types import SimpleNamespace

from triplea_battle_gym.local_llm_agent_verified import (
    _action_fact,
    _reason_matches_action,
    _safe_action_reason,
    _terminal_action_id,
)
from triplea_battle_gym.strategic_models import StrategicAction


class FakeCatalog:
    def __init__(self) -> None:
        self.observation = SimpleNamespace(player="Germans", phase="COMBAT_MOVE")
        self.actions = (
            StrategicAction(
                "move",
                {
                    "origin": "Vianden",
                    "destination": "Bastogne",
                    "unitType": "infantry",
                    "unitIds": "one,two",
                },
            ),
            StrategicAction("end_phase", {"phase": "COMBAT_MOVE"}),
        )

    def get(self, action_id: int) -> StrategicAction:
        return self.actions[action_id]

    def end_phase_id(self) -> int:
        return 1

    def describe(self, action_id: int) -> dict[str, object]:
        action = self.actions[action_id]
        result: dict[str, object] = {
            "actionId": action_id,
            "type": action.type,
            "parameters": dict(action.parameters),
        }
        if "unitIds" in action.parameters:
            result["unitCount"] = len(action.parameters["unitIds"].split(","))
        return result


def test_action_fact_is_derived_from_exact_engine_action() -> None:
    fact = _action_fact(FakeCatalog(), 0)  # type: ignore[arg-type]

    assert fact == "Germans의 infantry 2개를 Vianden에서 Bastogne로 이동합니다."


def test_reason_must_name_exact_origin_and_destination() -> None:
    action = FakeCatalog().describe(0)

    assert _reason_matches_action("Vianden의 병력을 Bastogne로 이동해 전선을 압박합니다.", action)
    assert not _reason_matches_action("Bitburg에서 Prum으로 이동합니다.", action)


def test_safe_reason_uses_exact_origin_and_destination() -> None:
    reason = _safe_action_reason(FakeCatalog(), 0)  # type: ignore[arg-type]

    assert "Vianden" in reason
    assert "Bastogne" in reason


def test_terminal_action_id_resolves_end_phase() -> None:
    assert _terminal_action_id("end_phase", {}, FakeCatalog()) == 1  # type: ignore[arg-type]


def test_terminal_action_id_validates_execute_action() -> None:
    assert _terminal_action_id(
        "execute_action", {"action_id": 0}, FakeCatalog()  # type: ignore[arg-type]
    ) == 0
