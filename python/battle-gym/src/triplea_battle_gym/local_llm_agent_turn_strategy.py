"""Persistent operational planning for the Small Front Ollama agent.

The model is called once at the first decision of each player turn to produce a versioned,
machine-readable operational plan. Individual actions are then selected deterministically from the
current legal mask. A second planning call is allowed only when meaningful progress has already been
made and the current plan has no positive legal action.
"""

from __future__ import annotations

import json
import os
import shlex
from collections import deque
from collections.abc import Mapping, Sequence
from dataclasses import dataclass
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
    ToolSession,
    build_parser,
    summarize_observation,
)
from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicAction, StrategicObservation, StrategicResetRequest

_PLAN_SCHEMA_VERSION = 1
_OBJECTIVE_TYPES = (
    "CAPTURE",
    "HOLD",
    "PROTECT_SUPPLY",
    "GAIN_AIR_SUPERIORITY",
    "REDEPLOY_RESERVE",
    "SCREEN",
)


@dataclass(frozen=True, slots=True)
class PlannedActionChoice:
    """One deterministic action selection with auditable scoring detail."""

    action_id: int
    score: int
    aligned: bool
    reasons: tuple[str, ...]


class TurnStrategyLocalLlmGame(verified.VerifiedLocalLlmGame):
    """One-call-per-turn planner with a deterministic legal-action executor."""

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
        self._replans_this_turn = 0

    def _run_model_decision(self, decision_number: int) -> None:
        catalog = ActionCatalog(self._env.raw_observation, self._env.legal_actions)
        self._ensure_turn_strategy(catalog)
        completed = self._history[self._active_turn_history_start :]
        choice = _choose_planned_action(
            catalog=catalog,
            strategy=self._active_strategy,
            completed_steps=completed,
        )

        if self._should_replan(choice, catalog, completed):
            self._replan(catalog, completed)
            choice = _choose_planned_action(
                catalog=catalog,
                strategy=self._active_strategy,
                completed_steps=completed,
            )

        reason = purpose._purpose_fallback(catalog, choice.action_id)
        verified._print_verified_explanation(
            catalog=catalog,
            action_id=choice.action_id,
            reason=reason,
            source="operational_turn_plan",
            logger=self._logger,
        )
        session = ToolSession(
            env=self._env,
            catalog=catalog,
            history=self._history,
            simulator=self._simulator,
            default_rollouts=self._default_rollouts,
            logger=self._logger,
        )
        result = session.call(
            "execute_action",
            {"action_id": choice.action_id, "reason": reason},
        )
        self._logger.write(
            "planned_decision",
            {
                "number": decision_number,
                "planId": self._active_strategy.get("planId"),
                "actionId": choice.action_id,
                "score": choice.score,
                "aligned": choice.aligned,
                "reasons": list(choice.reasons),
                "result": result,
            },
        )
        if "error" in result:
            raise RuntimeError(f"planned action could not be executed: {result['error']}")
        if session.selected_action is not None:
            action = session.selected_action
            print(
                f"[{catalog.observation.player} {catalog.observation.phase}] executed "
                f"{action.type} {dict(action.parameters)} "
                f"(plan score {choice.score})"
            )

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
        self._replans_this_turn = 0
        self._latest_strategy_by_player[observation.player] = strategy
        self._latest_turn_start_by_player[observation.player] = current_situation
        self._report_strategy(
            observation=observation,
            strategy=strategy,
            source=source,
            current_situation=current_situation,
            previous_situation=previous_situation,
            previous_strategy=previous_strategy,
            event="turn_strategy",
        )

    def _should_replan(
        self,
        choice: PlannedActionChoice,
        catalog: ActionCatalog,
        completed_steps: Sequence[ReplayStep],
    ) -> bool:
        maximum_replans = int(self._active_strategy.get("maximumReplans", 0))
        completed_actions = sum(not step.default_battle for step in completed_steps)
        return (
            catalog.get(choice.action_id).type == "end_phase"
            and any(action.type != "end_phase" for action in catalog.actions)
            and completed_actions >= 2
            and self._replans_this_turn < maximum_replans
        )

    def _replan(
        self,
        catalog: ActionCatalog,
        completed_steps: Sequence[ReplayStep],
    ) -> None:
        observation = catalog.observation
        current_situation = summarize_observation(observation, len(catalog.actions))
        previous = self._active_strategy
        try:
            replacement = _generate_turn_strategy(
                ollama=self._ollama,
                current_situation=current_situation,
                previous_turn_strategy=self._latest_strategy_by_player.get(observation.player),
                previous_turn_situation=self._latest_turn_start_by_player.get(observation.player),
                blocked_strategy=previous,
                completed_steps=completed_steps,
            )
            source = "ollama_replan"
        except OllamaError as error:
            replacement = _fallback_turn_strategy(observation)
            source = "fallback_replan"
            self._logger.write(
                "turn_strategy_replan_error",
                {
                    "round": observation.round,
                    "player": observation.player,
                    "error": str(error),
                },
            )
        self._replans_this_turn += 1
        self._active_strategy = replacement
        self._latest_strategy_by_player[observation.player] = replacement
        self._report_strategy(
            observation=observation,
            strategy=replacement,
            source=source,
            current_situation=current_situation,
            previous_situation=self._latest_turn_start_by_player.get(observation.player),
            previous_strategy=previous,
            event="turn_strategy_replan",
        )

    def _report_strategy(
        self,
        *,
        observation: StrategicObservation,
        strategy: Mapping[str, Any],
        source: str,
        current_situation: Mapping[str, Any],
        previous_situation: Mapping[str, Any] | None,
        previous_strategy: Mapping[str, Any] | None,
        event: str,
    ) -> None:
        print(f"[{observation.player} TURN_PLAN] intent: {strategy.get('commanderIntent', '')}")
        objectives = strategy.get("objectives", [])
        if isinstance(objectives, Sequence) and not isinstance(objectives, (str, bytes)):
            rendered = [
                f"{item.get('type')}:{item.get('territoryName')}({item.get('priority')})"
                for item in objectives
                if isinstance(item, Mapping)
            ]
            if rendered:
                print(f"[{observation.player} TURN_PLAN] objectives: " + " | ".join(rendered))
        self._logger.write(
            event,
            {
                "round": observation.round,
                "player": observation.player,
                "source": source,
                "currentSituation": dict(current_situation),
                "previousTurnSituation": previous_situation,
                "previousTurnPlan": previous_strategy,
                "operationalPlan": dict(strategy),
            },
        )


def _strategy_context_message(
    *,
    catalog: ActionCatalog,
    strategy: Mapping[str, Any],
    completed_steps: Sequence[ReplayStep],
) -> str:
    """Build a diagnostic snapshot of current state, plan, and accepted actions."""

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
        "currentTurnOperationalPlan": dict(strategy),
        "completedActionsThisTurn": completed,
    }
    return (
        "CURRENT TURN OPERATIONAL CONTEXT follows. CURRENT SITUATION is the only authoritative "
        "board state; all older action ids and state snapshots are expired. The deterministic "
        "executor follows CURRENT TURN OPERATIONAL PLAN and avoids reversing COMPLETED ACTIONS "
        "THIS TURN.\n" + json.dumps(payload, ensure_ascii=False)
    )


def _generate_turn_strategy(
    *,
    ollama: OllamaHttpClient,
    current_situation: Mapping[str, Any],
    previous_turn_strategy: Mapping[str, Any] | None,
    previous_turn_situation: Mapping[str, Any] | None,
    blocked_strategy: Mapping[str, Any] | None = None,
    completed_steps: Sequence[ReplayStep] = (),
) -> JsonObject:
    planning_input = {
        "currentSituation": dict(current_situation),
        "previousTurnOperationalPlan": (
            None if previous_turn_strategy is None else dict(previous_turn_strategy)
        ),
        "previousTurnOpeningSituation": (
            None if previous_turn_situation is None else dict(previous_turn_situation)
        ),
        "blockedCurrentPlan": (None if blocked_strategy is None else dict(blocked_strategy)),
        "completedActionsThisTurn": [
            {
                "type": step.action_type,
                "parameters": dict(step.fields),
                "automaticBattle": step.default_battle,
            }
            for step in completed_steps[-24:]
        ],
    }
    payload = verified._structured_chat(
        ollama=ollama,
        messages=[
            {
                "role": "system",
                "content": (
                    "You are the turn-level operational commander for one Small Front side. "
                    "Return one executable, machine-readable plan for the whole current turn. "
                    "Use only territory names present in currentSituation.territories. Current "
                    "fog-filtered information is authoritative; previous plans and situations are "
                    "historical only. Objectives must be few, prioritized, and dependency-safe. "
                    "Use GAIN_AIR_SUPERIORITY only when air support is operationally relevant. "
                    "Protect visible supply sources and necessary front reserves. Do not encode "
                    "movement legality, scramble radius, airbase capacity, combat values, or stack "
                    "limits: TripleA's legal action mask remains authoritative. Write "
                    "commanderIntent in concise Korean."
                ),
            },
            {
                "role": "user",
                "content": (
                    "Analyze these JSON inputs and issue the current turn's operational plan. "
                    "The plan will be executed deterministically without another model call for "
                    "each action. Return only the schema-constrained JSON object.\n"
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
    return _normalize_turn_strategy(parsed, current_situation=current_situation)


def _turn_strategy_schema() -> JsonObject:
    objective = {
        "type": "object",
        "additionalProperties": False,
        "required": [
            "objectiveId",
            "type",
            "territoryName",
            "priority",
            "prerequisiteObjectiveIds",
        ],
        "properties": {
            "objectiveId": {"type": "string", "minLength": 1, "maxLength": 80},
            "type": {"type": "string", "enum": list(_OBJECTIVE_TYPES)},
            "territoryName": {"type": "string", "minLength": 1, "maxLength": 120},
            "priority": {"type": "integer", "minimum": 0, "maximum": 100},
            "prerequisiteObjectiveIds": {
                "type": "array",
                "items": {"type": "string", "minLength": 1, "maxLength": 80},
                "maxItems": 6,
            },
        },
    }
    return {
        "type": "object",
        "additionalProperties": False,
        "required": [
            "schemaVersion",
            "planId",
            "playerName",
            "round",
            "commanderIntent",
            "objectives",
            "protectedTerritories",
            "maximumReplans",
        ],
        "properties": {
            "schemaVersion": {"type": "integer", "const": _PLAN_SCHEMA_VERSION},
            "planId": {"type": "string", "minLength": 1, "maxLength": 120},
            "playerName": {"type": "string", "minLength": 1, "maxLength": 120},
            "round": {"type": "integer", "minimum": 0},
            "commanderIntent": {"type": "string", "minLength": 1, "maxLength": 500},
            "objectives": {"type": "array", "items": objective, "maxItems": 10},
            "protectedTerritories": {
                "type": "array",
                "items": {"type": "string", "minLength": 1, "maxLength": 120},
                "maxItems": 16,
            },
            "maximumReplans": {"type": "integer", "minimum": 0, "maximum": 1},
        },
    }


def _normalize_turn_strategy(
    value: Mapping[str, Any],
    *,
    current_situation: Mapping[str, Any] | None = None,
) -> JsonObject:
    schema_version = _integer(value.get("schemaVersion"), "schemaVersion")
    if schema_version != _PLAN_SCHEMA_VERSION:
        raise OllamaError(f"unsupported turn plan schema: {schema_version}")

    authoritative_player = (
        str(current_situation.get("player", "")).strip()
        if current_situation is not None
        else str(value.get("playerName", "")).strip()
    )
    authoritative_round = (
        _integer(current_situation.get("round"), "currentSituation.round")
        if current_situation is not None
        else _integer(value.get("round"), "round")
    )
    if not authoritative_player:
        raise OllamaError("turn strategy playerName was empty")
    territory_names = _territory_names(current_situation) if current_situation is not None else None

    plan_id = str(value.get("planId", "")).strip()[:120]
    commander_intent = str(value.get("commanderIntent", "")).strip()[:500]
    if not plan_id:
        raise OllamaError("turn strategy field planId was empty")
    if not commander_intent:
        raise OllamaError("turn strategy field commanderIntent was empty")

    raw_objectives = _array(value.get("objectives"), "objectives")
    objectives: list[JsonObject] = []
    objective_ids: set[str] = set()
    for index, raw in enumerate(raw_objectives):
        if not isinstance(raw, Mapping):
            raise OllamaError(f"objectives[{index}] was not an object")
        objective_id = str(raw.get("objectiveId", "")).strip()[:80]
        objective_type = str(raw.get("type", "")).strip()
        territory_name = str(raw.get("territoryName", "")).strip()[:120]
        priority = _integer(raw.get("priority"), f"objectives[{index}].priority")
        prerequisites = [
            str(item).strip()[:80]
            for item in _array(
                raw.get("prerequisiteObjectiveIds"),
                f"objectives[{index}].prerequisiteObjectiveIds",
            )
            if str(item).strip()
        ]
        if not objective_id:
            raise OllamaError(f"objectives[{index}].objectiveId was empty")
        if objective_id in objective_ids:
            raise OllamaError(f"duplicate objective id: {objective_id}")
        if objective_type not in _OBJECTIVE_TYPES:
            raise OllamaError(f"unsupported objective type: {objective_type}")
        if not territory_name:
            raise OllamaError(f"objectives[{index}].territoryName was empty")
        if territory_names is not None and territory_name not in territory_names:
            raise OllamaError(f"unknown objective territory: {territory_name}")
        if priority < 0 or priority > 100:
            raise OllamaError(f"objective priority out of range: {priority}")
        if objective_id in prerequisites:
            raise OllamaError(f"objective cannot depend on itself: {objective_id}")
        objective_ids.add(objective_id)
        objectives.append(
            {
                "objectiveId": objective_id,
                "type": objective_type,
                "territoryName": territory_name,
                "priority": priority,
                "prerequisiteObjectiveIds": sorted(set(prerequisites)),
            }
        )

    for objective in objectives:
        for prerequisite in objective["prerequisiteObjectiveIds"]:
            if prerequisite not in objective_ids:
                raise OllamaError(f"unknown prerequisite objective: {prerequisite}")
    _validate_dependency_cycles(objectives)
    objectives.sort(key=lambda item: (-int(item["priority"]), str(item["objectiveId"])))

    protected = sorted(
        {
            str(item).strip()[:120]
            for item in _array(value.get("protectedTerritories"), "protectedTerritories")
            if str(item).strip()
        }
    )
    if territory_names is not None:
        unknown_protected = [name for name in protected if name not in territory_names]
        if unknown_protected:
            raise OllamaError(f"unknown protected territory: {unknown_protected[0]}")

    maximum_replans = _integer(value.get("maximumReplans"), "maximumReplans")
    if maximum_replans < 0 or maximum_replans > 1:
        raise OllamaError("maximumReplans must be between 0 and 1")
    return {
        "schemaVersion": _PLAN_SCHEMA_VERSION,
        "planId": plan_id,
        "playerName": authoritative_player,
        "round": authoritative_round,
        "commanderIntent": commander_intent,
        "objectives": objectives,
        "protectedTerritories": protected,
        "maximumReplans": maximum_replans,
    }


def _fallback_turn_strategy(observation: StrategicObservation) -> JsonObject:
    player = observation.player
    visible_enemy = sorted(
        (
            territory
            for territory in observation.territories
            if territory.visible and not territory.water and territory.owner not in {None, player}
        ),
        key=lambda territory: territory.territory,
    )
    supply_sources = sorted(
        (
            territory
            for territory in observation.territories
            if territory.visible and territory.owner == player and territory.supply_source
        ),
        key=lambda territory: territory.territory,
    )
    has_friendly_air = any(
        unit.owner == player and unit.air and unit.count > 0
        for territory in observation.territories
        for unit in territory.units
    )
    objectives: list[JsonObject] = []
    primary_name = visible_enemy[0].territory if visible_enemy else "current-front"
    for index, territory in enumerate(visible_enemy[:3]):
        dependencies: list[str] = []
        enemy_ground = any(
            unit.owner != player and unit.land and unit.count > 0 for unit in territory.units
        )
        friendly_control = (
            territory.air_control_status == "controlled" and territory.air_control_player == player
        )
        if index == 0 and has_friendly_air and enemy_ground and not friendly_control:
            air_id = f"gain-air-{_slug(territory.territory)}"
            objectives.append(
                {
                    "objectiveId": air_id,
                    "type": "GAIN_AIR_SUPERIORITY",
                    "territoryName": territory.territory,
                    "priority": 100,
                    "prerequisiteObjectiveIds": [],
                }
            )
            dependencies.append(air_id)
        objectives.append(
            {
                "objectiveId": f"capture-{_slug(territory.territory)}",
                "type": "CAPTURE",
                "territoryName": territory.territory,
                "priority": 95 if index == 0 else 60,
                "prerequisiteObjectiveIds": dependencies,
            }
        )

    for territory in supply_sources:
        objectives.append(
            {
                "objectiveId": f"protect-supply-{_slug(territory.territory)}",
                "type": "PROTECT_SUPPLY",
                "territoryName": territory.territory,
                "priority": 85,
                "prerequisiteObjectiveIds": [],
            }
        )

    raw: JsonObject = {
        "schemaVersion": _PLAN_SCHEMA_VERSION,
        "planId": f"{_slug(player)}-r{observation.round}-{_slug(primary_name)}",
        "playerName": player,
        "round": observation.round,
        "commanderIntent": (
            "가시 전선의 주공 목표에 전력을 집중하되 보급 거점과 필수 예비대를 유지하고 "
            "계획에 기여하지 않는 왕복 이동은 중단합니다."
        ),
        "objectives": objectives,
        "protectedTerritories": [territory.territory for territory in supply_sources],
        "maximumReplans": 1,
    }
    return _normalize_turn_strategy(
        raw,
        current_situation=summarize_observation(observation, 0),
    )


def _choose_planned_action(
    *,
    catalog: ActionCatalog,
    strategy: Mapping[str, Any],
    completed_steps: Sequence[ReplayStep],
) -> PlannedActionChoice:
    end_phase_id = catalog.end_phase_id()
    best = PlannedActionChoice(
        action_id=end_phase_id,
        score=0,
        aligned=False,
        reasons=("계획에 기여하는 양의 점수 행동이 없어 단계를 종료합니다.",),
    )
    for action_id, action in enumerate(catalog.actions):
        if action.type == "end_phase":
            continue
        choice = _score_planned_action(
            action_id=action_id,
            action=action,
            catalog=catalog,
            strategy=strategy,
            completed_steps=completed_steps,
        )
        if choice.score > best.score or (
            choice.score == best.score and choice.score > 0 and choice.action_id < best.action_id
        ):
            best = choice
    return best


def _score_planned_action(
    *,
    action_id: int,
    action: StrategicAction,
    catalog: ActionCatalog,
    strategy: Mapping[str, Any],
    completed_steps: Sequence[ReplayStep],
) -> PlannedActionChoice:
    observation = catalog.observation
    territories = {territory.territory: territory for territory in observation.territories}
    completed_objectives = _completed_objective_ids(strategy, observation)
    objectives = [
        objective
        for objective in _objectives(strategy)
        if _is_actionable(objective, completed_objectives)
    ]
    parameters = action.parameters
    destination = parameters.get("destination", "")
    origin = parameters.get("origin", "")
    score = 0
    aligned = False
    reasons: list[str] = []

    for objective in objectives:
        if objective["territoryName"] != destination:
            continue
        contribution = _direct_contribution(action, objective)
        if contribution > 0:
            score += contribution
            aligned = True
            reasons.append(f"{objective['objectiveId']} 직접 기여 +{contribution}")

    primary = next(
        (
            objective
            for objective in _objectives(strategy)
            if objective["objectiveId"] not in completed_objectives
            and set(objective["prerequisiteObjectiveIds"]).issubset(completed_objectives)
        ),
        None,
    )
    if primary is not None and action.type != "air_assignment":
        before = _distance(territories, origin, str(primary["territoryName"]))
        after = _distance(territories, destination, str(primary["territoryName"]))
        if before is not None and after is not None:
            progress = before - after
            progress_score = 18 * progress
            score += progress_score
            aligned = aligned or progress > 0
            if progress_score:
                reasons.append(f"주 목표 거리 변화 {progress_score:+d}")

    protected = {str(item) for item in strategy.get("protectedTerritories", []) if str(item)}
    if origin in protected and _empties_friendly_land(action, observation):
        score -= 120
        reasons.append("보호 지역의 지상 예비대 소진 -120")
    if destination in protected:
        score += 12
        reasons.append("보호 지역 증원 +12")

    if _is_immediate_reversal(action, completed_steps):
        score -= 100
        reasons.append("직전 이동 역전 -100")
    if parameters.get("uncertain") == "true":
        score -= 25
        reasons.append("불확실 지역 진입 -25")

    destination_state = territories.get(destination)
    if destination_state is not None and destination_state.visible:
        if _action_is_land(action, observation) and destination_state.supplied is False:
            score -= 80
            reasons.append("비보급 목적지 -80")
        if action.type != "air_assignment" and destination_state.owner not in {
            None,
            observation.player,
        }:
            score += 25
            reasons.append("가시 적 통제 지역 압박 +25")
        if action.type == "air_assignment" and not (
            destination_state.air_control_status == "controlled"
            and destination_state.air_control_player == observation.player
        ):
            score += 20
            reasons.append("제공권 개선 +20")

    if completed_steps and not aligned:
        score -= 20
        reasons.append("활성 목표 비정합 -20")
    return PlannedActionChoice(
        action_id=action_id,
        score=score,
        aligned=aligned,
        reasons=tuple(reasons) or ("기본 점수 0",),
    )


def _objectives(strategy: Mapping[str, Any]) -> list[Mapping[str, Any]]:
    raw = strategy.get("objectives", [])
    if not isinstance(raw, Sequence) or isinstance(raw, (str, bytes)):
        return []
    return [item for item in raw if isinstance(item, Mapping)]


def _completed_objective_ids(
    strategy: Mapping[str, Any],
    observation: StrategicObservation,
) -> set[str]:
    territories = {territory.territory: territory for territory in observation.territories}
    completed: set[str] = set()
    for objective in _objectives(strategy):
        territory = territories.get(str(objective.get("territoryName", "")))
        if territory is None or not territory.visible:
            continue
        objective_type = str(objective.get("type", ""))
        done = False
        if objective_type in {"CAPTURE", "HOLD"}:
            done = territory.owner == observation.player
        elif objective_type == "PROTECT_SUPPLY":
            done = territory.owner == observation.player and territory.supplied is not False
        elif objective_type == "GAIN_AIR_SUPERIORITY":
            done = (
                territory.air_control_status == "controlled"
                and territory.air_control_player == observation.player
            )
        elif objective_type in {"REDEPLOY_RESERVE", "SCREEN"}:
            done = any(
                unit.owner == observation.player and unit.land and unit.count > 0
                for unit in territory.units
            )
        if done:
            completed.add(str(objective.get("objectiveId", "")))
    return completed


def _is_actionable(
    objective: Mapping[str, Any],
    completed_objectives: set[str],
) -> bool:
    objective_id = str(objective.get("objectiveId", ""))
    if objective_id in completed_objectives:
        return False
    prerequisites = {str(item) for item in objective.get("prerequisiteObjectiveIds", [])}
    return objective.get("type") == "CAPTURE" or prerequisites.issubset(completed_objectives)


def _direct_contribution(
    action: StrategicAction,
    objective: Mapping[str, Any],
) -> int:
    priority = int(objective.get("priority", 0))
    objective_type = str(objective.get("type", ""))
    if objective_type == "GAIN_AIR_SUPERIORITY":
        return 2 * priority if action.type == "air_assignment" else 0
    if objective_type == "CAPTURE":
        return priority // 2 if action.type == "air_assignment" else priority
    if action.type == "air_assignment":
        return 0
    return priority


def _distance(
    territories: Mapping[str, Any],
    origin: str,
    destination: str,
) -> int | None:
    if origin not in territories or destination not in territories:
        return None
    queue: deque[tuple[str, int]] = deque([(origin, 0)])
    visited = {origin}
    while queue:
        current, distance = queue.popleft()
        if current == destination:
            return distance
        territory = territories[current]
        for neighbor in territory.neighbors:
            if neighbor in territories and neighbor not in visited:
                visited.add(neighbor)
                queue.append((neighbor, distance + 1))
    return None


def _action_is_land(
    action: StrategicAction,
    observation: StrategicObservation,
) -> bool:
    if action.type == "air_assignment":
        return False
    origin = action.parameters.get("origin", "")
    unit_type = action.parameters.get("unitType", "")
    for territory in observation.territories:
        if territory.territory != origin:
            continue
        return any(
            unit.owner == observation.player and unit.unit_type == unit_type and unit.land
            for unit in territory.units
        )
    return False


def _empties_friendly_land(
    action: StrategicAction,
    observation: StrategicObservation,
) -> bool:
    if not _action_is_land(action, observation):
        return False
    origin = action.parameters.get("origin", "")
    moving_count = _action_unit_count(action)
    for territory in observation.territories:
        if territory.territory != origin:
            continue
        friendly_land = sum(
            unit.count for unit in territory.units if unit.owner == observation.player and unit.land
        )
        return friendly_land > 0 and moving_count >= friendly_land
    return False


def _action_unit_count(action: StrategicAction) -> int:
    encoded = action.parameters.get("unitIds", "")
    if encoded:
        return len([item for item in encoded.split(",") if item])
    try:
        return int(action.parameters.get("unitCount", "1"))
    except ValueError:
        return 1


def _is_immediate_reversal(
    action: StrategicAction,
    completed_steps: Sequence[ReplayStep],
) -> bool:
    origin = action.parameters.get("origin", "")
    destination = action.parameters.get("destination", "")
    unit_type = action.parameters.get("unitType", "")
    if not origin or not destination:
        return False
    for step in reversed(completed_steps[-24:]):
        if step.default_battle:
            continue
        fields = dict(step.fields)
        return (
            fields.get("origin") == destination
            and fields.get("destination") == origin
            and fields.get("unitType", "") == unit_type
        )
    return False


def _territory_names(current_situation: Mapping[str, Any]) -> set[str]:
    territories = current_situation.get("territories", [])
    if not isinstance(territories, Sequence) or isinstance(territories, (str, bytes)):
        raise OllamaError("currentSituation.territories was not an array")
    names = {
        str(item.get("name", "")).strip()
        for item in territories
        if isinstance(item, Mapping) and str(item.get("name", "")).strip()
    }
    if not names:
        raise OllamaError("currentSituation contained no territory names")
    return names


def _validate_dependency_cycles(objectives: Sequence[Mapping[str, Any]]) -> None:
    graph = {
        str(objective["objectiveId"]): {str(item) for item in objective["prerequisiteObjectiveIds"]}
        for objective in objectives
    }
    visiting: set[str] = set()
    visited: set[str] = set()

    def visit(objective_id: str) -> None:
        if objective_id in visited:
            return
        if objective_id in visiting:
            raise OllamaError(f"objective prerequisite cycle includes: {objective_id}")
        visiting.add(objective_id)
        for prerequisite in graph[objective_id]:
            visit(prerequisite)
        visiting.remove(objective_id)
        visited.add(objective_id)

    for objective_id in graph:
        visit(objective_id)


def _array(value: Any, field: str) -> Sequence[Any]:
    if not isinstance(value, Sequence) or isinstance(value, (str, bytes)):
        raise OllamaError(f"turn strategy field {field} was not an array")
    return value


def _integer(value: Any, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int):
        raise OllamaError(f"turn strategy field {field} was not an integer")
    return value


def _slug(value: str) -> str:
    normalized = "".join(character.lower() if character.isalnum() else "-" for character in value)
    return "-".join(part for part in normalized.split("-") if part) or "front"


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

    print(f"Starting operational-plan Small Front local LLM self-play with {args.model}")
    print(f"Scenario: {scenario}")
    print("Turn plan: one structured Ollama call per player turn")
    print("Action selection: deterministic scoring over the current legal mask")
    print("Blocked plan: at most one additional planning call after meaningful progress")
    print("Commander explanations: deterministic operational purpose")
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
