from __future__ import annotations

from types import SimpleNamespace

from triplea_battle_gym.local_llm_agent_reliable import (
    EXECUTION_TOOLS,
    _initial_decision_message,
    _repair_decision_message,
)


class FakeCatalog:
    def __init__(self) -> None:
        self.observation = SimpleNamespace(round=1, player="Germans", phase="COMBAT_MOVE")
        self.actions = (object(), object(), object())

    def list_actions(self, *, limit: int) -> list[dict[str, object]]:
        assert limit > 0
        return [
            {"actionId": 0, "type": "move"},
            {"actionId": 2, "type": "end_phase"},
        ]


def test_execution_repair_exposes_only_terminal_decision_tools() -> None:
    names = {str(tool["function"]["name"]) for tool in EXECUTION_TOOLS}

    assert names == {"execute_action", "end_phase"}


def test_initial_message_includes_a_legal_action_shortlist() -> None:
    message = _initial_decision_message(4, FakeCatalog())  # type: ignore[arg-type]

    assert '"decision": 4' in message
    assert '"actionId": 0' in message
    assert "execute exactly one legal action" in message


def test_repair_message_requires_a_tool_call() -> None:
    message = _repair_decision_message(FakeCatalog(), 1)  # type: ignore[arg-type]

    assert "must now call execute_action" in message
    assert '"actionId": 2' in message
    assert "Return no prose-only answer" in message
