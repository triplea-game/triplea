"""Fixed-size numerical encoding of turn-level strategic observations."""

from __future__ import annotations

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

_AIR_CONTROL_CODES = {"UNCONTROLLED": 0.0, "CONTROLLED": 1.0, "CONTESTED": 2.0}


class StrategicObservationEncoder:
    """Encodes the territory graph and a padded legal-action mask.

    Territories occupy fixed slots ordered by name, so a given territory always lands in the same
    slot and the policy can learn positions. A fogged territory is not the same thing as an empty
    one: `visible` is its own feature and every field the server withholds under fog encodes as the
    sentinel -1 rather than 0, so "no owner known" cannot be confused with "owned by whoever hashes
    to zero" or "no units" with "units unseen".
    """

    GLOBAL_FEATURES = 8
    TERRITORY_FEATURES = 12
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

        state = np.zeros(self.state_size, dtype=np.float32)
        visible = [t for t in observation.territories if t.visible]
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

        ordered = sorted(observation.territories, key=lambda t: t.territory)
        offset = self.GLOBAL_FEATURES
        for index, territory in enumerate(ordered):
            start = offset + index * self.TERRITORY_FEATURES
            state[start : start + self.TERRITORY_FEATURES] = self._territory_features(
                territory, observation.player
            )

        mask = np.zeros(self.max_actions, dtype=np.int8)
        mask[: len(legal_actions)] = 1
        return {"state": state, "action_mask": mask}

    def _territory_features(self, t: TerritoryState, viewer: str) -> tuple[float, ...]:
        if not t.visible:
            # Only the identity and the graph shape survive the fog; the map layout is public.
            return (
                _stable_fraction(t.territory),
                0.0,
                float(t.water),
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                self.UNKNOWN,
                float(len(t.neighbors)),
                float(len(t.road_connections)),
            )
        own = sum(g.count for g in t.units if g.owner == viewer)
        other = sum(g.count for g in t.units if g.owner != viewer)
        return (
            _stable_fraction(t.territory),
            1.0,
            float(t.water),
            _stable_fraction(t.owner) if t.owner else self.UNKNOWN,
            _tri_state(t.supplied),
            float(t.supply_source),
            _AIR_CONTROL_CODES.get(t.air_control_status or "", self.UNKNOWN),
            _stable_fraction(t.air_control_player) if t.air_control_player else 0.0,
            float(own),
            float(other),
            float(len(t.neighbors)),
            float(len(t.road_connections)),
        )


def _tri_state(value: bool | None) -> float:
    if value is None:
        return StrategicObservationEncoder.UNKNOWN
    return 1.0 if value else 0.0
