"""Typed protocol models for TripleA's turn-level strategic simulation.

Field names mirror the Java records the server serialises with Gson, so they are camelCase on the
wire and snake_case here.
"""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass, field
from typing import Any

from .models import JsonObject, _mapping, _sequence, _string


@dataclass(frozen=True, slots=True)
class StrategicResetRequest:
    """Restores one player's turn from a TripleA save and fixes its RNG seed."""

    scenario_path: str
    seed: int
    player: str
    max_actions: int = 512

    def __post_init__(self) -> None:
        if not self.scenario_path:
            raise ValueError("scenario_path must not be empty")
        if not self.player:
            raise ValueError("player must not be empty")
        if self.max_actions < 1:
            raise ValueError("max_actions must be positive")

    def to_dict(self) -> JsonObject:
        return {
            "scenarioPath": self.scenario_path,
            "seed": self.seed,
            "player": self.player,
            "maxActions": self.max_actions,
        }


@dataclass(frozen=True, slots=True)
class StrategicAction:
    """One turn-level action. The server rejects anything outside the current legal mask."""

    type: str
    parameters: Mapping[str, str] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if not self.type:
            raise ValueError("action type must not be empty")
        normalized = {str(key): str(value) for key, value in self.parameters.items()}
        object.__setattr__(self, "parameters", dict(sorted(normalized.items())))

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> StrategicAction:
        parameters = _mapping(value.get("parameters", {}), "parameters")
        return cls(
            type=_string(value.get("type"), "type"),
            parameters={str(key): str(item) for key, item in parameters.items()},
        )

    def to_dict(self) -> JsonObject:
        return {"type": self.type, "parameters": dict(self.parameters)}


@dataclass(frozen=True, slots=True)
class StrategicUnitGroup:
    owner: str
    unit_type: str
    count: int
    land: bool
    air: bool
    sea: bool
    minimum_movement_left: str
    supplied: bool | None
    out_of_supply_turns: int | None
    turns_until_removal: int | None

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> StrategicUnitGroup:
        return cls(
            owner=_string(value.get("owner"), "owner"),
            unit_type=_string(value.get("unitType"), "unitType"),
            count=int(value.get("count", 0)),
            land=bool(value.get("land", False)),
            air=bool(value.get("air", False)),
            sea=bool(value.get("sea", False)),
            minimum_movement_left=_string(
                value.get("minimumMovementLeft", "0"), "minimumMovementLeft"
            ),
            supplied=_optional_bool(value.get("supplied")),
            out_of_supply_turns=_optional_int(value.get("outOfSupplyTurns")),
            turns_until_removal=_optional_int(value.get("turnsUntilRemoval")),
        )


@dataclass(frozen=True, slots=True)
class TerritoryState:
    """One territory. Everything past `visible` is withheld while it is fogged."""

    territory: str
    water: bool
    visible: bool
    owner: str | None
    supplied: bool | None
    supply_source: bool
    air_control_player: str | None
    air_control_status: str | None
    air_control_persistent: bool | None
    neighbors: tuple[str, ...]
    road_connections: tuple[str, ...]
    units: tuple[StrategicUnitGroup, ...]

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> TerritoryState:
        return cls(
            territory=_string(value.get("territory"), "territory"),
            water=bool(value.get("water", False)),
            visible=bool(value.get("visible", False)),
            owner=_optional_string(value.get("owner")),
            supplied=_optional_bool(value.get("supplied")),
            supply_source=bool(value.get("supplySource", False)),
            air_control_player=_optional_string(value.get("airControlPlayer")),
            air_control_status=_optional_string(value.get("airControlStatus")),
            air_control_persistent=_optional_bool(value.get("airControlPersistent")),
            neighbors=tuple(
                _string(item, "neighbors[]")
                for item in _sequence(value.get("neighbors", []), "neighbors")
            ),
            road_connections=tuple(
                _string(item, "roadConnections[]")
                for item in _sequence(value.get("roadConnections", []), "roadConnections")
            ),
            units=tuple(
                StrategicUnitGroup.from_dict(_mapping(item, "units[]"))
                for item in _sequence(value.get("units", []), "units")
            ),
        )


@dataclass(frozen=True, slots=True)
class ReinforcementEntry:
    round: int
    territory: str
    unit_type: str
    quantity: int

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> ReinforcementEntry:
        return cls(
            round=int(value.get("round", 0)),
            territory=_string(value.get("territory"), "territory"),
            unit_type=_string(value.get("unitType"), "unitType"),
            quantity=int(value.get("quantity", 0)),
        )


@dataclass(frozen=True, slots=True)
class ReinforcementObservation:
    schema_version: int
    player: str
    current_round: int
    last_processed_round: int
    pending: tuple[ReinforcementEntry, ...]
    scheduled: tuple[ReinforcementEntry, ...]

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> ReinforcementObservation:
        return cls(
            schema_version=int(value.get("schemaVersion", 0)),
            player=_string(value.get("player", ""), "player"),
            current_round=int(value.get("currentRound", 0)),
            last_processed_round=int(value.get("lastProcessedRound", 0)),
            pending=tuple(
                ReinforcementEntry.from_dict(_mapping(item, "pending[]"))
                for item in _sequence(value.get("pending", []), "pending")
            ),
            scheduled=tuple(
                ReinforcementEntry.from_dict(_mapping(item, "scheduled[]"))
                for item in _sequence(value.get("scheduled", []), "scheduled")
            ),
        )


@dataclass(frozen=True, slots=True)
class PendingBattle:
    battle_id: str
    territory: str
    battle_type: str

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> PendingBattle:
        return cls(
            battle_id=_string(value.get("battleId"), "battleId"),
            territory=_string(value.get("territory"), "territory"),
            battle_type=_string(value.get("battleType"), "battleType"),
        )


@dataclass(frozen=True, slots=True)
class StrategicObservation:
    """One turn-level observation, already filtered through fog of war by the server.

    There is deliberately no score here. A score counts territory the acting player may not be able
    to see, so carrying it would leak what the fog hides; the reward is computed server-side from
    the true state instead.
    """

    schema_version: int
    seed: int
    round: int
    player: str
    sequence_step: str
    phase: str
    decision_domain: str
    territories: tuple[TerritoryState, ...]
    reinforcements: ReinforcementObservation
    pending_battles: tuple[PendingBattle, ...]
    battle: Mapping[str, Any] | None
    over: bool

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> StrategicObservation:
        battle = value.get("battle")
        return cls(
            schema_version=int(value.get("schemaVersion", 0)),
            seed=int(value.get("seed", 0)),
            round=int(value.get("round", 0)),
            player=_string(value.get("player"), "player"),
            sequence_step=_string(value.get("sequenceStep", ""), "sequenceStep"),
            phase=_string(value.get("phase"), "phase"),
            decision_domain=_string(value.get("decisionDomain"), "decisionDomain"),
            territories=tuple(
                TerritoryState.from_dict(_mapping(item, "territories[]"))
                for item in _sequence(value.get("territories", []), "territories")
            ),
            reinforcements=ReinforcementObservation.from_dict(
                _mapping(value.get("reinforcements", {}), "reinforcements")
            ),
            pending_battles=tuple(
                PendingBattle.from_dict(_mapping(item, "pendingBattles[]"))
                for item in _sequence(value.get("pendingBattles", []), "pendingBattles")
            ),
            battle=None if battle is None else _mapping(battle, "battle"),
            over=bool(value.get("over", False)),
        )


@dataclass(frozen=True, slots=True)
class StrategicStepResult:
    observation: StrategicObservation
    reward: float
    terminated: bool
    truncated: bool
    info: Mapping[str, str]

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> StrategicStepResult:
        observation = _mapping(value.get("observation"), "observation")
        info = _mapping(value.get("info", {}), "info")
        return cls(
            observation=StrategicObservation.from_dict(observation),
            reward=float(value.get("reward", 0)),
            terminated=bool(value.get("terminated", False)),
            truncated=bool(value.get("truncated", False)),
            info={str(key): str(item) for key, item in info.items()},
        )


def _optional_string(value: object) -> str | None:
    return None if value is None else _string(value, "optional string")


def _optional_bool(value: object) -> bool | None:
    return None if value is None else bool(value)


def _optional_int(value: object) -> int | None:
    if value is None:
        return None
    if not isinstance(value, (int, float, str)):
        raise TypeError("optional int must be a number")
    return int(value)
