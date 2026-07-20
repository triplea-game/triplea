"""Run a local Ollama model as a tool-using Small Front strategic player.

The model never receives arbitrary Python or shell execution. It may inspect the fog-filtered
observation, query the current legal action mask, run bounded shadow simulations, and execute one
engine-validated action. Battles use TripleA's default casualty and continue decisions so the model
focuses on operational movement rather than UUID-level casualty bookkeeping.
"""

from __future__ import annotations

import argparse
import json
import os
import shlex
import statistics
import sys
import time
import urllib.error
import urllib.request
from collections.abc import Callable, Mapping, Sequence
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicAction, StrategicObservation, StrategicResetRequest

JsonObject = dict[str, Any]
EnvFactory = Callable[[], TripleAStrategicEnv]


class OllamaError(RuntimeError):
    """Raised when the local Ollama service cannot answer a tool request."""


class OllamaHttpClient:
    """Minimal standard-library client for Ollama's non-streaming chat endpoint."""

    def __init__(
        self,
        *,
        base_url: str,
        model: str,
        temperature: float,
        timeout_seconds: float = 300.0,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._model = model
        self._temperature = temperature
        self._timeout_seconds = timeout_seconds

    def check_available(self) -> None:
        request = urllib.request.Request(f"{self._base_url}/api/tags", method="GET")
        try:
            with urllib.request.urlopen(request, timeout=10) as response:  # noqa: S310
                payload = json.loads(response.read().decode("utf-8"))
        except (OSError, urllib.error.URLError, json.JSONDecodeError) as error:
            raise OllamaError(
                f"could not reach Ollama at {self._base_url}; start Ollama and retry"
            ) from error
        models = payload.get("models", []) if isinstance(payload, dict) else []
        names = {str(item.get("name", "")) for item in models if isinstance(item, Mapping)}
        if self._model not in names and not any(
            name.split(":", maxsplit=1)[0] == self._model for name in names
        ):
            raise OllamaError(
                f"Ollama model {self._model!r} is not installed; run: ollama pull {self._model}"
            )

    def chat(self, messages: Sequence[JsonObject], tools: Sequence[JsonObject]) -> JsonObject:
        body = json.dumps(
            {
                "model": self._model,
                "messages": list(messages),
                "stream": False,
                "tools": list(tools),
                "options": {"temperature": self._temperature},
            },
            ensure_ascii=False,
        ).encode("utf-8")
        request = urllib.request.Request(
            f"{self._base_url}/api/chat",
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        try:
            with urllib.request.urlopen(  # noqa: S310
                request, timeout=self._timeout_seconds
            ) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise OllamaError(f"Ollama returned HTTP {error.code}: {detail}") from error
        except (OSError, urllib.error.URLError, json.JSONDecodeError) as error:
            raise OllamaError(f"Ollama chat request failed: {error}") from error
        if not isinstance(payload, dict):
            raise OllamaError("Ollama response was not a JSON object")
        message = payload.get("message")
        if not isinstance(message, dict):
            raise OllamaError("Ollama response did not contain a message object")
        return payload


@dataclass(frozen=True, slots=True)
class ReplayStep:
    """UUID-independent description of an action used to reconstruct a shadow game."""

    action_type: str
    fields: tuple[tuple[str, str], ...]
    default_battle: bool = False

    @classmethod
    def from_action(cls, action: StrategicAction, *, default_battle: bool = False) -> ReplayStep:
        parameters = action.parameters
        if default_battle:
            keys = ("battleActionType", "battleTerritory")
        elif action.type in {"move", "air_assignment"}:
            keys = (
                "origin",
                "destination",
                "route",
                "unitType",
                "unitCount",
                "movementLeft",
                "uncertain",
            )
        elif action.type == "allocate_reinforcement":
            return cls(
                action.type,
                tuple(
                    sorted(
                        {
                            "origin": parameters.get("origin", ""),
                            "destination": parameters.get("destination", ""),
                            "unitType": parameters.get("unitType", ""),
                            "unitCount": str(_unit_id_count(parameters.get("unitIds", ""))),
                        }.items()
                    )
                ),
            )
        elif action.type == "end_phase":
            keys = ("phase",)
        else:
            keys = tuple(
                key
                for key in sorted(parameters)
                if key not in {"unitIds", "killedUnitIds", "damagedUnitIds"}
            )
        return cls(
            action.type,
            tuple((key, parameters.get(key, "")) for key in keys),
            default_battle,
        )

    def matches(self, action: StrategicAction) -> bool:
        candidate = ReplayStep.from_action(action, default_battle=self.default_battle)
        return self.action_type == candidate.action_type and self.fields == candidate.fields


class JsonlLogger:
    def __init__(self, path: Path | None) -> None:
        self._path = path
        if path is not None:
            path.parent.mkdir(parents=True, exist_ok=True)

    def write(self, event: str, data: Mapping[str, Any]) -> None:
        if self._path is None:
            return
        record = {"time": time.time(), "event": event, **dict(data)}
        with self._path.open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(record, ensure_ascii=False, sort_keys=True) + "\n")


class ActionCatalog:
    def __init__(
        self, observation: StrategicObservation, actions: Sequence[StrategicAction]
    ) -> None:
        self.observation = observation
        self.actions = tuple(actions)
        self._territories = {
            territory.territory: territory for territory in observation.territories
        }

    def get(self, action_id: int) -> StrategicAction:
        if action_id < 0 or action_id >= len(self.actions):
            raise ValueError(f"action_id must be between 0 and {len(self.actions) - 1}")
        return self.actions[action_id]

    def end_phase_id(self) -> int:
        for index, action in enumerate(self.actions):
            if action.type == "end_phase":
                return index
        raise ValueError("the current action mask has no end_phase action")

    def list_actions(
        self,
        *,
        action_type: str = "",
        origin: str = "",
        destination: str = "",
        limit: int = 40,
    ) -> list[JsonObject]:
        if limit < 1 or limit > 100:
            raise ValueError("limit must be between 1 and 100")
        result: list[JsonObject] = []
        for action_id, action in enumerate(self.actions):
            parameters = action.parameters
            if action_type and action.type != action_type:
                continue
            if origin and parameters.get("origin") != origin:
                continue
            if destination and parameters.get("destination") != destination:
                continue
            result.append(self.describe(action_id))
            if len(result) >= limit:
                break
        return result

    def describe(self, action_id: int) -> JsonObject:
        action = self.get(action_id)
        parameters = dict(action.parameters)
        description: JsonObject = {
            "actionId": action_id,
            "type": action.type,
            "parameters": {
                key: value
                for key, value in parameters.items()
                if key not in {"unitIds", "killedUnitIds", "damagedUnitIds"}
            },
        }
        if "unitIds" in parameters:
            description["unitCount"] = _unit_id_count(parameters["unitIds"])
        destination = parameters.get("destination")
        if destination and destination in self._territories:
            territory = self._territories[destination]
            description["destinationState"] = _territory_summary(territory)
        return description


class ShadowSimulator:
    """Replays the real action history in a disposable environment before testing a candidate."""

    def __init__(
        self,
        *,
        env_factory: EnvFactory,
        base_seed: int,
        max_rollouts: int,
    ) -> None:
        self._env_factory = env_factory
        self._base_seed = base_seed
        self._max_rollouts = max_rollouts
        self._cache: dict[tuple[object, ...], JsonObject] = {}

    def simulate(
        self,
        *,
        history: Sequence[ReplayStep],
        action: StrategicAction,
        requested_rollouts: int,
    ) -> JsonObject:
        if requested_rollouts < 1:
            raise ValueError("rollouts must be positive")
        rollouts = min(requested_rollouts, self._max_rollouts)
        stochastic_history = any(step.default_battle for step in history)
        note = ""
        if stochastic_history and rollouts > 1:
            rollouts = 1
            note = (
                "Earlier battles already consumed RNG. Only the original seed can reproduce the "
                "exact current state, so this evaluation was reduced to one exact rollout."
            )
        key = (
            tuple(history),
            ReplayStep.from_action(action),
            rollouts,
            stochastic_history,
        )
        cached = self._cache.get(key)
        if cached is not None:
            return {**cached, "cached": True}

        outcomes: list[JsonObject] = []
        failures: list[str] = []
        for index in range(rollouts):
            seed = self._base_seed if stochastic_history else self._base_seed + index * 10007
            try:
                outcomes.append(self._run_once(history=history, action=action, seed=seed))
            except Exception as error:  # The failure is returned to the model, not hidden.
                failures.append(f"seed {seed}: {type(error).__name__}: {error}")

        rewards = [float(outcome["reward"]) for outcome in outcomes]
        result: JsonObject = {
            "requestedRollouts": requested_rollouts,
            "completedRollouts": len(outcomes),
            "failedRollouts": failures,
            "exactCurrentState": stochastic_history or rollouts == 1,
            "note": note,
            "outcomes": outcomes,
        }
        if rewards:
            result.update(
                {
                    "meanReward": statistics.fmean(rewards),
                    "minimumReward": min(rewards),
                    "maximumReward": max(rewards),
                }
            )
        self._cache[key] = result
        return result

    def _run_once(
        self,
        *,
        history: Sequence[ReplayStep],
        action: StrategicAction,
        seed: int,
    ) -> JsonObject:
        env = self._env_factory()
        try:
            env.reset(seed=seed)
            for replay_step in history:
                action_index = _find_replay_action(env.legal_actions, replay_step)
                _, _, terminated, truncated, _ = env.step(action_index)
                if terminated or truncated:
                    raise RuntimeError("shadow episode ended while replaying the real history")

            before = env.raw_observation
            candidate_index = _find_replay_action(env.legal_actions, ReplayStep.from_action(action))
            _, reward, terminated, truncated, info = env.step(candidate_index)
            total_reward = float(reward)
            auto_steps = 0
            acting_player = before.player
            acting_round = before.round

            while not terminated and not truncated and auto_steps < 128:
                observation = env.raw_observation
                if observation.player != acting_player or observation.round != acting_round:
                    break
                if observation.phase in {"COMBAT_MOVE", "AIR_ASSIGNMENT"}:
                    next_index = _find_end_phase(env.legal_actions)
                elif observation.phase == "BATTLE":
                    next_index = _default_battle_action(env.legal_actions)
                else:
                    break
                _, step_reward, terminated, truncated, info = env.step(next_index)
                total_reward += float(step_reward)
                auto_steps += 1

            after = env.raw_observation
            return {
                "seed": seed,
                "reward": total_reward,
                "terminated": terminated,
                "truncated": truncated,
                "automaticSteps": auto_steps,
                "finalPlayer": after.player,
                "finalRound": after.round,
                "finalPhase": after.phase,
                "ownerChanges": _owner_changes(before, after),
                "visibleUnitDelta": _visible_unit_delta(before, after),
                "pendingBattles": [battle.territory for battle in after.pending_battles],
                "lastInfo": dict(info),
                "assumption": (
                    "After the candidate, the simulator ends remaining combat/air movement, uses "
                    "default casualties, declines optional retreats, and stops at redeployment."
                ),
            }
        finally:
            env.close()


class ToolSession:
    def __init__(
        self,
        *,
        env: TripleAStrategicEnv,
        catalog: ActionCatalog,
        history: list[ReplayStep],
        simulator: ShadowSimulator,
        default_rollouts: int,
        logger: JsonlLogger,
    ) -> None:
        self.env = env
        self.catalog = catalog
        self.history = history
        self.simulator = simulator
        self.default_rollouts = default_rollouts
        self.logger = logger
        self.executed = False
        self.selected_action: StrategicAction | None = None
        self.step_result: JsonObject | None = None

    def call(self, name: str, arguments: Mapping[str, Any]) -> JsonObject:
        if self.executed:
            return {"error": "one action has already been executed; wait for the next decision"}
        try:
            if name == "get_game_state":
                return summarize_observation(self.env.raw_observation, len(self.catalog.actions))
            if name == "list_legal_actions":
                return {
                    "actions": self.catalog.list_actions(
                        action_type=str(arguments.get("action_type", "")),
                        origin=str(arguments.get("origin", "")),
                        destination=str(arguments.get("destination", "")),
                        limit=int(arguments.get("limit", 40)),
                    ),
                    "totalLegalActions": len(self.catalog.actions),
                }
            if name == "inspect_action":
                return self.catalog.describe(int(arguments["action_id"]))
            if name == "simulate_action":
                action = self.catalog.get(int(arguments["action_id"]))
                if action.type == "end_phase":
                    return {"error": "end_phase does not need a battle simulation"}
                return self.simulator.simulate(
                    history=self.history,
                    action=action,
                    requested_rollouts=int(arguments.get("rollouts", self.default_rollouts)),
                )
            if name == "execute_action":
                return self._execute(int(arguments["action_id"]))
            if name == "end_phase":
                return self._execute(self.catalog.end_phase_id())
            return {"error": f"unknown tool: {name}"}
        except (KeyError, TypeError, ValueError, RuntimeError) as error:
            return {"error": f"{type(error).__name__}: {error}"}

    def _execute(self, action_id: int) -> JsonObject:
        action = self.catalog.get(action_id)
        _, reward, terminated, truncated, info = self.env.step(action_id)
        self.history.append(ReplayStep.from_action(action))
        self.executed = True
        self.selected_action = action
        self.step_result = {
            "actionId": action_id,
            "action": action.to_dict(),
            "reward": float(reward),
            "terminated": terminated,
            "truncated": truncated,
            "info": dict(info),
            "nextState": summarize_observation(
                self.env.raw_observation, len(self.env.legal_actions)
            ),
        }
        self.logger.write("action", self.step_result)
        return self.step_result


TOOLS: tuple[JsonObject, ...] = (
    {
        "type": "function",
        "function": {
            "name": "get_game_state",
            "description": "Return the current fog-filtered Small Front state.",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "list_legal_actions",
            "description": (
                "List engine-validated actions. Filter by type, origin, or destination. "
                "Action IDs are valid only for the current decision."
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "action_type": {"type": "string"},
                    "origin": {"type": "string"},
                    "destination": {"type": "string"},
                    "limit": {"type": "integer", "minimum": 1, "maximum": 100},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "inspect_action",
            "description": "Inspect one current legal action and its visible destination state.",
            "parameters": {
                "type": "object",
                "required": ["action_id"],
                "properties": {"action_id": {"type": "integer", "minimum": 0}},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "simulate_action",
            "description": (
                "Evaluate a candidate in disposable TripleA shadow games. It commits immediately "
                "after that candidate, uses default battle decisions, and does not mutate the real game."
            ),
            "parameters": {
                "type": "object",
                "required": ["action_id"],
                "properties": {
                    "action_id": {"type": "integer", "minimum": 0},
                    "rollouts": {"type": "integer", "minimum": 1, "maximum": 8},
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "execute_action",
            "description": (
                "Execute exactly one current legal action after TripleA validates it. "
                "Use this only after finishing analysis."
            ),
            "parameters": {
                "type": "object",
                "required": ["action_id"],
                "properties": {"action_id": {"type": "integer", "minimum": 0}},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "end_phase",
            "description": "Execute the current end_phase action.",
            "parameters": {"type": "object", "properties": {}},
        },
    },
)


SYSTEM_PROMPT = """You are the operational commander for the current Small Front player.
You are in a bounded tool loop. Use only the supplied tools; never invent actions, unit IDs, map
state, Python code, shell commands, or hidden information. First inspect the state and legal actions.
Compare a small number of promising actions. Use shadow simulation sparingly for attacks or risky
moves. Preserve supply, respect destination stack capacity, maintain reserves, and pursue objectives.
Execute exactly one action with execute_action, or call end_phase when further legal movement is not
useful. Battles and casualty choices are handled automatically by TripleA outside this loop.
"""


class LocalLlmGame:
    def __init__(
        self,
        *,
        env: TripleAStrategicEnv,
        ollama: OllamaHttpClient,
        simulator: ShadowSimulator,
        max_tool_rounds: int,
        default_rollouts: int,
        max_decisions: int,
        logger: JsonlLogger,
    ) -> None:
        self._env = env
        self._ollama = ollama
        self._simulator = simulator
        self._max_tool_rounds = max_tool_rounds
        self._default_rollouts = default_rollouts
        self._max_decisions = max_decisions
        self._logger = logger
        self._history: list[ReplayStep] = []

    def run(self) -> None:
        self._env.reset()
        for decision_number in range(1, self._max_decisions + 1):
            observation = self._env.raw_observation
            if observation.over:
                print(
                    f"Game over at round {observation.round} after {decision_number - 1} actions."
                )
                self._logger.write(
                    "game_over",
                    {"round": observation.round, "actions": decision_number - 1},
                )
                return
            if observation.phase == "BATTLE":
                self._execute_default_battle()
                continue
            self._run_model_decision(decision_number)
        raise RuntimeError(f"maximum decision count {self._max_decisions} reached")

    def _execute_default_battle(self) -> None:
        actions = self._env.legal_actions
        action_id = _default_battle_action(actions)
        action = actions[action_id]
        _, reward, terminated, truncated, info = self._env.step(action_id)
        self._history.append(ReplayStep.from_action(action, default_battle=True))
        event = {
            "action": action.to_dict(),
            "reward": float(reward),
            "terminated": terminated,
            "truncated": truncated,
            "info": dict(info),
        }
        self._logger.write("automatic_battle_action", event)
        print(
            f"[battle] {action.parameters.get('battleTerritory', '')}: "
            f"{action.parameters.get('battleActionType', action.type)}"
        )

    def _run_model_decision(self, decision_number: int) -> None:
        catalog = ActionCatalog(self._env.raw_observation, self._env.legal_actions)
        session = ToolSession(
            env=self._env,
            catalog=catalog,
            history=self._history,
            simulator=self._simulator,
            default_rollouts=self._default_rollouts,
            logger=self._logger,
        )
        observation = self._env.raw_observation
        messages: list[JsonObject] = [
            {"role": "system", "content": SYSTEM_PROMPT},
            {
                "role": "user",
                "content": (
                    f"Decision {decision_number}: round {observation.round}, "
                    f"player {observation.player}, phase {observation.phase}, "
                    f"{len(catalog.actions)} legal actions. Choose and execute one action."
                ),
            },
        ]
        self._logger.write(
            "decision",
            {
                "number": decision_number,
                "state": summarize_observation(observation, len(catalog.actions)),
            },
        )

        try:
            for _ in range(self._max_tool_rounds):
                response = self._ollama.chat(messages, TOOLS)
                raw_message = response["message"]
                assert isinstance(raw_message, dict)
                assistant_message = {
                    key: value
                    for key, value in raw_message.items()
                    if key in {"role", "content", "tool_calls", "thinking"}
                }
                messages.append(assistant_message)
                tool_calls = raw_message.get("tool_calls", [])
                if not isinstance(tool_calls, list) or not tool_calls:
                    break
                for tool_call in tool_calls:
                    name, arguments = _parse_tool_call(tool_call)
                    print(f"[{observation.player} {observation.phase}] tool: {name}")
                    result = session.call(name, arguments)
                    messages.append(
                        {
                            "role": "tool",
                            "tool_name": name,
                            "content": json.dumps(result, ensure_ascii=False),
                        }
                    )
                    self._logger.write(
                        "tool",
                        {"name": name, "arguments": dict(arguments), "result": result},
                    )
                    if session.executed:
                        break
                if session.executed:
                    break
        except OllamaError as error:
            self._logger.write("ollama_error", {"error": str(error)})
            print(f"Ollama error: {error}; using fallback action.", file=sys.stderr)

        if not session.executed:
            fallback = _fallback_action(catalog)
            print(
                f"[{observation.player} {observation.phase}] fallback action {fallback}: "
                f"{catalog.get(fallback).type}"
            )
            session.call("execute_action", {"action_id": fallback})
        elif session.selected_action is not None:
            action = session.selected_action
            print(
                f"[{observation.player} {observation.phase}] executed "
                f"{action.type} {dict(action.parameters)}"
            )


def summarize_observation(observation: StrategicObservation, legal_action_count: int) -> JsonObject:
    return {
        "schemaVersion": observation.schema_version,
        "round": observation.round,
        "player": observation.player,
        "sequenceStep": observation.sequence_step,
        "phase": observation.phase,
        "decisionDomain": observation.decision_domain,
        "legalActionCount": legal_action_count,
        "territories": [_territory_summary(territory) for territory in observation.territories],
        "reinforcements": {
            "pending": [
                {
                    "round": item.round,
                    "territory": item.territory,
                    "unitType": item.unit_type,
                    "quantity": item.quantity,
                }
                for item in observation.reinforcements.pending
            ],
            "scheduled": [
                {
                    "round": item.round,
                    "territory": item.territory,
                    "unitType": item.unit_type,
                    "quantity": item.quantity,
                }
                for item in observation.reinforcements.scheduled
            ],
        },
        "pendingBattles": [
            {
                "territory": battle.territory,
                "battleType": battle.battle_type,
            }
            for battle in observation.pending_battles
        ],
        "over": observation.over,
    }


def _territory_summary(territory: Any) -> JsonObject:
    result: JsonObject = {
        "name": territory.territory,
        "water": territory.water,
        "visible": territory.visible,
        "neighbors": list(territory.neighbors),
        "roadConnections": list(territory.road_connections),
    }
    if territory.visible:
        result.update(
            {
                "owner": territory.owner,
                "supplied": territory.supplied,
                "supplySource": territory.supply_source,
                "airControlPlayer": territory.air_control_player,
                "airControlStatus": territory.air_control_status,
                "units": [
                    {
                        "owner": unit.owner,
                        "unitType": unit.unit_type,
                        "count": unit.count,
                        "land": unit.land,
                        "air": unit.air,
                        "sea": unit.sea,
                        "movementLeft": unit.minimum_movement_left,
                        "supplied": unit.supplied,
                        "outOfSupplyTurns": unit.out_of_supply_turns,
                    }
                    for unit in territory.units
                ],
            }
        )
    return result


def _find_replay_action(actions: Sequence[StrategicAction], step: ReplayStep) -> int:
    matches = [index for index, action in enumerate(actions) if step.matches(action)]
    if not matches:
        raise RuntimeError(
            f"could not replay semantic action {step}; legal actions: {len(actions)}"
        )
    return matches[0]


def _find_end_phase(actions: Sequence[StrategicAction]) -> int:
    for index, action in enumerate(actions):
        if action.type == "end_phase":
            return index
    raise RuntimeError("no end_phase action is available")


def _default_battle_action(actions: Sequence[StrategicAction]) -> int:
    if not actions:
        raise RuntimeError("battle phase has no legal action")
    for index, action in enumerate(actions):
        if (
            action.type == "battle_decision"
            and action.parameters.get("battleActionType") == "continue"
        ):
            return index
    return 0


def _fallback_action(catalog: ActionCatalog) -> int:
    observation = catalog.observation
    territories = {territory.territory: territory for territory in observation.territories}
    best_id = catalog.end_phase_id()
    best_score = -10_000
    for action_id, action in enumerate(catalog.actions):
        if action.type == "end_phase":
            continue
        score = 10
        if action.type == "allocate_reinforcement":
            score += 30
        if action.type == "air_assignment":
            score += 15
        destination = action.parameters.get("destination")
        if destination and destination in territories:
            state = territories[destination]
            if state.visible and state.owner not in {None, observation.player}:
                score += 50
            if state.supplied is False:
                score -= 20
        if action.parameters.get("uncertain") == "true":
            score -= 10
        score += int(action.parameters.get("unitCount", "1"))
        if score > best_score:
            best_score = score
            best_id = action_id
    return best_id


def _parse_tool_call(value: object) -> tuple[str, Mapping[str, Any]]:
    if not isinstance(value, Mapping):
        raise OllamaError("tool call was not an object")
    function = value.get("function")
    if not isinstance(function, Mapping):
        raise OllamaError("tool call did not contain a function object")
    name = str(function.get("name", ""))
    if not name:
        raise OllamaError("tool call function name was empty")
    arguments = function.get("arguments", {})
    if isinstance(arguments, str):
        try:
            parsed = json.loads(arguments)
        except json.JSONDecodeError as error:
            raise OllamaError(f"tool arguments were invalid JSON: {arguments}") from error
        arguments = parsed
    if not isinstance(arguments, Mapping):
        raise OllamaError("tool arguments were not an object")
    return name, arguments


def _unit_id_count(value: str) -> int:
    return len([item for item in value.split(",") if item])


def _owner_changes(before: StrategicObservation, after: StrategicObservation) -> list[JsonObject]:
    before_owners = {
        territory.territory: territory.owner
        for territory in before.territories
        if territory.visible
    }
    changes: list[JsonObject] = []
    for territory in after.territories:
        if not territory.visible or territory.territory not in before_owners:
            continue
        previous = before_owners[territory.territory]
        if previous != territory.owner:
            changes.append(
                {
                    "territory": territory.territory,
                    "before": previous,
                    "after": territory.owner,
                }
            )
    return changes


def _visible_unit_delta(before: StrategicObservation, after: StrategicObservation) -> JsonObject:
    before_counts = _visible_unit_counts(before)
    after_counts = _visible_unit_counts(after)
    keys = sorted(set(before_counts) | set(after_counts))
    return {
        f"{owner}:{unit_type}": after_counts.get((owner, unit_type), 0)
        - before_counts.get((owner, unit_type), 0)
        for owner, unit_type in keys
        if after_counts.get((owner, unit_type), 0) != before_counts.get((owner, unit_type), 0)
    }


def _visible_unit_counts(observation: StrategicObservation) -> dict[tuple[str, str], int]:
    counts: dict[tuple[str, str], int] = {}
    for territory in observation.territories:
        if not territory.visible:
            continue
        for group in territory.units:
            key = (group.owner, group.unit_type)
            counts[key] = counts.get(key, 0) + group.count
    return counts


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument("--scenario", type=Path, required=True)
    parser.add_argument("--model", default="qwen3:8b")
    parser.add_argument("--ollama-url", default="http://127.0.0.1:11434")
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--max-actions", type=int, default=4096)
    parser.add_argument("--max-territories", type=int, default=64)
    parser.add_argument("--max-rounds", type=int, default=12)
    parser.add_argument("--max-decisions", type=int, default=2000)
    parser.add_argument("--max-tool-rounds", type=int, default=8)
    parser.add_argument("--simulation-rollouts", type=int, default=1)
    parser.add_argument("--max-simulation-rollouts", type=int, default=8)
    parser.add_argument("--temperature", type=float, default=0.2)
    parser.add_argument("--log", type=Path)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    scenario = args.scenario.resolve()
    if not scenario.is_file():
        raise SystemExit(f"scenario is not a file: {scenario}")
    if args.max_tool_rounds < 1 or args.max_decisions < 1:
        raise SystemExit("decision and tool limits must be positive")
    if args.simulation_rollouts < 1 or args.max_simulation_rollouts < 1:
        raise SystemExit("simulation rollout counts must be positive")

    command = tuple(shlex.split(args.server_command, posix=os.name != "nt"))
    reset_request = StrategicResetRequest(
        scenario_path=str(scenario),
        seed=args.seed,
        self_play=True,
        max_actions=args.max_actions,
        max_rounds=args.max_rounds,
    )

    def make_env() -> TripleAStrategicEnv:
        return TripleAStrategicEnv(
            server_command=command,
            reset_request=reset_request,
            max_territories=args.max_territories,
            max_actions=args.max_actions,
        )

    ollama = OllamaHttpClient(
        base_url=args.ollama_url,
        model=args.model,
        temperature=args.temperature,
    )
    ollama.check_available()
    logger = JsonlLogger(args.log)
    env = make_env()
    simulator = ShadowSimulator(
        env_factory=make_env,
        base_seed=args.seed,
        max_rollouts=args.max_simulation_rollouts,
    )
    print(f"Starting Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    if args.log is not None:
        print(f"Log: {args.log.resolve()}")
    try:
        LocalLlmGame(
            env=env,
            ollama=ollama,
            simulator=simulator,
            max_tool_rounds=args.max_tool_rounds,
            default_rollouts=args.simulation_rollouts,
            max_decisions=args.max_decisions,
            logger=logger,
        ).run()
    finally:
        env.close()


if __name__ == "__main__":
    main()
