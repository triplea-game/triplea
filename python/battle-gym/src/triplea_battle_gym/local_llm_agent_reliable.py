"""Reliable decision wrapper for the Small Front Ollama tool agent.

The model may use bounded inspection and simulation tools. If it finishes with prose instead of an
execution tool call, a second Ollama request uses a JSON Schema to require one legal action id and a
concise public Korean explanation. Only a failed structured choice reaches the heuristic fallback.
"""

from __future__ import annotations

import json
import os
import shlex
import sys
import urllib.error
import urllib.request
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from .local_llm_agent import (
    SYSTEM_PROMPT,
    TOOLS,
    ActionCatalog,
    JsonlLogger,
    JsonObject,
    LocalLlmGame,
    OllamaError,
    OllamaHttpClient,
    ShadowSimulator,
    ToolSession,
    _fallback_action,
    _parse_tool_call,
    build_parser,
    summarize_observation,
)
from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest

_EXECUTION_TOOL_NAMES = {"execute_action", "end_phase"}


def _require_public_reason(tool: JsonObject) -> JsonObject:
    """Clone one tool schema and require a concise user-visible reason on terminal actions."""
    cloned = json.loads(json.dumps(tool))
    function = cloned.get("function")
    if not isinstance(function, dict) or function.get("name") not in _EXECUTION_TOOL_NAMES:
        return cloned
    parameters = function.setdefault("parameters", {"type": "object", "properties": {}})
    properties = parameters.setdefault("properties", {})
    properties["reason"] = {
        "type": "string",
        "minLength": 1,
        "maxLength": 400,
        "description": (
            "A concise Korean commander explanation for the human console: describe the current "
            "situation, the immediate objective, and why this action is preferred. Use 1-3 sentences. "
            "Do not provide hidden chain-of-thought."
        ),
    }
    required = list(parameters.get("required", []))
    if "reason" not in required:
        required.append("reason")
    parameters["required"] = required
    return cloned


COMMENTED_TOOLS: tuple[JsonObject, ...] = tuple(_require_public_reason(tool) for tool in TOOLS)
EXECUTION_TOOLS: tuple[JsonObject, ...] = tuple(
    tool
    for tool in COMMENTED_TOOLS
    if isinstance(tool.get("function"), Mapping)
    and tool["function"].get("name") in _EXECUTION_TOOL_NAMES
)

RELIABLE_SYSTEM_PROMPT = (
    SYSTEM_PROMPT
    + "\nA decision is not complete until you call execute_action or end_phase. "
    + "Do not finish with prose after inspecting state. If an action attacks a visible enemy, "
    + "prefer simulate_action before executing it. Every execute_action or end_phase call must include "
    + "a concise Korean reason of 1-3 sentences. The reason is printed to the human command prompt, so "
    + "state the visible situation, immediate objective, and why the selected action is preferable. "
    + "Give only a public decision summary, not hidden chain-of-thought. If you return no terminal tool "
    + "call, a final JSON decision request will require an action id and Korean reason."
)


class ReliableLocalLlmGame(LocalLlmGame):
    """Local LLM game loop with structured final choices and console explanations."""

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

        try:
            for _ in range(self._max_tool_rounds):
                response = self._ollama.chat(messages, COMMENTED_TOOLS)
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
                    break

                for tool_call in tool_calls:
                    name, arguments = _parse_tool_call(tool_call)
                    print(f"[{observation.player} {observation.phase}] tool: {name}")
                    if name in _EXECUTION_TOOL_NAMES:
                        reason = _public_reason(arguments)
                        _print_and_log_reason(
                            player=observation.player,
                            phase=observation.phase,
                            tool=name,
                            reason=reason,
                            logger=self._logger,
                        )
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

            if not session.executed:
                action_id, reason = _structured_final_choice(
                    ollama=self._ollama,
                    messages=messages,
                    catalog=catalog,
                    decision_number=decision_number,
                )
                print(
                    f"[{observation.player} {observation.phase}] structured-choice: action {action_id}"
                )
                _print_and_log_reason(
                    player=observation.player,
                    phase=observation.phase,
                    tool="structured_choice",
                    reason=reason,
                    logger=self._logger,
                )
                result = session.call("execute_action", {"action_id": action_id, "reason": reason})
                if "error" in result:
                    raise OllamaError(f"structured action could not be executed: {result['error']}")
        except OllamaError as error:
            self._logger.write("ollama_error", {"error": str(error)})
            print(f"Ollama decision error: {error}; using fallback action.", file=sys.stderr)

        if not session.executed:
            fallback = _fallback_action(catalog)
            fallback_reason = (
                "모델의 도구 탐색과 구조화된 최종 선택이 모두 실패해 안전장치가 합법 행동을 "
                "선택했습니다. 이 설명은 모델의 판단이 아니라 휴리스틱 fallback입니다."
            )
            _print_and_log_reason(
                player=observation.player,
                phase=observation.phase,
                tool="fallback",
                reason=fallback_reason,
                logger=self._logger,
            )
            print(
                f"[{observation.player} {observation.phase}] fallback action {fallback}: "
                f"{catalog.get(fallback).type}"
            )
            session.call("execute_action", {"action_id": fallback, "reason": fallback_reason})

        if session.selected_action is not None:
            action = session.selected_action
            print(
                f"[{observation.player} {observation.phase}] executed "
                f"{action.type} {dict(action.parameters)}"
            )


def _print_and_log_reason(
    *, player: str, phase: str, tool: str, reason: str, logger: JsonlLogger
) -> None:
    print(f"[{player} {phase}] commander: {reason}")
    logger.write("decision_explanation", {"tool": tool, "reason": reason})


def _public_reason(arguments: Mapping[str, Any]) -> str:
    value = str(arguments.get("reason", "")).strip()
    if value:
        return value
    return "모델이 공개 설명을 제공하지 않았습니다. 선택한 행동만 엔진에서 검증해 실행합니다."


def _balanced_shortlist(catalog: ActionCatalog, limit: int) -> list[JsonObject]:
    """Return a deterministic shortlist spread across action types and origins."""
    if limit < 1:
        raise ValueError("shortlist limit must be positive")

    result: list[JsonObject] = []
    groups: dict[tuple[str, str], list[int]] = {}
    end_phase_ids: list[int] = []
    for action_id, action in enumerate(catalog.actions):
        if action.type == "end_phase":
            end_phase_ids.append(action_id)
            continue
        key = (action.type, action.parameters.get("origin", ""))
        groups.setdefault(key, []).append(action_id)

    for action_id in end_phase_ids:
        if len(result) >= limit:
            return result
        result.append(catalog.describe(action_id))

    active_keys = list(groups)
    while active_keys and len(result) < limit:
        next_keys: list[tuple[str, str]] = []
        for key in active_keys:
            action_ids = groups[key]
            if action_ids and len(result) < limit:
                result.append(catalog.describe(action_ids.pop(0)))
            if action_ids:
                next_keys.append(key)
        active_keys = next_keys
    return result


def _final_decision_schema(action_ids: Sequence[int]) -> JsonObject:
    if not action_ids:
        raise ValueError("structured decision requires at least one legal action")
    return {
        "type": "object",
        "additionalProperties": False,
        "required": ["action_id", "reason"],
        "properties": {
            "action_id": {
                "type": "integer",
                "enum": list(action_ids),
                "description": "One actionId from the supplied legal shortlist.",
            },
            "reason": {
                "type": "string",
                "minLength": 1,
                "maxLength": 400,
                "description": (
                    "한국어 1~3문장. 현재 보이는 상황, 즉시 목표, 이 행동을 선택한 이유를 "
                    "CMD 사용자에게 설명한다. 숨겨진 사고과정은 쓰지 않는다."
                ),
            },
        },
    }


def _structured_final_choice(
    *,
    ollama: OllamaHttpClient,
    messages: Sequence[JsonObject],
    catalog: ActionCatalog,
    decision_number: int,
) -> tuple[int, str]:
    shortlist = _balanced_shortlist(catalog, min(48, len(catalog.actions)))
    action_ids = [int(item["actionId"]) for item in shortlist]
    request_messages = [
        *messages,
        {
            "role": "user",
            "content": (
                "Final decision stage. Return only the JSON object required by the schema. Choose "
                "exactly one action_id from the legal shortlist and provide a concise Korean public "
                "commander explanation. Do not invent an action. Decision "
                f"{decision_number} legal shortlist:\n" + json.dumps(shortlist, ensure_ascii=False)
            ),
        },
    ]
    payload = _structured_chat(
        ollama=ollama,
        messages=request_messages,
        schema=_final_decision_schema(action_ids),
    )
    message = payload.get("message")
    if not isinstance(message, Mapping):
        raise OllamaError("structured response did not contain a message object")
    content = message.get("content", "")
    if isinstance(content, Mapping):
        decision: object = content
    elif isinstance(content, str):
        try:
            decision = json.loads(content)
        except json.JSONDecodeError as error:
            raise OllamaError(f"structured decision was not valid JSON: {content}") from error
    else:
        raise OllamaError("structured decision content had an unsupported type")
    if not isinstance(decision, Mapping):
        raise OllamaError("structured decision was not an object")

    action_id = decision.get("action_id")
    if isinstance(action_id, bool) or not isinstance(action_id, int):
        raise OllamaError("structured decision action_id was not an integer")
    if action_id not in action_ids:
        raise OllamaError(f"structured decision selected non-shortlisted action {action_id}")
    reason = str(decision.get("reason", "")).strip()
    if not reason:
        raise OllamaError("structured decision reason was empty")
    return action_id, reason[:400]


def _structured_chat(
    *, ollama: OllamaHttpClient, messages: Sequence[JsonObject], schema: JsonObject
) -> JsonObject:
    """Call Ollama's JSON-Schema response mode for a mandatory final decision."""
    body = json.dumps(
        {
            "model": ollama._model,
            "messages": list(messages),
            "stream": False,
            "format": schema,
            "options": {"temperature": 0},
        },
        ensure_ascii=False,
    ).encode("utf-8")
    request = urllib.request.Request(
        f"{ollama._base_url}/api/chat",
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(  # noqa: S310
            request, timeout=ollama._timeout_seconds
        ) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise OllamaError(
            f"Ollama structured choice returned HTTP {error.code}: {detail}"
        ) from error
    except (OSError, urllib.error.URLError, json.JSONDecodeError) as error:
        raise OllamaError(f"Ollama structured choice failed: {error}") from error
    if not isinstance(payload, dict):
        raise OllamaError("Ollama structured choice response was not an object")
    return payload


def _initial_decision_message(decision_number: int, catalog: ActionCatalog) -> str:
    observation = catalog.observation
    shortlist = _balanced_shortlist(catalog, min(24, len(catalog.actions)))
    payload = {
        "decision": decision_number,
        "round": observation.round,
        "player": observation.player,
        "phase": observation.phase,
        "legalActionCount": len(catalog.actions),
        "legalActionShortlist": shortlist,
    }
    return (
        "Inspect the visible situation and choose exactly one legal action. You may use tools to inspect "
        "or simulate promising candidates. Prefer simulate_action before a visible attack. Finish with "
        "execute_action or end_phase and include a concise Korean public reason. If you instead return "
        "prose, the controller will request a schema-constrained final JSON choice. Current decision:\n"
        + json.dumps(payload, ensure_ascii=False)
    )


def _repair_decision_message(catalog: ActionCatalog, attempt: int) -> str:
    shortlist = _balanced_shortlist(catalog, min(40, len(catalog.actions)))
    return (
        f"Repair attempt {attempt}: no action was executed. Choose one actionId from this balanced "
        "legal shortlist and provide a concise Korean reason. Return no prose-only answer. Legal "
        "shortlist:\n" + json.dumps(shortlist, ensure_ascii=False)
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
    print(f"Starting structured-choice Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    print("Commander explanations: enabled")
    print("Structured final choice: enabled")
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
