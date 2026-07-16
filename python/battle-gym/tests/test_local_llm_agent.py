from triplea_battle_gym.local_llm_agent import ReplayStep, _parse_tool_call
from triplea_battle_gym.strategic_models import StrategicAction


def test_move_replay_ignores_random_unit_ids() -> None:
    original = StrategicAction(
        "move",
        {
            "origin": "Prum",
            "destination": "Houffalize",
            "route": "Prum>Clervaux>Houffalize",
            "unitType": "mechanized",
            "unitCount": "1",
            "movementLeft": "2",
            "unitIds": "11111111-1111-1111-1111-111111111111",
            "uncertain": "false",
        },
    )
    shadow = StrategicAction(
        "move",
        {
            "origin": "Prum",
            "destination": "Houffalize",
            "route": "Prum>Clervaux>Houffalize",
            "unitType": "mechanized",
            "unitCount": "1",
            "movementLeft": "2",
            "unitIds": "22222222-2222-2222-2222-222222222222",
            "uncertain": "false",
        },
    )

    assert ReplayStep.from_action(original).matches(shadow)


def test_reinforcement_replay_matches_equivalent_group_size() -> None:
    original = StrategicAction(
        "allocate_reinforcement",
        {
            "origin": "Prum",
            "destination": "Bitburg",
            "unitType": "infantry",
            "unitIds": "one,two",
        },
    )
    shadow = StrategicAction(
        "allocate_reinforcement",
        {
            "origin": "Prum",
            "destination": "Bitburg",
            "unitType": "infantry",
            "unitIds": "alpha,beta",
        },
    )

    assert ReplayStep.from_action(original).matches(shadow)


def test_default_battle_replay_ignores_casualty_ids() -> None:
    original = StrategicAction(
        "battle_decision",
        {
            "battleActionType": "select_casualties",
            "battleTerritory": "St. Vith",
            "battleId": "original",
            "killedUnitIds": "one",
            "damagedUnitIds": "",
        },
    )
    shadow = StrategicAction(
        "battle_decision",
        {
            "battleActionType": "select_casualties",
            "battleTerritory": "St. Vith",
            "battleId": "shadow",
            "killedUnitIds": "two",
            "damagedUnitIds": "",
        },
    )

    assert ReplayStep.from_action(original, default_battle=True).matches(shadow)


def test_parse_tool_call_accepts_json_string_arguments() -> None:
    name, arguments = _parse_tool_call(
        {
            "function": {
                "name": "inspect_action",
                "arguments": '{"action_id": 7}',
            }
        }
    )

    assert name == "inspect_action"
    assert arguments == {"action_id": 7}
