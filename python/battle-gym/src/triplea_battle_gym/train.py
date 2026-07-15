"""Train a Maskable PPO baseline against the TripleA battle environment."""

from __future__ import annotations

import argparse
import shlex
from pathlib import Path
from typing import Any

from .env import TripleABattleEnv
from .models import BattleResetRequest


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument("--scenario", type=Path, required=True)
    parser.add_argument("--battle-id")
    parser.add_argument("--territory")
    parser.add_argument("--timesteps", type=int, default=100_000)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--max-actions", type=int, default=4096)
    parser.add_argument("--output", type=Path, default=Path("battle-policy.zip"))
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.timesteps <= 0:
        raise SystemExit("--timesteps must be positive")
    try:
        from sb3_contrib import MaskablePPO
        from sb3_contrib.common.wrappers import ActionMasker
    except ImportError as error:
        raise SystemExit(
            "training dependencies are missing; install triplea-battle-gym[train]"
        ) from error

    env = TripleABattleEnv(
        server_command=tuple(shlex.split(args.server_command)),
        reset_request=BattleResetRequest(
            scenario_path=str(args.scenario),
            seed=args.seed,
            battle_id=args.battle_id,
            territory=args.territory,
        ),
        max_actions=args.max_actions,
    )

    def mask(environment: Any) -> Any:
        return environment.action_masks()

    wrapped = ActionMasker(env, mask)
    try:
        model = MaskablePPO("MultiInputPolicy", wrapped, verbose=1, seed=args.seed)
        model.learn(total_timesteps=args.timesteps)
        model.save(str(args.output))
    finally:
        wrapped.close()


if __name__ == "__main__":
    main()
