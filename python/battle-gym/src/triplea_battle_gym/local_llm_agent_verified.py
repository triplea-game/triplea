"""Verified-action wrapper for the Small Front Ollama agent.

The action selector may inspect, simulate, call a terminal tool, or use the structured final-choice
fallback. After an action id is fixed, a separate schema-constrained Ollama request explains only that
exact engine action. The console always prints a deterministic action fact before the model rationale,
so an explanation cannot silently describe a different side, unit, origin, or destination.
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
    JsonlLogger,
    JsonObject,
    OllamaError,
    OllamaHttpClient,
    ReplayStep,
    ShadowSimulator,
    ToolSession,
    _fallback_action,
    _parse_tool_call,
    build_parser,
    summarize_observation,
)
from .local_llm_agent_reliable import (
    COMMENTED_TOOLS,
    RELIABLE_SYSTEM_PROMPT,
    ReliableLocalLlmGame,
    _public_reason,
    _structured_chat,
    _structured_final_choice,
)
from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest

_EXECUTION_TOOL_NAMES = {"execute_action", "end_phase"}

VERIFIED_SYSTEM_PROMPT = (
    RELIABLE_SYSTEM_PROMPT
    + "\nThe controller verifies every terminal explanation after the action id is fixed. Your draft "
    + "reason may express intent, but the final console explanation is regenerated from the exact "
    + "engine action. Never describe another side, origin, destination, unit type, or quantity."
)


class VerifiedLocalLlmGame(ReliableLocalLlmGame):
    """Tool-using game loop with exact-action explanations and graceful decision limits."""

    def run(self) -> None:
        self._env.reset()
        for decision_number in range(1, self._max_decisions + 1):
            observation = self._env.raw_observation
            if observation.over:
                self._report_game_over(decision_number - 1)
                return
            if observation.phase == "BATTLE":
                self._execute_default_battle()
                continue
            self._run_model_decision(decision_number)

        observation = self._env.raw_observation
        if observation.over:
            self._report_game_over(self._max_decisions)
            return
        print(
            f"Stopped normally after configured maximum {self._max_decisions} decisions "
            f"at round {observation.round}, player {observation.player}, phase {observation.phase}."
        )
        self._logger.write(
            "decision_limit",
            {
                "limit": self._max_decisions,
                "round": observation.round,
                "player": observation.player,
                "phase": observation.phase,
            },
        )

    def _report_game_over(self, actions: int) -> None:
        observation = self._env.raw_observation
        print(f"Game over at round {observation.round} after {actions} decisions.")
        self._logger.write("game_over", {"round": observation.round, "actions": actions})

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
            {"role": "system", "content": VERIFIED_SYSTEM_PROMPT},
            {
                "role": "user",
                "content": _initial_verified_decision_message(decision_number, catalog),
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
                        action_id = _terminal_action_id(name, arguments, catalog)
                        draft_reason = _public_reason(arguments)
                        reason = _verified_action_reason(
                            ollama=self._ollama,
                            catalog=catalog,
                            action_id=action_id,
                            draft_reason=draft_reason,
                        )
                        _print_verified_explanation(
                            catalog=catalog,
                            action_id=action_id,
                            reason=reason,
                            source=name,
                            logger=self._logger,
                        )
                        result = session.call(
                            "execute_action", {"action_id": action_id, "reason": reason}
                        )
                    else:
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
                action_id, draft_reason = _structured_final_choice(
                    ollama=self._ollama,
                    messages=messages,
                    catalog=catalog,
                    decision_number=decision_number,
                )
                print(
                    f"[{observation.player} {observation.phase}] structured-choice: action {action_id}"
                )
                reason = _verified_action_reason(
                    ollama=self._ollama,
                    catalog=catalog,
                    action_id=action_id,
                    draft_reason=draft_reason,
                )
                _print_verified_explanation(
                    catalog=catalog,
                    action_id=action_id,
                    reason=reason,
                    source="structured_choice",
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
            fallback_reason = _safe_action_reason(catalog, fallback)
            _print_verified_explanation(
                catalog=catalog,
                action_id=fallback,
                reason=(
                    fallback_reason
                    + " 모델의 선택 또는 설명 검증이 실패해 휴리스틱 안전장치가 선택했습니다."
                ),
                source="fallback",
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


def _terminal_action_id(name: str, arguments: Mapping[str, Any], catalog: ActionCatalog) -> int:
    if name == "end_phase":
        return catalog.end_phase_id()
    value = arguments.get("action_id")
    if isinstance(value, bool) or not isinstance(value, int):
        raise OllamaError("execute_action did not provide an integer action_id")
    catalog.get(value)
    return value


def _verified_action_reason(
    *,
    ollama: OllamaHttpClient,
    catalog: ActionCatalog,
    action_id: int,
    draft_reason: str,
) -> str:
    action = catalog.describe(action_id)
    observation = catalog.observation
    context = {
        "actingPlayer": observation.player,
        "round": observation.round,
        "phase": observation.phase,
        "fixedAction": action,
        "draftIntent": draft_reason,
    }
    schema: JsonObject = {
        "type": "object",
        "additionalProperties": False,
        "required": ["reason"],
        "properties": {
            "reason": {
                "type": "string",
                "minLength": 1,
                "maxLength": 300,
                "description": (
                    "한국어 1~2문장으로 고정된 행동의 작전 이유만 설명한다. 행동의 플레이어, "
                    "병종, 수량, 출발지, 목적지, 단계는 바꾸거나 추측하지 않는다."
                ),
            }
        },
    }
    payload = _structured_chat(
        ollama=ollama,
        messages=[
            {
                "role": "system",
                "content": (
                    "You explain a fixed TripleA action; you do not choose or modify it. Write Korean "
                    "only. Use the exact acting player, phase, unit type, quantity, origin and destination "
                    "from fixedAction. Do not mention a different move, side, territory or unit. Explain "
                    "the immediate operational purpose in one or two concise sentences. If the visible "
                    "data is insufficient, state a conservative purpose without inventing facts."
                ),
            },
            {
                "role": "user",
                "content": "Explain this exact fixed action:\n"
                + json.dumps(context, ensure_ascii=False),
            },
        ],
        schema=schema,
    )
    message = payload.get("message")
    if not isinstance(message, Mapping):
        return _safe_action_reason(catalog, action_id)
    content = message.get("content", "")
    try:
        parsed: object = json.loads(content) if isinstance(content, str) else content
    except json.JSONDecodeError:
        return _safe_action_reason(catalog, action_id)
    if not isinstance(parsed, Mapping):
        return _safe_action_reason(catalog, action_id)
    reason = str(parsed.get("reason", "")).strip()[:300]
    if not reason or not _reason_matches_action(reason, action):
        return _safe_action_reason(catalog, action_id)
    return reason


def _reason_matches_action(reason: str, action: Mapping[str, Any]) -> bool:
    parameters = action.get("parameters")
    if not isinstance(parameters, Mapping):
        return False
    action_type = str(action.get("type", ""))
    if action_type == "end_phase":
        phase = str(parameters.get("phase", ""))
        return not phase or phase in reason
    origin = str(parameters.get("origin", ""))
    destination = str(parameters.get("destination", ""))
    return bool(origin and destination and origin in reason and destination in reason)


def _action_fact(catalog: ActionCatalog, action_id: int) -> str:
    description = catalog.describe(action_id)
    action_type = str(description.get("type", ""))
    parameters = description.get("parameters")
    if not isinstance(parameters, Mapping):
        parameters = {}
    player = catalog.observation.player
    phase = catalog.observation.phase
    if action_type == "end_phase":
        return f"{player}가 {phase} 단계를 종료합니다."

    origin = str(parameters.get("origin", "?"))
    destination = str(parameters.get("destination", "?"))
    unit_type = str(parameters.get("unitType", "unit"))
    count = int(description.get("unitCount", 1))
    if action_type == "allocate_reinforcement":
        return f"{player}의 증원 {unit_type} {count}개를 {origin}에서 {destination}에 배치합니다."
    if action_type == "air_assignment":
        return f"{player}의 {unit_type} {count}개를 {origin}에서 {destination}로 항공 배치합니다."
    return f"{player}의 {unit_type} {count}개를 {origin}에서 {destination}로 이동합니다."


def _safe_action_reason(catalog: ActionCatalog, action_id: int) -> str:
    description = catalog.describe(action_id)
    action_type = str(description.get("type", ""))
    parameters = description.get("parameters")
    if not isinstance(parameters, Mapping):
        parameters = {}
    if action_type == "end_phase":
        phase = str(parameters.get("phase", catalog.observation.phase))
        return f"{phase}에서 더 실행할 이동의 이득이 제한적이므로 현재 배치를 확정하고 단계를 종료합니다."
    origin = str(parameters.get("origin", "출발지"))
    destination = str(parameters.get("destination", "목적지"))
    return (
        f"{origin}에서 {destination}로의 이 합법 행동은 현재 배치를 조정하기 위한 선택입니다. "
        "TripleA 엔진이 이동력, 보급 및 목적지 스택 제한을 다시 검증한 뒤 실행합니다."
    )


def _print_verified_explanation(
    *,
    catalog: ActionCatalog,
    action_id: int,
    reason: str,
    source: str,
    logger: JsonlLogger,
) -> None:
    player = catalog.observation.player
    phase = catalog.observation.phase
    fact = _action_fact(catalog, action_id)
    print(f"[{player} {phase}] action: {fact}")
    print(f"[{player} {phase}] commander: {reason}")
    logger.write(
        "decision_explanation",
        {
            "source": source,
            "actionId": action_id,
            "actionFact": fact,
            "reason": reason,
        },
    )


def _initial_verified_decision_message(decision_number: int, catalog: ActionCatalog) -> str:
    from .local_llm_agent_reliable import _initial_decision_message

    return (
        _initial_decision_message(decision_number, catalog)
        + "\nThe controller will regenerate the public explanation after your action id is fixed, using "
        + "the exact engine action. Focus on selecting the action; do not describe a different move."
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
    print(f"Starting verified-action Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    print("Commander explanations: verified against exact action")
    print("Decision limit: graceful stop")
    if args.log is not None:
        print(f"Log: {Path(args.log).resolve()}")
    try:
        VerifiedLocalLlmGame(
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
