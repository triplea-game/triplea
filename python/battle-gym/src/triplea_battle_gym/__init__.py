"""Gymnasium integration for TripleA headless battle simulation."""

from .actions import ActionSpaceOverflow, expand_legal_actions
from .client import BattleClient, BattleProtocolError, BattleServerError
from .env import TripleABattleEnv
from .models import (
    BattleAction,
    BattleDecisionObservation,
    BattleObservation,
    BattleResetRequest,
    BattleStepResult,
)
from .worker import BattleWorkerPool, BattleWorkerSpec

__all__ = [
    "ActionSpaceOverflow",
    "BattleAction",
    "BattleClient",
    "BattleDecisionObservation",
    "BattleObservation",
    "BattleProtocolError",
    "BattleResetRequest",
    "BattleServerError",
    "BattleStepResult",
    "BattleWorkerPool",
    "BattleWorkerSpec",
    "TripleABattleEnv",
    "expand_legal_actions",
]
