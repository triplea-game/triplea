package games.strategy.engine.history;

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
