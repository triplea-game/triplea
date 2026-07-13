from __future__ import annotations

import sys
from pathlib import Path

import pytest

from triplea_battle_gym.client import BattleClient

FAKE_SERVER = Path(__file__).with_name("fake_server.py")


@pytest.fixture
def server_command() -> tuple[str, ...]:
    return (sys.executable, str(FAKE_SERVER))


@pytest.fixture
def client(server_command: tuple[str, ...]) -> BattleClient:
    value = BattleClient(server_command)
    yield value
    value.close()
