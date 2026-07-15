from __future__ import annotations

from triplea_battle_gym.models import BattleResetRequest
from triplea_battle_gym.worker import BattleWorkerPool, BattleWorkerSpec


def test_worker_pool_preserves_input_order(server_command: tuple[str, ...]) -> None:
    specs = tuple(
        BattleWorkerSpec(
            server_command=server_command,
            reset_request=BattleResetRequest("fixture.tsvg", seed),
            max_unit_groups_per_side=4,
            max_actions=8,
        )
        for seed in (1, 2)
    )

    with BattleWorkerPool(specs) as pool:
        resets = pool.reset((101, 102))
        assert [item[1]["territory"] for item in resets] == [
            "Test Territory",
            "Test Territory",
        ]
        steps = pool.step((0, 1))
        assert [item[1] for item in steps] == [1.25, 1.25]
        assert all(item[2] for item in steps)
