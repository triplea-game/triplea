package games.strategy.triplea.delegate.strategic.simulation;

/** Raised instead of silently truncating a strategic legal-action mask. */
public final class StrategicActionSpaceOverflow extends IllegalStateException {
  public StrategicActionSpaceOverflow(final int actionCount, final int maxActions) {
    super(
        "strategic action mask contains "
            + actionCount
            + " actions but maxActions is "
            + maxActions
            + "; increase the reset bound");
  }
}
