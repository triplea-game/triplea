"""Typed protocol models for the TripleA battle simulation server."""

from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass, field
from typing import Any, TypeAlias

JsonObject: TypeAlias = dict[str, Any]


def _mapping(value: object, field_name: str) -> Mapping[str, Any]:
    if not isinstance(value, Mapping):
        raise TypeError(f"{field_name} must be an object")
    return value


def _sequence(value: object, field_name: str) -> list[Any]:
    if not isinstance(value, list):
        raise TypeError(f"{field_name} must be an array")
    return value


def _string(value: object, field_name: str) -> str:
    if not isinstance(value, str):
        raise TypeError(f"{field_name} must be a string")
    return value


@dataclass(frozen=True, slots=True)
class BattleAction:
    """One concrete engine action or one server-provided action descriptor."""

    type: str
    parameters: Mapping[str, str] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if not self.type:
            raise ValueError("action type must not be empty")
        normalized = {str(key): str(value) for key, value in self.parameters.items()}
        object.__setattr__(self, "parameters", dict(sorted(normalized.items())))

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> BattleAction:
        parameters = _mapping(value.get("parameters", {}), "parameters")
        return cls(
            type=_string(value.get("type"), "type"),
            parameters={str(key): str(item) for key, item in parameters.items()},
        )

    def to_dict(self) -> JsonObject:
        return {"type": self.type, "parameters": dict(self.parameters)}


@dataclass(frozen=True, slots=True)
class BattleResetRequest:
    """Selects one pending battle from a TripleA save and fixes its RNG seed."""

    scenario_path: str
    seed: int
    battle_id: str | None = None
    territory: str | None = None

    def __post_init__(self) -> None:
        if not self.scenario_path:
            raise ValueError("scenario_path must not be empty")

    def to_dict(self) -> JsonObject:
        value: JsonObject = {"scenarioPath": self.scenario_path, "seed": self.seed}
        if self.battle_id:
            value["battleId"] = self.battle_id
        if self.territory:
            value["territory"] = self.territory
        return value


@dataclass(frozen=True, slots=True)
class UnitGroupObservation:
    owner: str
    unit_type: str
    hits: int
    already_moved: float
    count: int

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> UnitGroupObservation:
        return cls(
            owner=_string(value.get("owner"), "owner"),
            unit_type=_string(value.get("unitType"), "unitType"),
            hits=int(value.get("hits", 0)),
            already_moved=float(value.get("alreadyMoved", 0)),
            count=int(value.get("count", 0)),
        )


@dataclass(frozen=True, slots=True)
class DecisionUnitObservation:
    unit_id: str
    owner: str
    unit_type: str
    hits: int
    hit_points: int
    already_moved: float

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> DecisionUnitObservation:
        return cls(
            unit_id=_string(value.get("unitId"), "unitId"),
            owner=_string(value.get("owner"), "owner"),
            unit_type=_string(value.get("unitType"), "unitType"),
            hits=int(value.get("hits", 0)),
            hit_points=int(value.get("hitPoints", 1)),
            already_moved=float(value.get("alreadyMoved", 0)),
        )


@dataclass(frozen=True, slots=True)
class BattleDecisionObservation:
    type: str
    player: str
    message: str
    required_hits: int
    allow_multiple_hits_per_unit: bool
    candidates: tuple[DecisionUnitObservation, ...]
    territories: tuple[str, ...]
    default_killed_unit_ids: tuple[str, ...]
    default_damaged_unit_ids: tuple[str, ...]

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> BattleDecisionObservation:
        return cls(
            type=_string(value.get("type", "NONE"), "decision.type"),
            player=_string(value.get("player", ""), "decision.player"),
            message=_string(value.get("message", ""), "decision.message"),
            required_hits=int(value.get("requiredHits", 0)),
            allow_multiple_hits_per_unit=bool(value.get("allowMultipleHitsPerUnit", False)),
            candidates=tuple(
                DecisionUnitObservation.from_dict(_mapping(item, "decision.candidates[]"))
                for item in _sequence(value.get("candidates", []), "decision.candidates")
            ),
            territories=tuple(
                _string(item, "decision.territories[]")
                for item in _sequence(value.get("territories", []), "decision.territories")
            ),
            default_killed_unit_ids=tuple(
                _string(item, "decision.defaultKilledUnitIds[]")
                for item in _sequence(
                    value.get("defaultKilledUnitIds", []), "decision.defaultKilledUnitIds"
                )
            ),
            default_damaged_unit_ids=tuple(
                _string(item, "decision.defaultDamagedUnitIds[]")
                for item in _sequence(
                    value.get("defaultDamagedUnitIds", []), "decision.defaultDamagedUnitIds"
                )
            ),
        )


@dataclass(frozen=True, slots=True)
class BattleObservation:
    schema_version: int
    seed: int
    battle_id: str
    territory: str
    round: int
    max_rounds: int
    over: bool
    amphibious: bool
    headless: bool
    offense_player: str
    defense_player: str
    offense: tuple[UnitGroupObservation, ...]
    defense: tuple[UnitGroupObservation, ...]
    attacker_retreat_territories: tuple[str, ...]
    decision: BattleDecisionObservation
    air_control_player: str = ""
    offense_ground_attack_bonus: int = 0

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> BattleObservation:
        decision = _mapping(value.get("decision", {}), "decision")
        return cls(
            schema_version=int(value.get("schemaVersion", 0)),
            seed=int(value.get("seed", 0)),
            battle_id=_string(value.get("battleId"), "battleId"),
            territory=_string(value.get("territory"), "territory"),
            round=int(value.get("round", 0)),
            max_rounds=int(value.get("maxRounds", 0)),
            over=bool(value.get("over", False)),
            amphibious=bool(value.get("amphibious", False)),
            headless=bool(value.get("headless", False)),
            offense_player=_string(value.get("offensePlayer"), "offensePlayer"),
            defense_player=_string(value.get("defensePlayer"), "defensePlayer"),
            offense=tuple(
                UnitGroupObservation.from_dict(_mapping(item, "offense[]"))
                for item in _sequence(value.get("offense", []), "offense")
            ),
            defense=tuple(
                UnitGroupObservation.from_dict(_mapping(item, "defense[]"))
                for item in _sequence(value.get("defense", []), "defense")
            ),
            attacker_retreat_territories=tuple(
                _string(item, "attackerRetreatTerritories[]")
                for item in _sequence(
                    value.get("attackerRetreatTerritories", []), "attackerRetreatTerritories"
                )
            ),
            decision=BattleDecisionObservation.from_dict(decision),
            air_control_player=_string(value.get("airControlPlayer", ""), "airControlPlayer"),
            offense_ground_attack_bonus=int(value.get("offenseGroundAttackBonus", 0)),
        )


@dataclass(frozen=True, slots=True)
class BattleStepResult:
    observation: BattleObservation
    reward: float
    terminated: bool
    truncated: bool
    info: Mapping[str, str]

    @classmethod
    def from_dict(cls, value: Mapping[str, Any]) -> BattleStepResult:
        observation = _mapping(value.get("observation"), "observation")
        info = _mapping(value.get("info", {}), "info")
        return cls(
            observation=BattleObservation.from_dict(observation),
            reward=float(value.get("reward", 0)),
            terminated=bool(value.get("terminated", False)),
            truncated=bool(value.get("truncated", False)),
            info={str(key): str(item) for key, item in info.items()},
        )
