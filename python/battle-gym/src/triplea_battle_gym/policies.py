"""Small deterministic and random policy baselines."""

from __future__ import annotations

import random

from .models import BattleAction


def scripted_action_index(actions: tuple[BattleAction, ...]) -> int:
    """Use default casualties and decline optional retreats when possible."""

    if not actions:
        raise ValueError("cannot choose from an empty legal-action set")
    for index, action in enumerate(actions):
        if action.type == "continue":
            return index
    return 0


def random_action_index(actions: tuple[BattleAction, ...], rng: random.Random) -> int:
    if not actions:
        raise ValueError("cannot choose from an empty legal-action set")
    return rng.randrange(len(actions))
