"""Turn-level grand strategy memory for the Small Front Ollama agent.

At the first decision of each player's turn, the model compares the current fog-filtered situation
with that player's previous-turn opening situation and previous grand strategy. It produces one
schema-constrained strategy for the new turn. Every subsequent action decision receives the latest
current situation, legal-action shortlist, completed actions for the turn, and that strategy.
"""

from __future__ import annotations

import json
import os
import shlex
from collections.abc import Mapping, Sequence
from pathlib import Path
from typing import Any

from . import local_llm_agent_purpose as purpose
from . import local_llm_agent_verified as verified
from .local_llm_agent import (
    ActionCatalog,
    JsonObject,
    JsonlLogger,
    OllamaError,
    OllamaHttpClient,
    ReplayStep,
    ShadowSimulator,
    build_parser,
    summarize_observation,
)
from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicObservation, StrategicResetRequest


class TurnStrategyLocalLlmGame(verified.VerifiedLocalLlmGame):
    """Verified action agent with separate per-player turn strategy memory."""

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
        super().__init__(
            env=env,
            ollama=ollama,
            simulator=simulator,
            max_tool_rounds=max_tool_rounds,
            default_rollouts=default_rollouts,
            max_decisions=max_decisions,
            logger=logger,
        )
        self._latest_strategy_by_player: dict[str, JsonObject] = {}
        self._latest_turn_start_by_player: dict[str, JsonObject] = {}
        self._active_turn_key: tuple[int, str] | None = None
        self._active_strategy: JsonObject = {}
        self._active_turn_history_start = 0

    def _run_model_decision(self, decision_number: int) -> None:
        catalog = ActionCatalog(self._env.raw_observation, self._env.legal_actions)
        self._ensure_turn_strategy(catalog)

        original_builder = verified._initial_verified_decision_message

        def strategy_builder(current_decision: int, current_catalog: ActionCatalog) -> str:
            completed = self._history[self._active_turn_history_start :]
            return original_builder(current_decision, current_catalog) + "\n" + _strategy_context_message(
                catalog=current_catalog,
                strategy=self._active_strategy,
                completed_steps=completed,
            )

        verified._initial_verified_decision_message = strategy_builder
        try:
            super()._run_model_decision(decision_number)
        finally:
            verified._initial_verified_decision_message = original_builder

    def _ensure_turn_strategy(self, catalog: ActionCatalog) -> None:
        observation = catalog.observation
        turn_key = (observation.round, observation.player)
        if turn_key == self._active_turn_key:
            return

        current_situation = summarize_observation(observation, len(catalog.actions))
        previous_strategy = self._latest_strategy_by_player.get(observation.player)
        previous_situation = self._latest_turn_start_by_player.get(observation.player)
        try:
            strategy = _generate_turn_strategy(
                ollama=self._ollama,
                current_situation=current_situation,
                previous_turn_strategy=previous_strategy,
                previous_turn_situation=previous_situation,
            )
            source = "ollama"
        except OllamaError as error:
            strategy = _fallback_turn_strategy(observation)
            source = "fallback"
            self._logger.write(
                "turn_strategy_error",
                {
                    "round": observation.round,
                    "player": observation.player,
                    "error": str(error),
                },
            )

        self._active_turn_key = turn_key
        self._active_strategy = strategy
        self._active_turn_history_start = len(self._history)
        self._latest_strategy_by_player[observation.player] = strategy
        self._latest_turn_start_by_player[observation.player] = current_situation

        print(
            f"[{observation.player} TURN_STRATEGY] objective: "
            f"{strategy['turnObjective']}"
        )
        print(
            f"[{observation.player} TURN_STRATEGY] assessment: "
            f"{strategy['situationAssessment']}"
        )
        priorities = strategy.get("priorityAxes", [])
        if isinstance(priorities, list) and priorities:
            print(
                f"[{observation.player} TURN_STRATEGY] priorities: "
                + " | ".join(str(item) for item in priorities)
            )
        self._logger.write(
            "turn_strategy",
            {
                "round": observation.round,
                "player": observation.player,
                "source": source,
                "currentSituation": current_situation,
                "previousTurnSituation": previous_situation,
                "previousTurnGrandStrategy": previous_strategy,
                "grandStrategy": strategy,
            },
        )


def _strategy_context_message(
    *,
    catalog: ActionCatalog,
    strategy: Mapping[str, Any],
    completed_steps: Sequence[ReplayStep],
) -> str:
    """Build the authoritative per-action context appended to the normal legal-action prompt."""
    current_situation = summarize_observation(catalog.observation, len(catalog.actions))
    completed = [
        {
            "type": step.action_type,
            "parameters": dict(step.fields),
            "automaticBattle": step.default_battle,
        }
        for step in completed_steps[-24:]
    ]
    payload = {
        "currentSituation": current_situation,
        "currentTurnGrandStrategy": dict(strategy),
        "completedActionsThisTurn": completed,
    }
    return (
        "CURRENT TURN COMMAND CONTEXT follows. CURRENT SITUATION is the only authoritative board "
        "state; all older action ids and state snapshots are expired. Follow CURRENT TURN GRAND "
        "STRATEGY unless the latest legal state makes it impossible or clearly harmful. Avoid reversing "
        "COMPLETED ACTIONS THIS TURN without a concrete new reason. Choose only a current legal action.\n"
        + json.dumps(payload, ensure_ascii=False)
    )


def _generate_turn_strategy(
    *,
    ollama: OllamaHttpClient,
    current_situation: Mapping[str, Any],
    previous_turn_strategy: Mapping[str, Any] | None,
    previous_turn_situation: Mapping[str, Any] | None,
) -> JsonObject:
    planning_input = {
        "currentSituation": dict(current_situation),
        "previousTurnGrandStrategy": (
            None if previous_turn_strategy is None else dict(previous_turn_strategy)
        ),
        "previousTurnOpeningSituation": (
            None if previous_turn_situation is None else dict(previous_turn_situation)
        ),
    }
    payload = verified._structured_chat(
        ollama=ollama,
        messages=[
            {
                "role": "system",
                "content": (
                    "You are the turn-level operational commander for one Small Front side. Create a "
                    "single coherent grand strategy for the entire current turn. Compare the current "
                    "fog-filtered situation with the same side's previous-turn opening situation and "
                    "previous grand strategy. Previous information is historical and may be stale; never "
                    "treat it as current hidden information. Preserve supply, obey destination stack "
                    "capacity, concentrate force, retain necessary reserves, avoid pointless reversals, "
                    "and identify when movement should stop. Write concise Korean strategy text."
                ),
            },
            {
                "role": "user",
                "content": (
                    "Analyze these JSON inputs and issue this turn's grand strategy. The strategy will be "
                    "inserted into every individual action decision for the rest of the turn. Return only "
                    "the schema-constrained JSON object.\n"
                    + json.dumps(planning_input, ensure_ascii=False)
                ),
            },
        ],
        schema=_turn_strategy_schema(),
    )
    message = payload.get("message")
    if not isinstance(message, Mapping):
        raise OllamaError("turn strategy response did not contain a message object")
    content = message.get("content", "")
    try:
        parsed: object = json.loads(content) if isinstance(content, str) else content
    except json.JSONDecodeError as error:
        raise OllamaError(f"turn strategy was not valid JSON: {content}") from error
    if not isinstance(parsed, Mapping):
        raise OllamaError("turn strategy was not an object")
    return _normalize_turn_strategy(parsed)


def _turn_strategy_schema() -> JsonObject:
    string_list = {
        "type": "array",
        "items": {"type": "string", "minLength": 1, "maxLength": 160},
        "maxItems": 6,
    }
    return {
        "type": "object",
        "additionalProperties": False,
        "required": [
            "situationAssessment",
            "turnObjective",
            "priorityAxes",
            "attackTargets",
            "defensiveReserves",
            "operationalRules",
            "endPhaseCriteria",
        ],
        "properties": {
            "situationAssessment": {"type": "string", "minLength": 1, "maxLength": 500},
            "turnObjective": {"type": "string", "minLength": 1, "maxLength": 240},
            "priorityAxes": {**string_list, "minItems": 1, "maxItems": 4},
            "attackTargets": {**string_list, "maxItems": 4},
            "defensiveReserves": {**string_list, "maxItems": 4},
            "operationalRules": {**string_list, "minItems": 2, "maxItems": 6},
            "endPhaseCriteria": {**string_list, "minItems": 1, "maxItems": 4},
        },
    }


def _normalize_turn_strategy(value: Mapping[str, Any]) -> JsonObject:
    scalar_keys = ("situationAssessment", "turnObjective")
    list_keys = (
        "priorityAxes",
        "attackTargets",
        "defensiveReserves",
        "operationalRules",
        "endPhaseCriteria",
    )
    result: JsonObject = {}
    for key in scalar_keys:
        text = str(value.get(key, "")).strip()
        if not text:
            raise OllamaError(f"turn strategy field {key} was empty")
        result[key] = text[:500 if key == "situationAssessment" else 240]
    for key in list_keys:
        raw = value.get(key, [])
        if not isinstance(raw, Sequence) or isinstance(raw, (str, bytes)):
            raise OllamaError(f"turn strategy field {key} was not an array")
        items = [str(item).strip()[:160] for item in raw if str(item).strip()]
        if key in {"priorityAxes", "operationalRules", "endPhaseCriteria"} and not items:
            raise OllamaError(f"turn strategy field {key} was empty")
        result[key] = items[:6]
    return result


def _fallback_turn_strategy(observation: StrategicObservation) -> JsonObject:
    enemy_targets = [
        territory.territory
        for territory in observation.territories
        if territory.visible
        and not territory.water
        and territory.owner not in {None, observation.player}
    ][:4]
    reserve_areas = [
        territory.territory
        for territory in observation.territories
        if territory.visible
        and territory.owner == observation.player
        and territory.supply_source
    ][:4]
    priority_axes = enemy_targets[:3] or reserve_areas[:3] or ["보급이 유지되는 전선"]
    return {
        "situationAssessment": (
            "현재 보이는 전선과 보급 상태를 기준으로 병력을 분산하지 않고 핵심 축에 집중해야 합니다. "
            "불확실한 지역은 제한적으로 다루고 후방의 필수 방어력을 유지합니다."
        ),
        "turnObjective": "보급을 유지하면서 핵심 전선에 전력을 집중하고 불필요한 왕복 이동을 피합니다.",
        "priorityAxes": priority_axes,
        "attackTargets": enemy_targets,
        "defensiveReserves": reserve_areas,
        "operationalRules": [
            "이미 집중한 병력을 명확한 이유 없이 다시 후방으로 돌리지 않습니다.",
            "적 통제 지역 공격은 가능한 경우 실행 전에 시뮬레이션합니다.",
            "목적지 스택과 보급을 확인하고 최소한의 예비대를 남깁니다.",
        ],
        "endPhaseCriteria": [
            "추가 행동이 대전략에 기여하지 않거나 기존 전력을 불필요하게 약화시키면 단계를 종료합니다."
        ],
    }


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
    purpose.verified._verified_action_reason = purpose._purpose_only_reason
    purpose.verified._safe_action_reason = purpose._purpose_fallback

    print(f"Starting turn-strategy Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    print("Turn grand strategy: generated per player turn")
    print("Action context: current state + legal actions + strategy + completed actions")
    print("Commander explanations: operational purpose only")
    print("Decision limit: graceful stop")
    if args.log is not None:
        print(f"Log: {Path(args.log).resolve()}")
    try:
        TurnStrategyLocalLlmGame(
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
