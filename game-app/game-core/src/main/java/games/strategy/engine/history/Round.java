package games.strategy.engine.history;

/** A history node that represents an entire game round. */
public class Round extends IndexedHistoryNode {
  private static final long serialVersionUID = 7645058269791039043L;
  private final int roundNo;

  Round(final int round, final int changeStartIndex) {
    super("Round: " + round, changeStartIndex);
    roundNo = round;
  }

  public int getRoundNo() {
    return roundNo;
  }

  @Override
  public SerializationWriter getWriter() {
    return new RoundHistorySerializer(roundNo);
  }
}
