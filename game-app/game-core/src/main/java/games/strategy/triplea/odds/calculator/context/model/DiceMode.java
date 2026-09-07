package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): confirm whether analytic/batched-vector modes need their own constants here or live
// only behind the HitRoller impl choice.
/** How a roll group's dice resolve; the seam where luck models diverge. */
public enum DiceMode {
  NORMAL,
  LOW_LUCK
}
