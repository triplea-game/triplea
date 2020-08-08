package games.strategy.engine.history;

import games.strategy.engine.data.Change;

class ChangeSerializationWriter implements SerializationWriter {
  private static final long serialVersionUID = -3802807345707883606L;

  private final Change change;

  ChangeSerializationWriter(final Change change) {
    this.change = change;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.addChange(change);
  }
}
