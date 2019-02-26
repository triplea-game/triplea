package games.strategy.engine.history;

class RoundHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 9006488114384654514L;

  private final int roundNo;

  RoundHistorySerializer(final int roundNo) {
    this.roundNo = roundNo;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextRound(roundNo);
  }
}
