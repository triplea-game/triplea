package games.strategy.engine.history;

import games.strategy.engine.data.PlayerID;

class StepHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 3546486775516371557L;

  private final String stepName;
  private final String delegateName;
  private final PlayerID playerId;
  private final String displayName;

  public StepHistorySerializer(final String stepName, final String delegateName, final PlayerID playerId,
      final String displayName) {
    this.stepName = stepName;
    this.delegateName = delegateName;
    this.playerId = playerId;
    this.displayName = displayName;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextStep(stepName, delegateName, playerId, displayName);
  }
}
