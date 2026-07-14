from __future__ import annotations

from triplea_battle_gym.client import BattleClient
from triplea_battle_gym.models import BattleAction, BattleResetRequest


def test_typed_reset_step_and_replay_commands(client: BattleClient) -> None:
    assert client.ping()["schemaVersion"] == 4
    observation = client.reset(BattleResetRequest("fixture.tsvg", 7))
    assert observation.territory == "Test Territory"
    assert observation.air_control_player == "attacker"
    assert observation.offense_ground_attack_bonus == 1
    actions = client.legal_actions()
    assert [action.type for action in actions] == ["continue", "retreat"]

    result = client.step(BattleAction("continue"))
    assert result.terminated
    assert result.reward == 1.25
    assert client.episode_log()["logSchemaVersion"] == 1
    assert client.replay({"logSchemaVersion": 1})["matched"]
    assert client.batch([{"logSchemaVersion": 1}], parallelism=1)["matchedEpisodes"] == 1
