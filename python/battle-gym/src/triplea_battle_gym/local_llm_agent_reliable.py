"""Reliable decision wrapper for the Small Front Ollama tool agent.

This module keeps the original bounded tool surface but adds a repair pass when a local model
returns prose without executing an action. The repair pass exposes only execute_action/end_phase and
includes a compact legal-action shortlist, which substantially reduces accidental heuristic fallback.
"""

from __future__ import annotations

import json
import os
import shlex
import sys
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from .local_llm_agent import (
    ActionCatalog,
    JsonObject,
    JsonlLogger,
    LocalLlmGame,
    OllamaError,
    OllamaHttpClient,
    ReplayStep,
    ShadowSimulator,
    SYSTEM_PROMPT,
    TOOLS,
    ToolSession,
    _fallback_action,
    _parse_tool_call,
    build_parser,
    summarize_observation,
)
from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest

_EXECUTION_TOOL_NAMES = {"execute_action", "end_phase"}
EXECUTION_TOOLS: tuple[JsonObject, ...] = tuple(
    tool
    for tool in TOOLS
    if isinstance(tool.get("function"), Mapping)
    and tool["function"].get("name") in _EXECUTION_TOOL_NAMES
)

RELIABLE_SYSTEM_PROMPT = (
    SYSTEM_PROMPT
    + "\nA decision is not complete until you call execute_action or end_phase. "
    + "Do not finish with prose after inspecting state. If an action attacks a visible enemy, "
    + "prefer simulate_action before executing it."
)


class ReliableLocalLlmGame(LocalLlmGame):
    """Local LLM game loop with one bounded action-selection repair pass."""

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
            {"role": "system", "content": RELIABLE_SYSTEM_PROMPT},
            {
                "role": "user",
                "content": _initial_decision_message(decision_number, catalog),
            },
        ]
        self._logger.write(
            "decision",
            {
                "number": decision_number,
                "state": summarize_observation(observation, len(catalog.actions)),
            },
        )

        no_tool_responses = 0
        try:
            for _ in range(self._max_tool_rounds):
                tools = EXECUTION_TOOLS if no_tool_responses else TOOLS
                response = self._ollama.chat(messages, tools)
                raw_message = response["message"]
                if not isinstance(raw_message, dict):
                    raise OllamaError("Ollama message was not an object")
                assistant_message = {
                    key: value
                    for key, value in raw_message.items()
                    if key in {"role", "content", "tool_calls", "thinking"}
                }
                messages.append(assistant_message)
                tool_calls = raw_message.get("tool_calls", [])
                if not isinstance(tool_calls, list) or not tool_calls:
                    no_tool_responses += 1
                    if no_tool_responses <= 2:
                        repair = _repair_decision_message(catalog, no_tool_responses)
                        messages.append({"role": "user", "content": repair})
                        self._logger.write(
                            "decision_repair",
                            {
                                "attempt": no_tool_responses,
                                "reason": "model returned no tool call",
                            },
                        )
                        continue
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


def _initial_decision_message(decision_number: int, catalog: ActionCatalog) -> str:
    observation = catalog.observation
    shortlist = catalog.list_actions(limit=min(24, len(catalog.actions)))
    payload = {
        "decision": decision_number,
        "round": observation.round,
        "player": observation.player,
        "phase": observation.phase,
        "legalActionCount": len(catalog.actions),
        "legalActionShortlist": shortlist,
    }
    return (
        "Choose and execute exactly one legal action. You may inspect or simulate first. "
        "Do not stop with an explanation. Current decision:\n"
        + json.dumps(payload, ensure_ascii=False)
    )


def _repair_decision_message(catalog: ActionCatalog, attempt: int) -> str:
    shortlist = catalog.list_actions(limit=min(40, len(catalog.actions)))
    return (
        f"Repair attempt {attempt}: no action was executed. "
        "You must now call execute_action with one action_id from the shortlist, or call end_phase. "
        "Return no prose-only answer. Legal shortlist:\n"
        + json.dumps(shortlist, ensure_ascii=False)
    )


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
    print(f"Starting reliable Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    if args.log is not None:
        print(f"Log: {Path(args.log).resolve()}")
    try:
        ReliableLocalLlmGame(
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
