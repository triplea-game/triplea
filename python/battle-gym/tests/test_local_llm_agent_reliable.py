from __future__ import annotations

from types import SimpleNamespace

from triplea_battle_gym.local_llm_agent_reliable import (
    EXECUTION_TOOLS,
    _initial_decision_message,
    _public_reason,
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


def test_terminal_tools_require_public_reason() -> None:
    for tool in EXECUTION_TOOLS:
        parameters = tool["function"]["parameters"]

        assert "reason" in parameters["required"]
        assert parameters["properties"]["reason"]["type"] == "string"


def test_initial_message_includes_a_legal_action_shortlist() -> None:
    message = _initial_decision_message(4, FakeCatalog())  # type: ignore[arg-type]

    assert '"decision": 4' in message
    assert '"actionId": 0' in message
    assert "execute exactly one legal action" in message
    assert "printed in CMD" in message


def test_repair_message_requires_a_tool_call_and_reason() -> None:
    message = _repair_decision_message(FakeCatalog(), 1)  # type: ignore[arg-type]

    assert "must now call execute_action" in message
    assert '"actionId": 2' in message
    assert "concise Korean reason" in message
    assert "Return no prose-only" in message


def test_public_reason_uses_model_summary() -> None:
    assert _public_reason({"reason": "보급선을 유지하기 위해 Bitburg로 이동합니다."}) == (
        "보급선을 유지하기 위해 Bitburg로 이동합니다."
    )


def test_public_reason_has_missing_reason_fallback() -> None:
    assert "공개 설명을 제공하지 않았습니다" in _public_reason({})
