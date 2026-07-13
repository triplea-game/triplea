package games.strategy.triplea.delegate.battle.simulation;

/** Internal control-flow signal used to pause an execution stack at a player decision. */
final class BattleDecisionRequiredException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  BattleDecisionRequiredException() {
    super("battle decision required", null, false, false);
  }
}
