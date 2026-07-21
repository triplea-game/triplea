package games.strategy.triplea.ai.smallfront;

/** Stable high-level missions understood by both heuristic and LLM turn planners. */
public enum OperationalObjectiveType {
  CAPTURE,
  HOLD,
  PROTECT_SUPPLY,
  GAIN_AIR_SUPERIORITY,
  REDEPLOY_RESERVE,
  SCREEN
}
