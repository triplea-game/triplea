"""Expansion of strategic battle-decision descriptors into concrete actions."""

from __future__ import annotations

from collections.abc import Iterable

from .actions import expand_legal_actions
from .models import BattleAction
from .strategic_models import StrategicAction, StrategicObservation

_WRAPPER_KEYS = ("battleActionType", "battleId", "battleTerritory")


def expand_strategic_actions(
    observation: StrategicObservation,
    descriptors: Iterable[StrategicAction],
    *,
    max_actions: int,
) -> tuple[StrategicAction, ...]:
    """Turn every server descriptor into actions the server will accept.

    Moves and phase ends arrive concrete and pass straight through. A battle decision does not: the
    server wraps whatever the battle layer offered, and for casualty selection that is a descriptor
    naming the candidates and the hit count rather than a choice. Those are expanded through the
    same routine the battle environment uses, then re-wrapped.
    """
    expanded: list[StrategicAction] = []
    seen: set[tuple[str, tuple[tuple[str, str], ...]]] = set()
    for descriptor in descriptors:
        for action in _expand(observation, descriptor, max_actions=max_actions):
            key = (action.type, tuple(action.parameters.items()))
            if key in seen:
                continue
            seen.add(key)
            expanded.append(action)
    return tuple(expanded)


def _expand(
    observation: StrategicObservation,
    descriptor: StrategicAction,
    *,
    max_actions: int,
) -> tuple[StrategicAction, ...]:
    if descriptor.type != "battle_decision":
        return (descriptor,)
    battle = observation.battle
    if battle is None:
        raise ValueError("a battle_decision descriptor arrived with no battle observation")

    inner = BattleAction(
        type=descriptor.parameters["battleActionType"],
        parameters={
            key: value for key, value in descriptor.parameters.items() if key not in _WRAPPER_KEYS
        },
    )
    concrete = expand_legal_actions(battle, (inner,), max_actions=max_actions)
    return tuple(_rewrap(descriptor, action) for action in concrete)


def _rewrap(descriptor: StrategicAction, inner: BattleAction) -> StrategicAction:
    parameters = dict(inner.parameters)
    parameters["battleActionType"] = inner.type
    parameters["battleId"] = descriptor.parameters["battleId"]
    parameters["battleTerritory"] = descriptor.parameters["battleTerritory"]
    return StrategicAction("battle_decision", parameters)
