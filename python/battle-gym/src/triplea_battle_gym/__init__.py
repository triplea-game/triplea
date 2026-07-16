"""Gymnasium integration for TripleA headless battle and strategic simulation."""

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
from .strategic_encoding import StrategicObservationEncoder
from .strategic_env import TripleAStrategicEnv
from .strategic_models import (
    StrategicAction,
    StrategicObservation,
    StrategicResetRequest,
    StrategicStepResult,
    TerritoryState,
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
    "StrategicAction",
    "StrategicObservation",
    "StrategicObservationEncoder",
    "StrategicResetRequest",
    "StrategicStepResult",
    "TerritoryState",
    "TripleABattleEnv",
    "TripleAStrategicEnv",
    "expand_legal_actions",
]
