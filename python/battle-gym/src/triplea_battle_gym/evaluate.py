"""Command-line evaluation for random and scripted battle policies."""

from __future__ import annotations

import argparse
import json
import random
import shlex
from pathlib import Path

from .env import TripleABattleEnv
from .models import BattleResetRequest
from .policies import random_action_index, scripted_action_index


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument("--scenario", type=Path, required=True)
    parser.add_argument("--battle-id")
    parser.add_argument("--territory")
    parser.add_argument("--episodes", type=int, default=10)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--max-steps", type=int, default=1000)
    parser.add_argument("--policy", choices=("random", "scripted"), default="scripted")
    parser.add_argument("--max-actions", type=int, default=4096)
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.episodes <= 0:
        raise SystemExit("--episodes must be positive")
    rng = random.Random(args.seed)
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
    rewards: list[float] = []
    terminations = 0
    truncations = 0
    try:
        for episode in range(args.episodes):
            _, _ = env.reset(seed=args.seed + episode)
            total_reward = 0.0
            for _step in range(args.max_steps):
                if args.policy == "random":
                    action = random_action_index(env.legal_actions, rng)
                else:
                    action = scripted_action_index(env.legal_actions)
                _, reward, terminated, truncated, _ = env.step(action)
                total_reward += reward
                if terminated or truncated:
                    terminations += int(terminated)
                    truncations += int(truncated)
                    break
            else:
                truncations += 1
            rewards.append(total_reward)
    finally:
        env.close()

    summary = {
        "episodes": args.episodes,
        "policy": args.policy,
        "meanReward": sum(rewards) / len(rewards),
        "minimumReward": min(rewards),
        "maximumReward": max(rewards),
        "terminatedEpisodes": terminations,
        "truncatedEpisodes": truncations,
    }
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    main()
