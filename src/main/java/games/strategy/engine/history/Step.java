package games.strategy.engine.history;

import games.strategy.engine.data.PlayerID;

public class Step extends IndexedHistoryNode {
  private static final long serialVersionUID = 1015799886178275645L;
  private final PlayerID player;
  private final String stepName;
  private final String delegateName;

  /** Creates a new instance of Step. */
  Step(final String stepName, final String delegateName, final PlayerID player, final int changeStartIndex,
      final String displayName) {
    super(displayName, changeStartIndex);
    this.stepName = stepName;
    this.delegateName = delegateName;
    this.player = player;
  }

  public PlayerID getPlayerId() {
    return player;
  }

  @Override
  public SerializationWriter getWriter() {
    return new StepHistorySerializer(stepName, delegateName, player, super.getTitle());
  }

  public String getDelegateName() {
    return delegateName;
  }

  public String getStepName() {
    return stepName;
  }
}
