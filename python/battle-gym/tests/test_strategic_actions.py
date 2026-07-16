from __future__ import annotations

import dataclasses

import pytest

from triplea_battle_gym.models import BattleObservation
from triplea_battle_gym.strategic_actions import expand_strategic_actions
from triplea_battle_gym.strategic_models import (
    ReinforcementObservation,
    StrategicAction,
    StrategicObservation,
)

NO_DECISION = {
    "type": "NONE",
    "player": "",
    "message": "",
    "requiredHits": 0,
    "allowMultipleHitsPerUnit": False,
    "candidates": [],
    "territories": [],
    "defaultKilledUnitIds": [],
    "defaultDamagedUnitIds": [],
}


def battle_dict(decision: dict) -> dict:
    return {
        "schemaVersion": 4,
        "seed": 7,
        "battleId": "battle-1",
        "territory": "Bastogne",
        "round": 1,
        "maxRounds": 3,
        "over": False,
        "amphibious": False,
        "headless": True,
        "offensePlayer": "Germans",
        "defensePlayer": "Americans",
        "offense": [],
        "defense": [],
        "attackerRetreatTerritories": ["Wiltz"],
        "decision": decision,
    }


def observation_with(decision: dict) -> StrategicObservation:
    return StrategicObservation(
        schema_version=2,
        seed=1,
        round=1,
        player="Germans",
        sequence_step="GermanBattle",
        phase="BATTLE",
        decision_domain="BATTLE",
        territories=(),
        reinforcements=ReinforcementObservation.from_dict(
            {
                "schemaVersion": 1,
                "player": "Germans",
                "currentRound": 1,
                "lastProcessedRound": 1,
                "pending": [],
                "scheduled": [],
            }
        ),
        pending_battles=(),
        battle=BattleObservation.from_dict(battle_dict(decision)),
        over=False,
    )


def unit(unit_id: str) -> dict:
    return {
        "unitId": unit_id,
        "owner": "Germans",
        "unitType": "infantry",
        "hits": 0,
        "hitPoints": 1,
        "alreadyMoved": 0,
    }


def test_non_battle_actions_pass_through_untouched() -> None:
    move = StrategicAction("move", {"origin": "Alpha", "destination": "Bravo"})

    assert expand_strategic_actions(observation_with(NO_DECISION), (move,), max_actions=8) == (
        move,
    )


def test_casualty_descriptor_expands_into_concrete_choices() -> None:
    observation = observation_with(
        {
            "type": "SELECT_CASUALTIES",
            "player": "Germans",
            "message": "",
            "requiredHits": 1,
            "allowMultipleHitsPerUnit": False,
            "candidates": [unit("u1"), unit("u2")],
            "territories": [],
            "defaultKilledUnitIds": ["u1"],
            "defaultDamagedUnitIds": [],
        }
    )
    descriptor = StrategicAction(
        "battle_decision",
        {
            "battleActionType": "select_casualties",
            "battleId": "battle-1",
            "battleTerritory": "Bastogne",
            "candidateUnitIds": "u1,u2",
            "requiredHits": "1",
        },
    )

    expanded = expand_strategic_actions(observation, (descriptor,), max_actions=8)

    # One concrete choice per candidate, and the descriptor's own bookkeeping keys are gone.
    assert {action.parameters["killedUnitIds"] for action in expanded} == {"u1", "u2"}
    for action in expanded:
        assert action.type == "battle_decision"
        assert action.parameters["battleId"] == "battle-1"
        assert action.parameters["battleTerritory"] == "Bastogne"
        assert "candidateUnitIds" not in action.parameters


def test_retreat_keeps_its_destination_separate_from_the_battle_location() -> None:
    """The battle is in Bastogne; the retreat goes to Wiltz. Collapsing those loses the retreat."""
    observation = observation_with(
        {
            "type": "RETREAT",
            "player": "Germans",
            "message": "",
            "requiredHits": 0,
            "allowMultipleHitsPerUnit": False,
            "candidates": [],
            "territories": ["Wiltz"],
            "defaultKilledUnitIds": [],
            "defaultDamagedUnitIds": [],
        }
    )
    descriptor = StrategicAction(
        "battle_decision",
        {
            "battleActionType": "retreat",
            "battleId": "battle-1",
            "battleTerritory": "Bastogne",
            "territory": "Wiltz",
        },
    )

    expanded = expand_strategic_actions(observation, (descriptor,), max_actions=8)

    assert len(expanded) == 1
    assert expanded[0].parameters["territory"] == "Wiltz"
    assert expanded[0].parameters["battleTerritory"] == "Bastogne"


def test_battle_decision_without_a_battle_observation_is_rejected() -> None:
    without_battle = dataclasses.replace(observation_with(NO_DECISION), battle=None)
    descriptor = StrategicAction(
        "battle_decision",
        {"battleActionType": "continue", "battleId": "b", "battleTerritory": "t"},
    )

    with pytest.raises(ValueError, match="no battle observation"):
        expand_strategic_actions(without_battle, (descriptor,), max_actions=8)
