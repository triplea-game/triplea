"""Small deterministic NDJSON server used by Python client tests."""

from __future__ import annotations

import json
import sys
from typing import Any

OVER = False
STRATEGIC_OVER = False


def strategic_territory(name: str, *, visible: bool) -> dict[str, Any]:
    """A fogged territory keeps its identity and graph shape and withholds everything else."""
    if not visible:
        return {
            "territory": name,
            "water": False,
            "visible": False,
            "owner": None,
            "supplied": None,
            "supplySource": False,
            "airControlPlayer": None,
            "airControlStatus": None,
            "airControlPersistent": None,
            "neighbors": ["Alpha"],
            "roadConnections": [],
            "units": [],
        }
    return {
        "territory": name,
        "water": False,
        "visible": True,
        "owner": "Blue",
        "supplied": True,
        "supplySource": True,
        "airControlPlayer": "Blue",
        "airControlStatus": "CONTROLLED",
        "airControlPersistent": False,
        "neighbors": ["Bravo"],
        "roadConnections": ["Bravo"],
        "units": [
            {
                "owner": "Blue",
                "unitType": "infantry",
                "count": 2,
                "land": True,
                "air": False,
                "sea": False,
                "minimumMovementLeft": "1",
                "supplied": True,
                "outOfSupplyTurns": 0,
                "turnsUntilRemoval": None,
            }
        ],
    }


def strategic_observation(*, over: bool) -> dict[str, Any]:
    return {
        "schemaVersion": 2,
        "seed": 11,
        "round": 3,
        "player": "Blue",
        "sequenceStep": "BlueCombatMove",
        "phase": "COMPLETE" if over else "COMBAT_MOVE",
        "decisionDomain": "COMPLETE" if over else "STRATEGIC",
        "territories": [
            strategic_territory("Alpha", visible=True),
            strategic_territory("Bravo", visible=False),
        ],
        "reinforcements": {
            "schemaVersion": 1,
            "player": "Blue",
            "currentRound": 3,
            "lastProcessedRound": 3,
            "pending": [],
            "scheduled": [{"round": 4, "territory": "Alpha", "unitType": "armour", "quantity": 1}],
        },
        "pendingBattles": [],
        "battle": None,
        "over": over,
    }


def observation(*, over: bool) -> dict[str, Any]:
    return {
        "schemaVersion": 4,
        "seed": 7,
        "battleId": "battle-1",
        "territory": "Test Territory",
        "round": 1,
        "maxRounds": 2,
        "over": over,
        "amphibious": False,
        "headless": True,
        "offensePlayer": "attacker",
        "defensePlayer": "defender",
        "offense": [
            {
                "owner": "attacker",
                "unitType": "infantry",
                "hits": 0,
                "alreadyMoved": 0,
                "count": 2,
            }
        ],
        "defense": []
        if over
        else [
            {
                "owner": "defender",
                "unitType": "infantry",
                "hits": 0,
                "alreadyMoved": 0,
                "count": 1,
            }
        ],
        "attackerRetreatTerritories": ["Rear"],
        "airControlPlayer": "attacker",
        "offenseGroundAttackBonus": 1,
        "decision": {
            "type": "NONE" if over else "RETREAT",
            "player": "" if over else "attacker",
            "message": "",
            "requiredHits": 0,
            "allowMultipleHitsPerUnit": False,
            "candidates": [],
            "territories": [] if over else ["Rear"],
            "defaultKilledUnitIds": [],
            "defaultDamagedUnitIds": [],
        },
    }


def success(kind: str, data: Any) -> dict[str, Any]:
    return {"ok": True, "type": kind, "data": data, "error": None}


def handle(command: str, data: dict[str, Any]) -> dict[str, Any]:
    global OVER, STRATEGIC_OVER
    if command == "strategicReset":
        STRATEGIC_OVER = False
        return success("strategicObservation", {"observation": strategic_observation(over=False)})
    if command == "strategicLegalActions":
        # StatefulStrategicEnvironment sorts by type then parameters before returning, so a
        # faithful double has to hand back the same order the real server guarantees.
        actions = (
            []
            if STRATEGIC_OVER
            else [
                {"type": "end_phase", "parameters": {"phase": "COMBAT_MOVE"}},
                {
                    "type": "move",
                    "parameters": {"origin": "Alpha", "destination": "Bravo", "uncertain": "true"},
                },
            ]
        )
        return success("strategicLegalActions", actions)
    if command == "strategicStep":
        if STRATEGIC_OVER:
            return {"ok": False, "type": "error", "data": None, "error": "episode finished"}
        if data.get("type") not in {"move", "end_phase"}:
            return {"ok": False, "type": "error", "data": None, "error": "illegal action"}
        STRATEGIC_OVER = True
        return success(
            "strategicStep",
            {
                "observation": strategic_observation(over=True),
                "reward": 2.0,
                "terminated": True,
                "truncated": False,
                "info": {"actionType": data.get("type", ""), "stepId": "1"},
            },
        )
    if command == "ping":
        return success("pong", {"schemaVersion": 4})
    if command == "schema":
        return success(
            "schema",
            {
                "schemaVersion": 4,
                "episodeLogSchemaVersion": 1,
                "commands": [
                    "ping",
                    "schema",
                    "reset",
                    "legalActions",
                    "step",
                    "episodeLog",
                    "replay",
                    "batch",
                ],
                "environmentAvailable": True,
            },
        )
    if command == "reset":
        OVER = False
        return success("observation", {"observation": observation(over=False)})
    if command == "legalActions":
        actions = (
            []
            if OVER
            else [
                {"type": "continue", "parameters": {}},
                {"type": "retreat", "parameters": {"territory": "Rear"}},
            ]
        )
        return success("legalActions", actions)
    if command == "step":
        if OVER:
            return {"ok": False, "type": "error", "data": None, "error": "episode finished"}
        if data.get("type") not in {"continue", "retreat"}:
            return {"ok": False, "type": "error", "data": None, "error": "illegal action"}
        OVER = True
        return success(
            "step",
            {
                "observation": observation(over=True),
                "reward": 1.25,
                "terminated": True,
                "truncated": False,
                "info": {"result": "resolved"},
            },
        )
    if command == "episodeLog":
        return success("episodeLog", {"logSchemaVersion": 1, "transitions": []})
    if command == "replay":
        return success("replay", {"matched": True, "verifiedTransitions": 0})
    if command == "batch":
        episodes = data.get("episodes", [])
        return success(
            "batch",
            {
                "results": [{"matched": True} for _ in episodes],
                "matchedEpisodes": len(episodes),
                "mismatchedEpisodes": 0,
            },
        )
    return {"ok": False, "type": "error", "data": None, "error": f"unknown: {command}"}


for line in sys.stdin:
    if not line.strip():
        continue
    request = json.loads(line)
    print(json.dumps(handle(request["command"], request.get("data", {}))), flush=True)
