package games.strategy.engine.history;

public class Round extends IndexedHistoryNode {
  private static final long serialVersionUID = 7645058269791039043L;
  private final int m_RoundNo;

  Round(final int round, final int changeStartIndex) {
    super("Round: " + round, changeStartIndex);
    m_RoundNo = round;
  }

  public int getRoundNo() {
    return m_RoundNo;
  }

  @Override
  public SerializationWriter getWriter() {
    return new RoundHistorySerializer(m_RoundNo);
  }
}


class RoundHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 9006488114384654514L;
  private final int m_roundNo;

  public RoundHistorySerializer(final int roundNo) {
    m_roundNo = roundNo;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextRound(m_roundNo);
  }
}
