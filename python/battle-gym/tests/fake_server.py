"""Small deterministic NDJSON server used by Python client tests."""

from __future__ import annotations

import json
import sys
from typing import Any

OVER = False


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
    global OVER
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
