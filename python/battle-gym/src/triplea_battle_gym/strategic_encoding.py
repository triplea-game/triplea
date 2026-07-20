"""Fixed-size numerical encoding of turn-level strategic observations and actions."""

from __future__ import annotations

from collections.abc import Mapping

import numpy as np

from .encoding import _stable_fraction
from .strategic_models import StrategicAction, StrategicObservation, TerritoryState

_PHASE_CODES = {
    "REINFORCEMENT_ALLOCATION": 0.0,
    "COMBAT_MOVE": 1.0,
    "AIR_ASSIGNMENT": 2.0,
    "BATTLE": 3.0,
    "REDEPLOYMENT": 4.0,
    "COMPLETE": 5.0,
}

_DOMAIN_CODES = {"STRATEGIC": 0.0, "BATTLE": 1.0, "COMPLETE": 2.0}

_ACTION_TYPE_INDEX = {
    "end_phase": 0,
    "allocate_reinforcement": 1,
    "move": 2,
    "air_assignment": 3,
    "battle_decision": 4,
}


class StrategicObservationEncoder:
    """Encodes the territory graph, semantic legal-action features and a padded mask.

    Territories occupy fixed slots ordered by name, so a given territory always lands in the same
    slot for one map. Fogged state uses the sentinel ``-1`` rather than zero.

    Action indices are ephemeral because the legal list changes after every step. The padded
    ``action_features`` matrix therefore describes the current meaning of each action index. A
    policy can distinguish moving armour toward a visible enemy from ending a phase instead of
    learning only a positional preference for action slot 17.
    """

    GLOBAL_FEATURES = 8
    TERRITORY_FEATURES = 12
    ACTION_FEATURES = 24
    UNKNOWN = -1.0

    def __init__(self, *, max_territories: int, max_actions: int) -> None:
        if max_territories <= 0:
            raise ValueError("max_territories must be positive")
        if max_actions <= 0:
            raise ValueError("max_actions must be positive")
        self.max_territories = max_territories
        self.max_actions = max_actions

    @property
    def state_size(self) -> int:
        return self.GLOBAL_FEATURES + self.TERRITORY_FEATURES * self.max_territories

    def encode(
        self,
        observation: StrategicObservation,
        legal_actions: tuple[StrategicAction, ...],
    ) -> dict[str, np.ndarray]:
        if len(legal_actions) > self.max_actions:
            raise ValueError("legal action count exceeds configured max_actions")
        if len(observation.territories) > self.max_territories:
            raise ValueError("territory count exceeds configured max_territories")

        ordered = sorted(observation.territories, key=lambda territory: territory.territory)
        territory_slots = {
            territory.territory: index + 1 for index, territory in enumerate(ordered)
        }
        territory_by_name = {territory.territory: territory for territory in ordered}

        state = np.zeros(self.state_size, dtype=np.float32)
        visible = [territory for territory in ordered if territory.visible]
        state[: self.GLOBAL_FEATURES] = np.asarray(
            [
                observation.round,
                _stable_fraction(observation.player),
                _PHASE_CODES.get(observation.phase, self.UNKNOWN),
                _DOMAIN_CODES.get(observation.decision_domain, self.UNKNOWN),
                float(observation.over),
                len(visible),
                len(observation.pending_battles),
                len(observation.reinforcements.pending),
            ],
            dtype=np.float32,
        )

        offset = self.GLOBAL_FEATURES
        for index, territory in enumerate(ordered):
            start = offset + index * self.TERRITORY_FEATURES
            state[start : start + self.TERRITORY_FEATURES] = self._territory_features(
                territory, observation.player
            )

        action_features = np.zeros((self.max_actions, self.ACTION_FEATURES), dtype=np.float32)
        for index, action in enumerate(legal_actions):
            action_features[index] = self._action_features(
                action=action,
                observation=observation,
                territory_slots=territory_slots,
                territories=territory_by_name,
            )

        mask = np.zeros(self.max_actions, dtype=np.int8)
        mask[: len(legal_actions)] = 1
        return {
            "state": state,
            "action_features": action_features,
            "action_mask": mask,
        }

    def _territory_features(self, territory: TerritoryState, viewer: str) -> tuple[float, ...]:
        if not territory.visible:
            # Only the identity and graph shape survive the fog; the map layout is public.
            return (
                _stable_fraction(territory.territory),
                0.0,
                float(territory.water),
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                float(len(territory.neighbors)),
                float(len(territory.road_connections)),
            )
        own = sum(group.count for group in territory.units if group.owner == viewer)
        other = sum(group.count for group in territory.units if group.owner != viewer)
        return (
            _stable_fraction(territory.territory),
            1.0,
            float(territory.water),
            _relative_owner(territory.owner, viewer),
            _tri_state(territory.supplied),
            float(territory.supply_source),
            _relative_air_control(territory, viewer),
            _tri_state(territory.air_control_persistent),
            float(own),
            float(other),
            float(len(territory.neighbors)),
            float(len(territory.road_connections)),
        )

    def _action_features(
        self,
        *,
        action: StrategicAction,
        observation: StrategicObservation,
        territory_slots: Mapping[str, int],
        territories: Mapping[str, TerritoryState],
    ) -> np.ndarray:
        values = np.zeros(self.ACTION_FEATURES, dtype=np.float32)
        action_type_index = _ACTION_TYPE_INDEX.get(action.type, 5)
        values[action_type_index] = 1.0

        parameters = action.parameters
        origin_name = parameters.get("origin", "")
        destination_name = parameters.get("destination", "")
        origin = territories.get(origin_name)
        destination = territories.get(destination_name)
        unit_type = parameters.get("unitType", "")
        moving_count = _action_unit_count(parameters)
        friendly_origin_count = _friendly_unit_count(
            origin, observation.player, unit_type=unit_type
        )

        values[6] = _slot_fraction(territory_slots.get(origin_name), self.max_territories)
        values[7] = _slot_fraction(territory_slots.get(destination_name), self.max_territories)
        values[8] = _stable_fraction(unit_type) if unit_type else 0.0
        values[9] = float(moving_count)
        values[10] = _number(parameters.get("movementLeft"))
        values[11] = float(parameters.get("uncertain") == "true")
        values[12] = _visibility(destination)
        values[13] = (
            _relative_owner(destination.owner, observation.player)
            if destination is not None and destination.visible
            else self.UNKNOWN
        )
        values[14] = (
            _tri_state(destination.supplied)
            if destination is not None and destination.visible
            else self.UNKNOWN
        )
        values[15] = (
            float(destination.supply_source)
            if destination is not None and destination.visible
            else self.UNKNOWN
        )
        values[16] = (
            _relative_air_control(destination, observation.player)
            if destination is not None and destination.visible
            else self.UNKNOWN
        )
        values[17] = (
            float(origin.supply_source) if origin is not None and origin.visible else self.UNKNOWN
        )
        values[18] = float(friendly_origin_count)
        values[19] = moving_count / friendly_origin_count if friendly_origin_count > 0 else 0.0
        battle_action_type = parameters.get("battleActionType", "")
        values[20] = _stable_fraction(battle_action_type) if battle_action_type else 0.0
        values[21] = float(_csv_count(parameters.get("killedUnitIds", "")))
        values[22] = float(_csv_count(parameters.get("damagedUnitIds", "")))
        values[23] = 1.0
        return values


def _relative_owner(owner: str | None, viewer: str) -> float:
    if owner is None:
        return 0.0
    return 1.0 if owner == viewer else -1.0


def _relative_air_control(territory: TerritoryState, viewer: str) -> float:
    status = (territory.air_control_status or "").upper()
    if status == "CONTESTED":
        return 0.0
    if status != "CONTROLLED":
        return 0.0
    if territory.air_control_player is None:
        return 0.0
    return 1.0 if territory.air_control_player == viewer else -1.0


def _tri_state(value: bool | None) -> float:
    if value is None:
        return StrategicObservationEncoder.UNKNOWN
    return 1.0 if value else 0.0


def _slot_fraction(slot: int | None, maximum: int) -> float:
    return 0.0 if slot is None else slot / maximum


def _visibility(territory: TerritoryState | None) -> float:
    if territory is None:
        return StrategicObservationEncoder.UNKNOWN
    return 1.0 if territory.visible else 0.0


def _action_unit_count(parameters: Mapping[str, str]) -> int:
    unit_ids = parameters.get("unitIds", "")
    if unit_ids:
        return _csv_count(unit_ids)
    return max(0, int(_number(parameters.get("unitCount"), default=1.0)))


def _friendly_unit_count(
    territory: TerritoryState | None,
    viewer: str,
    *,
    unit_type: str,
) -> int:
    if territory is None or not territory.visible:
        return 0
    return sum(
        group.count
        for group in territory.units
        if group.owner == viewer and (not unit_type or group.unit_type == unit_type)
    )


def _number(value: str | None, *, default: float = 0.0) -> float:
    if value is None or not value:
        return default
    try:
        return float(value)
    except ValueError:
        return default


def _csv_count(value: str) -> int:
    return len([item for item in value.split(",") if item])
