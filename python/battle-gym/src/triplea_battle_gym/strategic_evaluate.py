"""Evaluate a strategic learner side against the deterministic scripted opponent."""

from __future__ import annotations

import argparse
import json
import random
import shlex
from pathlib import Path
from typing import Any

import numpy as np

from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest
from .strategic_training_env import SingleSideStrategicEnv, scripted_opponent_action_index


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument("--scenario", type=Path, required=True)
    parser.add_argument("--learner-player", required=True)
    parser.add_argument("--episodes", type=int, default=20)
    parser.add_argument("--seed", type=int, default=10_001)
    parser.add_argument("--max-steps", type=int, default=10_000)
    parser.add_argument("--max-actions", type=int, default=2048)
    parser.add_argument("--max-territories", type=int, default=64)
    parser.add_argument("--max-rounds", type=int, default=12)
    parser.add_argument(
        "--policy",
        choices=("model", "scripted", "random"),
        default="model",
        help="learner policy; the opponent is always the deterministic scripted baseline",
    )
    parser.add_argument("--model", type=Path, help="MaskablePPO model used by --policy model")
    parser.add_argument(
        "--stochastic",
        action="store_true",
        help="sample from the model policy instead of deterministic argmax actions",
    )
    parser.add_argument(
        "--device",
        default="auto",
        help="PyTorch device accepted by Stable-Baselines3, such as auto, cpu, or cuda",
    )
    return parser


def _mean(values: list[float] | list[int]) -> float:
    return float(sum(values) / len(values))


def main() -> None:
    args = build_parser().parse_args()
    if args.episodes <= 0:
        raise SystemExit("--episodes must be positive")
    if args.max_steps <= 0:
        raise SystemExit("--max-steps must be positive")
    if args.max_actions <= 0:
        raise SystemExit("--max-actions must be positive")
    if args.max_territories <= 0:
        raise SystemExit("--max-territories must be positive")
    if not args.scenario.is_file():
        raise SystemExit(f"--scenario is not a file: {args.scenario}")
    if args.policy == "model" and args.model is None:
        raise SystemExit("--model is required when --policy model")
    if args.model is not None and not args.model.is_file():
        raise SystemExit(f"--model is not a file: {args.model}")

    model: Any | None = None
    if args.policy == "model":
        try:
            from sb3_contrib import MaskablePPO
        except ImportError as error:
            raise SystemExit(
                "evaluation dependencies are missing; install triplea-battle-gym[train]"
            ) from error
        model = MaskablePPO.load(str(args.model), device=args.device)

    base_env = TripleAStrategicEnv(
        server_command=tuple(shlex.split(args.server_command)),
        reset_request=StrategicResetRequest(
            scenario_path=str(args.scenario),
            seed=args.seed,
            self_play=True,
            max_actions=args.max_actions,
            max_rounds=args.max_rounds,
        ),
        max_territories=args.max_territories,
        max_actions=args.max_actions,
    )
    env = SingleSideStrategicEnv(base_env, learner_player=args.learner_player)
    rng = random.Random(args.seed)
    rewards: list[float] = []
    episode_steps: list[int] = []
    opponent_steps: list[int] = []
    terminations = 0
    truncations = 0

    try:
        for episode in range(args.episodes):
            observation, _ = env.reset(seed=args.seed + episode)
            total_reward = 0.0
            total_opponent_steps = 0
            for step in range(1, args.max_steps + 1):
                if args.policy == "model":
                    assert model is not None
                    predicted, _ = model.predict(
                        observation,
                        action_masks=env.action_masks(),
                        deterministic=not args.stochastic,
                    )
                    action = int(np.asarray(predicted).item())
                elif args.policy == "scripted":
                    action = scripted_opponent_action_index(
                        env.raw_observation,
                        env.legal_actions,
                    )
                else:
                    legal = np.flatnonzero(env.action_masks())
                    if len(legal) == 0:
                        raise RuntimeError("strategic environment returned an empty action mask")
                    action = int(rng.choice(legal.tolist()))

                observation, reward, terminated, truncated, info = env.step(action)
                total_reward += float(reward)
                total_opponent_steps += int(info.get("opponentSteps", 0))
                if terminated or truncated:
                    terminations += int(terminated)
                    truncations += int(truncated)
                    episode_steps.append(step)
                    break
            else:
                truncations += 1
                episode_steps.append(args.max_steps)

            rewards.append(total_reward)
            opponent_steps.append(total_opponent_steps)
    finally:
        env.close()

    summary = {
        "episodes": args.episodes,
        "learnerPlayer": args.learner_player,
        "policy": args.policy,
        "model": str(args.model) if args.model is not None else None,
        "deterministic": not args.stochastic if args.policy == "model" else True,
        "seedStart": args.seed,
        "meanReward": _mean(rewards),
        "minimumReward": min(rewards),
        "maximumReward": max(rewards),
        "meanLearnerSteps": _mean(episode_steps),
        "meanOpponentSteps": _mean(opponent_steps),
        "terminatedEpisodes": terminations,
        "truncatedEpisodes": truncations,
    }
    print(json.dumps(summary, sort_keys=True))


if __name__ == "__main__":
    main()
