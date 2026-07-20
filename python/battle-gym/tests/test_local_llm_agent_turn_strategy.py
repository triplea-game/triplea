from __future__ import annotations

import json
from types import SimpleNamespace
from typing import Any

import pytest

from triplea_battle_gym.local_llm_agent import ActionCatalog, OllamaError, ReplayStep
from triplea_battle_gym.local_llm_agent_turn_strategy import (
    _choose_planned_action,
    _fallback_turn_strategy,
    _normalize_turn_strategy,
    _score_planned_action,
    _strategy_context_message,
    _turn_strategy_schema,
)
from triplea_battle_gym.strategic_models import StrategicAction


def _unit(
    *,
    owner: str,
    unit_type: str,
    count: int,
    land: bool,
    air: bool,
) -> SimpleNamespace:
    return SimpleNamespace(
        owner=owner,
        unit_type=unit_type,
        count=count,
        land=land,
        air=air,
        sea=False,
        minimum_movement_left="1",
        supplied=True,
        out_of_supply_turns=0,
        turns_until_removal=None,
    )


def _territory(
    *,
    name: str,
    owner: str | None,
    neighbors: tuple[str, ...],
    units: tuple[SimpleNamespace, ...] = (),
    supply_source: bool = False,
    supplied: bool | None = True,
    air_control_player: str | None = None,
    air_control_status: str | None = None,
) -> SimpleNamespace:
    return SimpleNamespace(
        territory=name,
        water=False,
        visible=True,
        owner=owner,
        supplied=supplied,
        supply_source=supply_source,
        air_control_player=air_control_player,
        air_control_status=air_control_status,
        air_control_persistent=False,
        neighbors=neighbors,
        road_connections=neighbors,
        units=units,
    )


def _observation() -> SimpleNamespace:
    infantry = _unit(
        owner="Germans",
        unit_type="infantry",
        count=2,
        land=True,
        air=False,
    )
    fighter = _unit(
        owner="Germans",
        unit_type="fighter",
        count=1,
        land=False,
        air=True,
    )
    enemy = _unit(
        owner="Americans",
        unit_type="infantry",
        count=2,
        land=True,
        air=False,
    )
    prum = _territory(
        name="Prum",
        owner="Germans",
        neighbors=("Losheim Gap", "Bitburg"),
        units=(infantry, fighter),
        supply_source=True,
        air_control_player="Germans",
        air_control_status="controlled",
    )
    losheim = _territory(
        name="Losheim Gap",
        owner="Germans",
        neighbors=("Prum", "St. Vith"),
        units=(),
    )
    st_vith = _territory(
        name="St. Vith",
        owner="Americans",
        neighbors=("Losheim Gap",),
        units=(enemy,),
        air_control_player="Americans",
        air_control_status="controlled",
    )
    bitburg = _territory(
        name="Bitburg",
        owner="Germans",
        neighbors=("Prum",),
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
        territories=(prum, losheim, st_vith, bitburg),
        reinforcements=reinforcements,
        pending_battles=(),
        over=False,
    )


def _plan(*, target: str = "St. Vith", protected: tuple[str, ...] = ("Prum",)) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "planId": "germans-r2-st-vith",
        "playerName": "Germans",
        "round": 2,
        "commanderIntent": "주공 축에 전력을 집중하고 보급 거점의 예비대를 유지합니다.",
        "objectives": [
            {
                "objectiveId": "gain-air-st-vith",
                "type": "GAIN_AIR_SUPERIORITY",
                "territoryName": target,
                "priority": 100,
                "prerequisiteObjectiveIds": [],
            },
            {
                "objectiveId": "capture-st-vith",
                "type": "CAPTURE",
                "territoryName": target,
                "priority": 95,
                "prerequisiteObjectiveIds": ["gain-air-st-vith"],
            },
        ],
        "protectedTerritories": list(protected),
        "maximumReplans": 1,
    }


def _action(
    action_type: str,
    *,
    origin: str,
    destination: str,
    unit_type: str,
    unit_count: int = 1,
) -> StrategicAction:
    return StrategicAction(
        action_type,
        {
            "origin": origin,
            "destination": destination,
            "route": f"{origin}>{destination}",
            "unitType": unit_type,
            "unitCount": str(unit_count),
            "movementLeft": "1",
            "uncertain": "false",
        },
    )


def _end_phase() -> StrategicAction:
    return StrategicAction("end_phase", {"phase": "COMBAT_MOVE"})


def test_turn_strategy_schema_requires_machine_readable_plan_fields() -> None:
    required = set(_turn_strategy_schema()["required"])

    assert required == {
        "schemaVersion",
        "planId",
        "playerName",
        "round",
        "commanderIntent",
        "objectives",
        "protectedTerritories",
        "maximumReplans",
    }


def test_action_context_contains_current_state_plan_and_completed_actions() -> None:
    catalog = ActionCatalog(_observation(), (_end_phase(),))
    steps = [
        ReplayStep(
            action_type="move",
            fields=(("origin", "Prum"), ("destination", "St. Vith")),
        )
    ]

    message = _strategy_context_message(
        catalog=catalog,
        strategy=_plan(),
        completed_steps=steps,
    )
    payload = json.loads(message.split("\n", maxsplit=1)[1])

    assert payload["currentSituation"]["player"] == "Germans"
    assert payload["currentTurnOperationalPlan"]["planId"] == "germans-r2-st-vith"
    assert payload["completedActionsThisTurn"][0]["parameters"]["origin"] == "Prum"
    assert "deterministic executor" in message


def test_fallback_plan_pairs_air_and_ground_objectives_and_protects_supply() -> None:
    plan = _fallback_turn_strategy(_observation())  # type: ignore[arg-type]

    assert plan["protectedTerritories"] == ["Prum"]
    assert plan["objectives"][0]["type"] == "GAIN_AIR_SUPERIORITY"
    assert plan["objectives"][0]["territoryName"] == "St. Vith"
    capture = next(
        objective for objective in plan["objectives"] if objective["type"] == "CAPTURE"
    )
    assert capture["territoryName"] == "St. Vith"
    assert capture["prerequisiteObjectiveIds"] == ["gain-air-st-vith"]
    assert any(
        objective["type"] == "PROTECT_SUPPLY" and objective["territoryName"] == "Prum"
        for objective in plan["objectives"]
    )


def test_normalize_plan_uses_authoritative_player_and_rejects_missing_dependency() -> None:
    value = _plan()
    value["playerName"] = "Wrong Side"
    value["round"] = 99
    value["objectives"][1]["prerequisiteObjectiveIds"] = ["missing-air-objective"]
    situation = {
        "round": 2,
        "player": "Germans",
        "territories": [{"name": name} for name in ("Prum", "St. Vith")],
    }

    with pytest.raises(OllamaError, match="unknown prerequisite objective"):
        _normalize_turn_strategy(value, current_situation=situation)


def test_plan_redirects_air_assignment_to_selected_objective() -> None:
    observation = _observation()
    st_vith = _action(
        "air_assignment",
        origin="Prum",
        destination="St. Vith",
        unit_type="fighter",
    )
    rear = _action(
        "air_assignment",
        origin="Prum",
        destination="Bitburg",
        unit_type="fighter",
    )
    catalog = ActionCatalog(observation, (rear, st_vith, _end_phase()))

    choice = _choose_planned_action(
        catalog=catalog,
        strategy=_plan(),
        completed_steps=(),
    )

    assert choice.action_id == 1
    assert choice.score > 0
    assert choice.aligned is True


def test_immediate_reversal_subtracts_one_hundred_points() -> None:
    observation = _observation()
    reverse = _action(
        "move",
        origin="Losheim Gap",
        destination="Prum",
        unit_type="infantry",
    )
    catalog = ActionCatalog(observation, (reverse, _end_phase()))
    completed = [
        ReplayStep(
            action_type="move",
            fields=(
                ("origin", "Prum"),
                ("destination", "Losheim Gap"),
                ("unitType", "infantry"),
            ),
        )
    ]

    without_history = _score_planned_action(
        action_id=0,
        action=reverse,
        catalog=catalog,
        strategy=_plan(protected=()),
        completed_steps=(),
    )
    with_history = _score_planned_action(
        action_id=0,
        action=reverse,
        catalog=catalog,
        strategy=_plan(protected=()),
        completed_steps=completed,
    )

    assert with_history.score == without_history.score - 120
    assert "직전 이동 역전 -100" in with_history.reasons
    assert "활성 목표 비정합 -20" in with_history.reasons


def test_plan_penalizes_emptying_a_protected_supply_source() -> None:
    observation = _observation()
    partial = _action(
        "move",
        origin="Prum",
        destination="Losheim Gap",
        unit_type="infantry",
        unit_count=1,
    )
    full = _action(
        "move",
        origin="Prum",
        destination="Losheim Gap",
        unit_type="infantry",
        unit_count=2,
    )
    catalog = ActionCatalog(observation, (partial, full, _end_phase()))

    partial_score = _score_planned_action(
        action_id=0,
        action=partial,
        catalog=catalog,
        strategy=_plan(),
        completed_steps=(),
    )
    full_score = _score_planned_action(
        action_id=1,
        action=full,
        catalog=catalog,
        strategy=_plan(),
        completed_steps=(),
    )

    assert partial_score.score == full_score.score + 120
    assert "보호 지역의 지상 예비대 소진 -120" in full_score.reasons
