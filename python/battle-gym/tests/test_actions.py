from __future__ import annotations

import pytest

from triplea_battle_gym.actions import ActionSpaceOverflow, expand_legal_actions
from triplea_battle_gym.models import (
    BattleAction,
    BattleDecisionObservation,
    BattleObservation,
    DecisionUnitObservation,
)


def make_observation(*, multiple: bool = False) -> BattleObservation:
    candidates = tuple(
        DecisionUnitObservation(
            unit_id=f"unit-{index}",
            owner="defender",
            unit_type="infantry",
            hits=0,
            hit_points=2 if multiple else 1,
            already_moved=0,
        )
        for index in range(3)
    )
    return BattleObservation(
        schema_version=3,
        seed=1,
        battle_id="battle",
        territory="field",
        round=1,
        max_rounds=2,
        over=False,
        amphibious=False,
        headless=True,
        offense_player="attacker",
        defense_player="defender",
        offense=(),
        defense=(),
        attacker_retreat_territories=(),
        decision=BattleDecisionObservation(
            type="SELECT_CASUALTIES",
            player="defender",
            message="",
            required_hits=2,
            allow_multiple_hits_per_unit=multiple,
            candidates=candidates,
            territories=(),
            default_killed_unit_ids=("unit-0", "unit-1"),
            default_damaged_unit_ids=(),
        ),
    )


def test_expands_distinct_single_hit_casualty_combinations() -> None:
    actions = expand_legal_actions(
        make_observation(),
        (BattleAction("select_casualties", {"requiredHits": "2"}),),
        max_actions=10,
    )

    assert len(actions) == 3
    assert actions[0].parameters["killedUnitIds"] == "unit-0,unit-1"
    assert all(action.parameters["damagedUnitIds"] == "" for action in actions)


def test_multi_hit_expansion_includes_nonlethal_assignment() -> None:
    actions = expand_legal_actions(
        make_observation(multiple=True),
        (BattleAction("select_casualties"),),
        max_actions=20,
    )

    assert any(
        action.parameters["killedUnitIds"] == ""
        and action.parameters["damagedUnitIds"] == "unit-0,unit-1"
        for action in actions
    )


def test_overflow_is_explicit() -> None:
    with pytest.raises(ActionSpaceOverflow):
        expand_legal_actions(
            make_observation(),
            (BattleAction("select_casualties"),),
            max_actions=2,
        )
