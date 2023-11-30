package games.strategy.engine.history;

import lombok.Getter;

/** A history node that represents an entire game round. */
@Getter
public class Round extends IndexedHistoryNode {
  private static final long serialVersionUID = 7645058269791039043L;
  private final int roundNo;

  Round(final int round, final int changeStartIndex) {
    super("Round: " + round, changeStartIndex);
    roundNo = round;
  }

  @Override
  public SerializationWriter getWriter() {
    return new RoundHistorySerializer(roundNo);
  }
}
