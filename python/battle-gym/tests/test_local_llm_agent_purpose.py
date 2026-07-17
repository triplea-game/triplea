from __future__ import annotations

from types import SimpleNamespace
from typing import Any

from triplea_battle_gym.local_llm_agent_purpose import (
    _is_safe_purpose,
    _purpose_fallback,
)


class FakeCatalog:
    def __init__(self, description: dict[str, Any]) -> None:
        self.observation = SimpleNamespace(player="Germans", round=1, phase="COMBAT_MOVE")
        self._description = description

    def describe(self, action_id: int) -> dict[str, Any]:
        assert action_id == 7
        return self._description


def test_purpose_rejects_action_names_but_accepts_korean_purpose() -> None:
    description = {
        "actionId": 7,
        "type": "move",
        "parameters": {
            "origin": "Bitburg",
            "destination": "Blankenheim",
            "unitType": "mechanized",
        },
    }
    catalog = FakeCatalog(description)

    assert _is_safe_purpose(
        "전선의 기동성과 인접 축의 대응력을 높이기 위한 이동입니다.",
        catalog,  # type: ignore[arg-type]
        description,
    )
    assert not _is_safe_purpose(
        "Bitburg에서 전력을 이동해 전선을 강화합니다.",
        catalog,  # type: ignore[arg-type]
        description,
    )


def test_reinforcement_fallback_describes_operational_purpose() -> None:
    catalog = FakeCatalog(
        {
            "actionId": 7,
            "type": "allocate_reinforcement",
            "parameters": {
                "origin": "Bitburg",
                "destination": "Blankenheim",
                "unitType": "mechanized",
            },
        }
    )

    assert _purpose_fallback(catalog, 7) == (  # type: ignore[arg-type]
        "전선의 기동성과 인접 축에 대한 대응력을 높이기 위해 증원을 배치합니다."
    )


def test_enemy_destination_fallback_explains_attack_pressure() -> None:
    catalog = FakeCatalog(
        {
            "actionId": 7,
            "type": "move",
            "parameters": {
                "origin": "Bitburg",
                "destination": "Bastogne",
                "unitType": "armour",
            },
            "destinationState": {
                "visible": True,
                "owner": "Americans",
            },
        }
    )

    assert "공격 압력" in _purpose_fallback(catalog, 7)  # type: ignore[arg-type]


def test_friendly_destination_fallback_explains_defensive_density() -> None:
    catalog = FakeCatalog(
        {
            "actionId": 7,
            "type": "move",
            "parameters": {
                "origin": "Prum",
                "destination": "Bitburg",
                "unitType": "infantry",
            },
            "destinationState": {
                "visible": True,
                "owner": "Germans",
            },
        }
    )

    assert "방어 밀도" in _purpose_fallback(catalog, 7)  # type: ignore[arg-type]
