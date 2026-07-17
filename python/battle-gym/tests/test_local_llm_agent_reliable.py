from __future__ import annotations

from types import SimpleNamespace

from triplea_battle_gym.local_llm_agent_reliable import (
    EXECUTION_TOOLS,
    _balanced_shortlist,
    _final_decision_schema,
    _initial_decision_message,
    _public_reason,
    _repair_decision_message,
)


class FakeCatalog:
    def __init__(self) -> None:
        self.observation = SimpleNamespace(round=1, player="Germans", phase="COMBAT_MOVE")
        self.actions = (
            SimpleNamespace(type="move", parameters={"origin": "Prum"}),
            SimpleNamespace(type="move", parameters={"origin": "Prum"}),
            SimpleNamespace(type="move", parameters={"origin": "Bitburg"}),
            SimpleNamespace(type="end_phase", parameters={"phase": "COMBAT_MOVE"}),
        )

    def describe(self, action_id: int) -> dict[str, object]:
        action = self.actions[action_id]
        return {
            "actionId": action_id,
            "type": action.type,
            "parameters": dict(action.parameters),
        }



def test_execution_tools_require_public_reason() -> None:
    names = {str(tool["function"]["name"]) for tool in EXECUTION_TOOLS}

    assert names == {"execute_action", "end_phase"}
    for tool in EXECUTION_TOOLS:
        parameters = tool["function"]["parameters"]
        assert "reason" in parameters["required"]
        assert parameters["properties"]["reason"]["type"] == "string"



def test_balanced_shortlist_includes_phase_end_and_spreads_origins() -> None:
    shortlist = _balanced_shortlist(FakeCatalog(), 3)  # type: ignore[arg-type]

    assert [item["actionId"] for item in shortlist] == [3, 0, 2]



def test_final_schema_allows_only_shortlisted_action_ids() -> None:
    schema = _final_decision_schema([3, 0, 2])

    assert schema["required"] == ["action_id", "reason"]
    assert schema["properties"]["action_id"]["enum"] == [3, 0, 2]
    assert schema["additionalProperties"] is False



def test_initial_message_includes_balanced_legal_shortlist() -> None:
    message = _initial_decision_message(4, FakeCatalog())  # type: ignore[arg-type]

    assert '"decision": 4' in message
    assert '"actionId": 3' in message
    assert "schema-constrained final JSON choice" in message
    assert "concise Korean public reason" in message



def test_repair_message_requests_shortlisted_choice_and_reason() -> None:
    message = _repair_decision_message(FakeCatalog(), 1)  # type: ignore[arg-type]

    assert "Choose one actionId" in message
    assert '"actionId": 3' in message
    assert "concise Korean reason" in message
    assert "Return no prose-only" in message



def test_public_reason_uses_model_summary() -> None:
    assert _public_reason({"reason": "보급선을 유지하기 위해 Bitburg로 이동합니다."}) == (
        "보급선을 유지하기 위해 Bitburg로 이동합니다."
    )



def test_public_reason_has_missing_reason_fallback() -> None:
    assert "공개 설명을 제공하지 않았습니다" in _public_reason({})
