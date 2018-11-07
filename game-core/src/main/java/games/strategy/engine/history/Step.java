package games.strategy.engine.history;

import javax.annotation.Nullable;

import games.strategy.engine.data.PlayerId;

/**
 * A history node that represents one step of game play (e.g. "Britain Combat Move").
 */
public class Step extends IndexedHistoryNode {
  private static final long serialVersionUID = 1015799886178275645L;
  private final @Nullable PlayerId player;
  private final String stepName;
  private final String delegateName;

  Step(final String stepName, final String delegateName, final @Nullable PlayerId player, final int changeStartIndex,
      final String displayName) {
    super(displayName, changeStartIndex);
    this.stepName = stepName;
    this.delegateName = delegateName;
    this.player = player;
  }

  public @Nullable PlayerId getPlayerId() {
    return player;
  }

  @Override
  public SerializationWriter getWriter() {
    return new StepHistorySerializer(stepName, delegateName, player, super.getTitle());
  }

  public String getStepName() {
    return stepName;
  }
}
