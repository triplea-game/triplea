from pathlib import Path

from triplea_battle_gym.strategic_evaluate import build_parser as build_evaluate_parser
from triplea_battle_gym.strategic_train import build_parser as build_train_parser


def test_strategic_train_parser_accepts_resume_and_logging_paths() -> None:
    args = build_train_parser().parse_args(
        [
            "--server-command",
            "./gradlew :game-headless:runBattleSimulationServer",
            "--scenario",
            "scenario.xml",
            "--learner-player",
            "Germans",
            "--resume",
            "checkpoint.zip",
            "--tensorboard-log",
            "runs/tensorboard",
            "--device",
            "cpu",
        ]
    )

    assert args.learner_player == "Germans"
    assert args.resume == Path("checkpoint.zip")
    assert args.tensorboard_log == Path("runs/tensorboard")
    assert args.device == "cpu"


def test_strategic_evaluate_parser_defaults_to_model_policy() -> None:
    args = build_evaluate_parser().parse_args(
        [
            "--server-command",
            "./gradlew :game-headless:runBattleSimulationServer",
            "--scenario",
            "scenario.xml",
            "--learner-player",
            "Americans",
            "--model",
            "policy.zip",
        ]
    )

    assert args.policy == "model"
    assert args.model == Path("policy.zip")
    assert args.learner_player == "Americans"
    assert args.episodes == 20
