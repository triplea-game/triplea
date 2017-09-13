package games.strategy.engine.history;

import games.strategy.engine.data.Change;

class ChangeSerializationWriter implements SerializationWriter {
  private static final long serialVersionUID = -3802807345707883606L;
  private final Change aChange;

  public ChangeSerializationWriter(final Change change) {
    aChange = change;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.addChange(aChange);
  }
}
