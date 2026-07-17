from __future__ import annotations

import json
from types import SimpleNamespace
from typing import Any

import pytest

from triplea_battle_gym.local_llm_agent import OllamaError, ReplayStep
from triplea_battle_gym.local_llm_agent_turn_strategy import (
    _fallback_turn_strategy,
    _normalize_turn_strategy,
    _strategy_context_message,
    _turn_strategy_schema,
)


def _observation() -> SimpleNamespace:
    unit = SimpleNamespace(
        owner="Germans",
        unit_type="infantry",
        count=2,
        land=True,
        air=False,
        sea=False,
        minimum_movement_left="1",
        supplied=True,
        out_of_supply_turns=0,
    )
    friendly = SimpleNamespace(
        territory="Prum",
        water=False,
        visible=True,
        owner="Germans",
        supplied=True,
        supply_source=True,
        air_control_player="Germans",
        air_control_status="controlled",
        neighbors=("Bitburg",),
        road_connections=("Bitburg",),
        units=(unit,),
    )
    enemy = SimpleNamespace(
        territory="St. Vith",
        water=False,
        visible=True,
        owner="Americans",
        supplied=True,
        supply_source=False,
        air_control_player="Americans",
        air_control_status="controlled",
        neighbors=("Losheim Gap",),
        road_connections=("Losheim Gap",),
        units=(),
    )
    reinforcements = SimpleNamespace(pending=(), scheduled=())
    return SimpleNamespace(
        schema_version=1,
        round=2,
        player="Germans",
        sequence_step="germanCombatMove",
        phase="COMBAT_MOVE",
        decision_domain="STRATEGIC",
        territories=(friendly, enemy),
        reinforcements=reinforcements,
        pending_battles=(),
        over=False,
    )


def _strategy() -> dict[str, Any]:
    return {
        "situationAssessment": "적 전선에 압력을 가하되 보급 거점의 예비대를 유지해야 합니다.",
        "turnObjective": "주공 축에 전력을 집중하고 불필요한 왕복 이동을 피합니다.",
        "priorityAxes": ["St. Vith 축"],
        "attackTargets": ["St. Vith"],
        "defensiveReserves": ["Prum"],
        "operationalRules": ["보급 유지", "왕복 이동 금지"],
        "endPhaseCriteria": ["추가 이동이 주공에 기여하지 않을 때"],
    }


def test_turn_strategy_schema_requires_command_fields() -> None:
    required = set(_turn_strategy_schema()["required"])

    assert required == {
        "situationAssessment",
        "turnObjective",
        "priorityAxes",
        "attackTargets",
        "defensiveReserves",
        "operationalRules",
        "endPhaseCriteria",
    }


def test_action_context_contains_current_state_strategy_and_completed_actions() -> None:
    catalog = SimpleNamespace(observation=_observation(), actions=(object(), object()))
    steps = [
        ReplayStep(
            action_type="move",
            fields=(("origin", "Prum"), ("destination", "St. Vith")),
        )
    ]

    message = _strategy_context_message(
        catalog=catalog,  # type: ignore[arg-type]
        strategy=_strategy(),
        completed_steps=steps,
    )
    payload = json.loads(message.split("\n", maxsplit=1)[1])

    assert payload["currentSituation"]["player"] == "Germans"
    assert payload["currentTurnGrandStrategy"]["attackTargets"] == ["St. Vith"]
    assert payload["completedActionsThisTurn"][0]["parameters"]["origin"] == "Prum"
    assert "only authoritative board state" in message


def test_fallback_strategy_uses_visible_enemy_and_supply_source() -> None:
    strategy = _fallback_turn_strategy(_observation())  # type: ignore[arg-type]

    assert strategy["attackTargets"] == ["St. Vith"]
    assert strategy["defensiveReserves"] == ["Prum"]
    assert "왕복 이동" in strategy["turnObjective"]


def test_normalize_turn_strategy_rejects_missing_required_array() -> None:
    value = _strategy()
    value["priorityAxes"] = []

    with pytest.raises(OllamaError, match="priorityAxes"):
        _normalize_turn_strategy(value)
