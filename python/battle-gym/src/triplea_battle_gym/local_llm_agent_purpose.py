"""Purpose-only console explanations for the verified Small Front Ollama agent.

The exact action fact is already printed from TripleA's legal action object. This wrapper therefore
asks the model to explain only the operational purpose, without repeating or inventing players,
territories, unit types, quantities, routes, or phase names. Invalid output falls back to a concise,
deterministic purpose matched to the selected action.
"""

from __future__ import annotations

import json
import re
from collections.abc import Mapping
from typing import Any

from . import local_llm_agent_verified as verified
from .local_llm_agent import ActionCatalog, JsonObject, OllamaHttpClient

_ASCII_WORD = re.compile(r"[A-Za-z]")


def _purpose_only_reason(
    *,
    ollama: OllamaHttpClient,
    catalog: ActionCatalog,
    action_id: int,
    draft_reason: str,
) -> str:
    """Generate a public rationale that cannot contradict the exact action fact line."""
    action = catalog.describe(action_id)
    context = {
        "round": catalog.observation.round,
        "phase": catalog.observation.phase,
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
                "maxLength": 240,
                "description": (
                    "한국어 1~2문장으로 이 행동의 작전 목적만 설명한다. 플레이어명, 지역명, "
                    "병종명, 수량, 경로, 단계명은 쓰지 않는다. 숨겨진 사고과정은 쓰지 않는다."
                ),
            }
        },
    }
    payload = verified._structured_chat(
        ollama=ollama,
        messages=[
            {
                "role": "system",
                "content": (
                    "You explain only the operational purpose of a fixed TripleA action. Write Korean "
                    "only. Do not repeat or introduce any player name, territory name, unit type, number, "
                    "route, or phase name; the console prints those facts separately. Give one or two "
                    "concise sentences about purposes such as concentrating force, reinforcing a front, "
                    "maintaining supply, improving mobility, preserving reserves, or ending an exhausted "
                    "phase. Do not reveal hidden chain-of-thought."
                ),
            },
            {
                "role": "user",
                "content": (
                    "Explain only the operational purpose of this already-fixed action. Do not restate "
                    "the action facts:\n" + json.dumps(context, ensure_ascii=False)
                ),
            },
        ],
        schema=schema,
    )
    message = payload.get("message")
    if not isinstance(message, Mapping):
        return _purpose_fallback(catalog, action_id)
    content = message.get("content", "")
    try:
        parsed: object = json.loads(content) if isinstance(content, str) else content
    except json.JSONDecodeError:
        return _purpose_fallback(catalog, action_id)
    if not isinstance(parsed, Mapping):
        return _purpose_fallback(catalog, action_id)
    reason = str(parsed.get("reason", "")).strip()[:240]
    if not _is_safe_purpose(reason, catalog, action):
        return _purpose_fallback(catalog, action_id)
    return reason


def _is_safe_purpose(
    reason: str,
    catalog: ActionCatalog,
    action: Mapping[str, Any],
) -> bool:
    if not reason or _ASCII_WORD.search(reason):
        return False
    parameters = action.get("parameters")
    if isinstance(parameters, Mapping):
        forbidden = {
            str(parameters.get("origin", "")),
            str(parameters.get("destination", "")),
            str(parameters.get("unitType", "")),
            str(parameters.get("phase", "")),
            str(catalog.observation.player),
        }
        lowered = reason.casefold()
        if any(item and item.casefold() in lowered for item in forbidden):
            return False
    return True


def _purpose_fallback(catalog: ActionCatalog, action_id: int) -> str:
    description = catalog.describe(action_id)
    action_type = str(description.get("type", ""))
    parameters = description.get("parameters")
    if not isinstance(parameters, Mapping):
        parameters = {}

    if action_type == "end_phase":
        return "추가 행동의 기대 이득이 제한적이므로 현재 배치를 확정하고 다음 절차로 넘어갑니다."
    if action_type == "allocate_reinforcement":
        return "전선의 기동성과 인접 축에 대한 대응력을 높이기 위해 증원을 배치합니다."
    if action_type == "air_assignment":
        return "전투 지원 범위를 조정하고 필요한 축에 항공 전력을 집중하기 위한 배치입니다."

    destination_state = description.get("destinationState")
    if isinstance(destination_state, Mapping) and destination_state.get("visible"):
        owner = destination_state.get("owner")
        if owner and owner != catalog.observation.player:
            return "적 통제 지역에 전력을 집중해 공격 압력을 높이고 전선의 주도권을 확보하려는 이동입니다."
        if owner == catalog.observation.player:
            return "우군 통제 지역으로 전력을 재배치해 방어 밀도와 인접 전선의 대응력을 높이려는 이동입니다."

    if parameters.get("uncertain") == "true":
        return "불확실한 축의 상황을 확인하면서 후속 작전 선택지를 넓히기 위한 제한적 이동입니다."
    return "전력을 재배치해 전선 연결성과 후속 작전 선택지를 넓히기 위한 이동입니다."


def main() -> None:
    verified._verified_action_reason = _purpose_only_reason
    verified._safe_action_reason = _purpose_fallback
    verified.main()


if __name__ == "__main__":
    main()
