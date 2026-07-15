package games.strategy.triplea.delegate.strategic.simulation;

/** Ordered phases controlled by the turn-level strategic environment. */
public enum StrategicPhase {
  REINFORCEMENT_ALLOCATION,
  COMBAT_MOVE,
  AIR_ASSIGNMENT,
  BATTLE,
  REDEPLOYMENT,
  COMPLETE
}
