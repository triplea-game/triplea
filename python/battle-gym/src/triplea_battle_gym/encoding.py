"""Fixed-size numerical encoding for Gymnasium policies."""

from __future__ import annotations

import hashlib

import numpy as np

from .models import BattleAction, BattleObservation, UnitGroupObservation

_DECISION_CODES = {
    "NONE": 0.0,
    "SELECT_CASUALTIES": 1.0,
    "RETREAT": 2.0,
    "SUBMERGE": 3.0,
}


class ObservationEncoder:
    """Encodes grouped battle state and a padded legal-action mask."""

    GLOBAL_FEATURES = 12
    GROUP_FEATURES = 6

    def __init__(self, *, max_unit_groups_per_side: int, max_actions: int) -> None:
        if max_unit_groups_per_side <= 0:
            raise ValueError("max_unit_groups_per_side must be positive")
        if max_actions <= 0:
            raise ValueError("max_actions must be positive")
        self.max_unit_groups_per_side = max_unit_groups_per_side
        self.max_actions = max_actions

    @property
    def state_size(self) -> int:
        return self.GLOBAL_FEATURES + (self.GROUP_FEATURES * self.max_unit_groups_per_side * 2)

    def encode(
        self,
        observation: BattleObservation,
        legal_actions: tuple[BattleAction, ...],
    ) -> dict[str, np.ndarray]:
        if len(legal_actions) > self.max_actions:
            raise ValueError("legal action count exceeds configured max_actions")
        if len(observation.offense) > self.max_unit_groups_per_side:
            raise ValueError("offense unit-group count exceeds configured maximum")
        if len(observation.defense) > self.max_unit_groups_per_side:
            raise ValueError("defense unit-group count exceeds configured maximum")

        state = np.zeros(self.state_size, dtype=np.float32)
        offense_count = sum(group.count for group in observation.offense)
        defense_count = sum(group.count for group in observation.defense)
        state[: self.GLOBAL_FEATURES] = np.asarray(
            [
                observation.round,
                observation.max_rounds,
                float(observation.over),
                float(observation.amphibious),
                float(observation.headless),
                offense_count,
                defense_count,
                observation.decision.required_hits,
                float(observation.decision.allow_multiple_hits_per_unit),
                _DECISION_CODES.get(observation.decision.type, -1.0),
                _stable_fraction(observation.territory),
                len(observation.decision.candidates),
            ],
            dtype=np.float32,
        )
        offset = self.GLOBAL_FEATURES
        offset = self._write_groups(state, offset, observation.offense, side=1.0)
        self._write_groups(state, offset, observation.defense, side=-1.0)

        mask = np.zeros(self.max_actions, dtype=np.int8)
        mask[: len(legal_actions)] = 1
        return {"state": state, "action_mask": mask}

    def _write_groups(
        self,
        state: np.ndarray,
        offset: int,
        groups: tuple[UnitGroupObservation, ...],
        *,
        side: float,
    ) -> int:
        for index in range(self.max_unit_groups_per_side):
            if index < len(groups):
                group = groups[index]
                values = (
                    side,
                    _stable_fraction(group.owner),
                    _stable_fraction(group.unit_type),
                    float(group.hits),
                    float(group.already_moved),
                    float(group.count),
                )
                start = offset + index * self.GROUP_FEATURES
                state[start : start + self.GROUP_FEATURES] = values
        return offset + self.max_unit_groups_per_side * self.GROUP_FEATURES


def _stable_fraction(value: str) -> float:
    digest = hashlib.blake2b(value.encode("utf-8"), digest_size=8).digest()
    integer = int.from_bytes(digest, byteorder="big", signed=False)
    return integer / float((1 << 64) - 1)
