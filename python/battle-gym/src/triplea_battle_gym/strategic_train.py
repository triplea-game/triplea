"""Train a Maskable PPO policy by self-play over whole Small Front games.

Self-play here means one policy plays both sides: an episode alternates turns, and every reward is
already from the acting player's perspective, so the same weights are updated for whoever moved. The
policy never sees which side it is beyond the observation, which is what makes a single set of
weights usable for both.
"""

from __future__ import annotations

import argparse
import shlex
from pathlib import Path
from typing import Any

from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument(
        "--scenario",
        type=Path,
        required=True,
        help="a map XML to start a fresh game, or a save game to resume one",
    )
    parser.add_argument("--timesteps", type=int, default=200_000)
    parser.add_argument("--seed", type=int, default=1)
    parser.add_argument("--max-actions", type=int, default=2048)
    parser.add_argument("--max-territories", type=int, default=64)
    parser.add_argument(
        "--max-rounds",
        type=int,
        default=12,
        help="backstop for a map whose scoring round never fires; 0 leaves it to the map",
    )
    parser.add_argument("--output", type=Path, default=Path("strategic-policy.zip"))
    parser.add_argument("--checkpoint-every", type=int, default=20_000)
    parser.add_argument(
        "--n-envs",
        type=int,
        default=1,
        help="parallel games, each with its own server; one env runs about 4 steps/sec",
    )
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.timesteps <= 0:
        raise SystemExit("--timesteps must be positive")
    try:
        from sb3_contrib import MaskablePPO
        from sb3_contrib.common.wrappers import ActionMasker
        from stable_baselines3.common.callbacks import CheckpointCallback
        from stable_baselines3.common.vec_env import DummyVecEnv, SubprocVecEnv
    except ImportError as error:
        raise SystemExit(
            "training dependencies are missing; install triplea-battle-gym[train]"
        ) from error

    def mask(environment: Any) -> Any:
        return environment.action_masks()

    def make(rank: int) -> Any:
        def build() -> Any:
            env = TripleAStrategicEnv(
                server_command=tuple(shlex.split(args.server_command)),
                reset_request=StrategicResetRequest(
                    scenario_path=str(args.scenario),
                    # Each worker plays its own games, or they would all replay one line.
                    seed=args.seed + rank * 1000,
                    self_play=True,
                    max_actions=args.max_actions,
                    max_rounds=args.max_rounds,
                ),
                max_territories=args.max_territories,
                max_actions=args.max_actions,
            )
            return ActionMasker(env, mask)

        return build

    # One server process per worker: the environment is bound to one game, and a step is a
    # round-trip into Java, so throughput comes from running games side by side.
    factories = [make(rank) for rank in range(max(1, args.n_envs))]
    wrapped = DummyVecEnv(factories) if len(factories) == 1 else SubprocVecEnv(factories)
    try:
        model = MaskablePPO("MultiInputPolicy", wrapped, verbose=1, seed=args.seed)
        callback = None
        if args.checkpoint_every > 0:
            # A run this slow should not lose everything if it is interrupted.
            callback = CheckpointCallback(
                save_freq=args.checkpoint_every,
                save_path=str(args.output.parent / "checkpoints"),
                name_prefix=args.output.stem,
            )
        model.learn(total_timesteps=args.timesteps, callback=callback)
        model.save(str(args.output))
        print(f"saved policy to {args.output}")
    finally:
        wrapped.close()


if __name__ == "__main__":
    main()
