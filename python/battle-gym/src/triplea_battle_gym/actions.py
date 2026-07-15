"""Deterministic expansion of parameterized TripleA action descriptors."""

from __future__ import annotations

from collections.abc import Iterable
from itertools import combinations

from .models import BattleAction, BattleObservation, DecisionUnitObservation


class ActionSpaceOverflow(RuntimeError):
    """A decision has more concrete legal actions than the configured fixed space."""


def expand_legal_actions(
    observation: BattleObservation,
    descriptors: Iterable[BattleAction],
    *,
    max_actions: int,
) -> tuple[BattleAction, ...]:
    """Expand server descriptors into concrete actions without dropping any combination."""

    if max_actions <= 0:
        raise ValueError("max_actions must be positive")

    expanded: list[BattleAction] = []
    seen: set[tuple[str, tuple[tuple[str, str], ...]]] = set()
    for descriptor in descriptors:
        actions = (
            _expand_casualties(observation)
            if descriptor.type == "select_casualties"
            else (descriptor,)
        )
        for action in actions:
            key = (action.type, tuple(action.parameters.items()))
            if key in seen:
                continue
            seen.add(key)
            expanded.append(action)
            if len(expanded) > max_actions:
                raise ActionSpaceOverflow(
                    f"decision expands to more than max_actions={max_actions}; "
                    "increase max_actions to preserve the full legal-action set"
                )
    return tuple(expanded)


def _expand_casualties(observation: BattleObservation) -> tuple[BattleAction, ...]:
    decision = observation.decision
    if decision.type != "SELECT_CASUALTIES":
        raise ValueError("select_casualties descriptor does not match the observation decision")
    if decision.required_hits < 0:
        raise ValueError("required_hits must not be negative")
    candidates = tuple(sorted(decision.candidates, key=lambda item: item.unit_id))
    if decision.required_hits > 0 and not candidates:
        raise ValueError("casualty decision has hits but no candidates")

    default = _casualty_action(
        decision.default_killed_unit_ids,
        decision.default_damaged_unit_ids,
    )
    if decision.required_hits == 0:
        return (default,)

    generated: list[BattleAction] = [default]
    if not decision.allow_multiple_hits_per_unit:
        for killed in combinations(candidates, decision.required_hits):
            generated.append(_casualty_action((unit.unit_id for unit in killed), ()))
        return _deduplicate(generated)

    allocations: list[tuple[tuple[str, ...], tuple[str, ...]]] = []
    _enumerate_multi_hit_allocations(
        candidates=candidates,
        index=0,
        remaining=decision.required_hits,
        killed=[],
        damaged=[],
        output=allocations,
    )
    generated.extend(_casualty_action(killed, damaged) for killed, damaged in allocations)
    return _deduplicate(generated)


def _enumerate_multi_hit_allocations(
    *,
    candidates: tuple[DecisionUnitObservation, ...],
    index: int,
    remaining: int,
    killed: list[str],
    damaged: list[str],
    output: list[tuple[tuple[str, ...], tuple[str, ...]]],
) -> None:
    if remaining == 0:
        output.append((tuple(killed), tuple(damaged)))
        return
    if index >= len(candidates):
        return

    candidate = candidates[index]
    maximum_damage = max(0, candidate.hit_points - candidate.hits - 1)
    max_contribution = min(remaining, maximum_damage + 1)

    _enumerate_multi_hit_allocations(
        candidates=candidates,
        index=index + 1,
        remaining=remaining,
        killed=killed,
        damaged=damaged,
        output=output,
    )
    for contribution in range(1, max_contribution + 1):
        damage_count = min(contribution, maximum_damage)
        is_killed = contribution > damage_count
        if damage_count:
            damaged.extend([candidate.unit_id] * damage_count)
        if is_killed:
            killed.append(candidate.unit_id)
        _enumerate_multi_hit_allocations(
            candidates=candidates,
            index=index + 1,
            remaining=remaining - contribution,
            killed=killed,
            damaged=damaged,
            output=output,
        )
        if is_killed:
            killed.pop()
        if damage_count:
            del damaged[-damage_count:]


def _casualty_action(
    killed_unit_ids: Iterable[str],
    damaged_unit_ids: Iterable[str],
) -> BattleAction:
    return BattleAction(
        "select_casualties",
        {
            "killedUnitIds": ",".join(killed_unit_ids),
            "damagedUnitIds": ",".join(damaged_unit_ids),
        },
    )


def _deduplicate(actions: Iterable[BattleAction]) -> tuple[BattleAction, ...]:
    unique: dict[tuple[str, tuple[tuple[str, str], ...]], BattleAction] = {}
    for action in actions:
        unique.setdefault((action.type, tuple(action.parameters.items())), action)
    return tuple(unique.values())
