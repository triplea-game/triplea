package games.strategy.engine.history;

import games.strategy.engine.data.PlayerID;

public class Step extends IndexedHistoryNode {
  private static final long serialVersionUID = 1015799886178275645L;
  private final PlayerID m_player;
  private final String m_stepName;
  private final String m_delegateName;

  /** Creates a new instance of Step. */
  Step(final String stepName, final String delegateName, final PlayerID player, final int changeStartIndex,
      final String displayName) {
    super(displayName, changeStartIndex);
    m_stepName = stepName;
    m_delegateName = delegateName;
    m_player = player;
  }

  public PlayerID getPlayerID() {
    return m_player;
  }

  @Override
  public SerializationWriter getWriter() {
    return new StepHistorySerializer(m_stepName, m_delegateName, m_player, super.getTitle());
  }

  public String getDelegateName() {
    return m_delegateName;
  }

  public String getStepName() {
    return m_stepName;
  }
}


class StepHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 3546486775516371557L;
  private final String m_stepName;
  private final String m_delegateName;
  private final PlayerID m_playerID;
  private final String m_displayName;

  public StepHistorySerializer(final String stepName, final String delegateName, final PlayerID playerID,
      final String displayName) {
    m_stepName = stepName;
    m_delegateName = delegateName;
    m_playerID = playerID;
    m_displayName = displayName;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextStep(m_stepName, m_delegateName, m_playerID, m_displayName);
  }
}
