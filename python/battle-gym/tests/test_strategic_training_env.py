from __future__ import annotations

from typing import Any

import numpy as np
from gymnasium import spaces

from triplea_battle_gym.strategic_models import StrategicAction, StrategicObservation
from triplea_battle_gym.strategic_training_env import (
    SingleSideStrategicEnv,
    scripted_opponent_action_index,
)


def test_single_side_wrapper_accumulates_opponent_rewards() -> None:
    base = AlternatingStrategicEnv()
    env = SingleSideStrategicEnv(base, learner_player="Blue")  # type: ignore[arg-type]

    observation, info = env.reset()
    assert info["learnerPlayer"] == "Blue"
    assert base.raw_observation.player == "Blue"

    next_observation, reward, terminated, truncated, step_info = env.step(0)

    assert observation["state"][0] == 0.0
    assert next_observation["state"][0] == 2.0
    assert reward == 0.5  # Blue +1.0, then Red +0.5 is -0.5 from Blue's perspective.
    assert not terminated
    assert not truncated
    assert step_info["opponentSteps"] == 1
    assert step_info["opponentActions"][0]["player"] == "Red"
    assert base.raw_observation.player == "Blue"


def test_scripted_opponent_preserves_supply_reserve_and_attacks_enemy() -> None:
    observation = StrategicObservation.from_dict(
        {
            "schemaVersion": 2,
            "seed": 1,
            "round": 1,
            "player": "Red",
            "sequenceStep": "RedCombatMove",
            "phase": "COMBAT_MOVE",
            "decisionDomain": "STRATEGIC",
            "territories": [
                _territory(
                    "Alpha",
                    owner="Red",
                    supply_source=True,
                    units=[_unit("Red", "infantry", 2)],
                ),
                _territory("Bravo", owner="Red"),
                _territory("Charlie", owner="Blue"),
            ],
            "reinforcements": _reinforcements("Red"),
            "pendingBattles": [],
            "battle": None,
            "over": False,
        }
    )
    actions = (
        StrategicAction("end_phase", {"phase": "COMBAT_MOVE"}),
        StrategicAction(
            "move",
            {
                "origin": "Alpha",
                "destination": "Bravo",
                "unitType": "infantry",
                "unitIds": "u1,u2",
            },
        ),
        StrategicAction(
            "move",
            {
                "origin": "Alpha",
                "destination": "Charlie",
                "unitType": "infantry",
                "unitIds": "u1",
            },
        ),
    )

    assert scripted_opponent_action_index(observation, actions) == 2


class AlternatingStrategicEnv:
    def __init__(self) -> None:
        self.action_space = spaces.Discrete(2)
        self.observation_space = spaces.Dict(
            {
                "state": spaces.Box(-np.inf, np.inf, shape=(1,), dtype=np.float32),
                "action_features": spaces.Box(
                    -np.inf, np.inf, shape=(2, 1), dtype=np.float32
                ),
                "action_mask": spaces.Box(0, 1, shape=(2,), dtype=np.int8),
            }
        )
        self._state = 0
        self._observation = _observation("Blue", step=0)

    @property
    def raw_observation(self) -> StrategicObservation:
        return self._observation

    @property
    def legal_actions(self) -> tuple[StrategicAction, ...]:
        if self._observation.player == "Blue":
            return (StrategicAction("end_phase", {"phase": "COMBAT_MOVE"}),)
        return (
            StrategicAction(
                "move",
                {"origin": "R1", "destination": "R2", "unitType": "infantry"},
            ),
        )

    def action_masks(self) -> np.ndarray:
        return np.asarray([True, False], dtype=np.bool_)

    def reset(
        self,
        *,
        seed: int | None = None,
        options: dict[str, Any] | None = None,
    ) -> tuple[dict[str, np.ndarray], dict[str, Any]]:
        self._state = 0
        self._observation = _observation("Blue", step=0)
        return self._encoded(), {"player": "Blue"}

    def step(
        self, action: int
    ) -> tuple[dict[str, np.ndarray], float, bool, bool, dict[str, Any]]:
        assert action == 0
        if self._state == 0:
            self._state = 1
            self._observation = _observation("Red", step=1)
            return self._encoded(), 1.0, False, False, {"selectedAction": {}}
        if self._state == 1:
            self._state = 2
            self._observation = _observation("Blue", step=2)
            return self._encoded(), 0.5, False, False, {"selectedAction": {}}
        raise AssertionError("unexpected step")

    def render(self) -> str:
        return self._observation.player

    def close(self) -> None:
        pass

    def _encoded(self) -> dict[str, np.ndarray]:
        return {
            "state": np.asarray([float(self._state)], dtype=np.float32),
            "action_features": np.zeros((2, 1), dtype=np.float32),
            "action_mask": np.asarray([1, 0], dtype=np.int8),
        }


def _observation(player: str, *, step: int) -> StrategicObservation:
    return StrategicObservation.from_dict(
        {
            "schemaVersion": 2,
            "seed": 1,
            "round": step // 2 + 1,
            "player": player,
            "sequenceStep": f"{player}CombatMove",
            "phase": "COMBAT_MOVE",
            "decisionDomain": "STRATEGIC",
            "territories": [],
            "reinforcements": _reinforcements(player),
            "pendingBattles": [],
            "battle": None,
            "over": False,
        }
    )


def _territory(
    name: str,
    *,
    owner: str,
    supply_source: bool = False,
    units: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    return {
        "territory": name,
        "water": False,
        "visible": True,
        "owner": owner,
        "supplied": True,
        "supplySource": supply_source,
        "airControlPlayer": None,
        "airControlStatus": "UNCONTROLLED",
        "airControlPersistent": False,
        "neighbors": [],
        "roadConnections": [],
        "units": units or [],
    }


def _unit(owner: str, unit_type: str, count: int) -> dict[str, Any]:
    return {
        "owner": owner,
        "unitType": unit_type,
        "count": count,
        "land": True,
        "air": False,
        "sea": False,
        "minimumMovementLeft": "1",
        "supplied": True,
        "outOfSupplyTurns": 0,
        "turnsUntilRemoval": None,
    }


def _reinforcements(player: str) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "player": player,
        "currentRound": 1,
        "lastProcessedRound": 1,
        "pending": [],
        "scheduled": [],
    }
