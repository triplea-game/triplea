package games.strategy.engine.history;

import games.strategy.engine.data.GamePlayer;

class StepHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 3546486775516371557L;

  private final String stepName;
  private final String delegateName;
  private final GamePlayer gamePlayer;
  private final String displayName;

  StepHistorySerializer(
      final String stepName,
      final String delegateName,
      final GamePlayer gamePlayer,
      final String displayName) {
    this.stepName = stepName;
    this.delegateName = delegateName;
    this.gamePlayer = gamePlayer;
    this.displayName = displayName;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextStep(stepName, delegateName, gamePlayer, displayName);
  }
}
