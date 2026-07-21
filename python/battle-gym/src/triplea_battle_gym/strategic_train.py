"""Train a Maskable PPO learner against a fixed strategic opponent.

PPO receives decisions for exactly one named player. The Java environment still runs complete
two-sided games, but ``SingleSideStrategicEnv`` executes the opponent internally and accumulates all
score-margin changes into the learner transition. This avoids interleaving two players' trajectories
inside one single-agent rollout.
"""

from __future__ import annotations

import argparse
import shlex
from pathlib import Path
from typing import Any

from .strategic_env import TripleAStrategicEnv
from .strategic_models import StrategicResetRequest
from .strategic_training_env import SingleSideStrategicEnv


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-command", required=True)
    parser.add_argument(
        "--scenario",
        type=Path,
        required=True,
        help="a map XML to start a fresh game, or a save game to resume one",
    )
    parser.add_argument(
        "--learner-player",
        required=True,
        help="exact player name controlled by PPO; the other side uses the scripted baseline",
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
        help="parallel learner games, each with its own TripleA server process",
    )
    parser.add_argument(
        "--resume",
        type=Path,
        help="existing MaskablePPO checkpoint to continue for --timesteps additional timesteps",
    )
    parser.add_argument(
        "--tensorboard-log",
        type=Path,
        help="directory for TensorBoard event files",
    )
    parser.add_argument(
        "--device",
        default="auto",
        help="PyTorch device accepted by Stable-Baselines3, such as auto, cpu, or cuda",
    )
    return parser


def main() -> None:
    args = build_parser().parse_args()
    if args.timesteps <= 0:
        raise SystemExit("--timesteps must be positive")
    if args.n_envs <= 0:
        raise SystemExit("--n-envs must be positive")
    if args.max_actions <= 0:
        raise SystemExit("--max-actions must be positive")
    if args.max_territories <= 0:
        raise SystemExit("--max-territories must be positive")
    if args.checkpoint_every < 0:
        raise SystemExit("--checkpoint-every must not be negative")
    if not args.scenario.is_file():
        raise SystemExit(f"--scenario is not a file: {args.scenario}")
    if args.resume is not None and not args.resume.is_file():
        raise SystemExit(f"--resume is not a file: {args.resume}")
    try:
        from sb3_contrib import MaskablePPO
        from stable_baselines3.common.callbacks import CheckpointCallback
        from stable_baselines3.common.vec_env import DummyVecEnv, SubprocVecEnv
    except ImportError as error:
        raise SystemExit(
            "training dependencies are missing; install triplea-battle-gym[train]"
        ) from error

    def make(rank: int) -> Any:
        def build() -> SingleSideStrategicEnv:
            # Workers interleave disjoint episode seeds:
            # rank 0 => seed, seed+n, ...; rank 1 => seed+1, seed+1+n, ...
            env = TripleAStrategicEnv(
                server_command=tuple(shlex.split(args.server_command)),
                reset_request=StrategicResetRequest(
                    scenario_path=str(args.scenario),
                    seed=args.seed + rank,
                    self_play=True,
                    max_actions=args.max_actions,
                    max_rounds=args.max_rounds,
                ),
                max_territories=args.max_territories,
                max_actions=args.max_actions,
                episode_seed_stride=args.n_envs,
            )
            return SingleSideStrategicEnv(
                env,
                learner_player=args.learner_player,
            )

        return build

    # One server process per worker: the environment is bound to one game, and a step is a
    # round-trip into Java, so throughput comes from running games side by side.
    # SingleSideStrategicEnv implements action_masks() directly, as required by MaskablePPO when
    # SubprocVecEnv is used. ActionMasker must not wrap subprocess environments.
    factories = [make(rank) for rank in range(args.n_envs)]
    wrapped = DummyVecEnv(factories) if len(factories) == 1 else SubprocVecEnv(factories)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    tensorboard_log = None
    if args.tensorboard_log is not None:
        args.tensorboard_log.mkdir(parents=True, exist_ok=True)
        tensorboard_log = str(args.tensorboard_log)

    try:
        if args.resume is None:
            model = MaskablePPO(
                "MultiInputPolicy",
                wrapped,
                verbose=1,
                seed=args.seed,
                tensorboard_log=tensorboard_log,
                device=args.device,
            )
        else:
            model = MaskablePPO.load(
                str(args.resume),
                env=wrapped,
                device=args.device,
                tensorboard_log=tensorboard_log,
            )
            print(f"resuming policy from {args.resume}")

        callback = None
        if args.checkpoint_every > 0:
            # Callback calls count vector steps rather than individual timesteps.
            save_frequency = max(args.checkpoint_every // args.n_envs, 1)
            callback = CheckpointCallback(
                save_freq=save_frequency,
                save_path=str(args.output.parent / "checkpoints"),
                name_prefix=args.output.stem,
            )
        model.learn(
            total_timesteps=args.timesteps,
            callback=callback,
            reset_num_timesteps=args.resume is None,
            tb_log_name=args.output.stem,
        )
        model.save(str(args.output))
        print(f"saved policy to {args.output}")
    finally:
        wrapped.close()


if __name__ == "__main__":
    main()
