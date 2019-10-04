package games.strategy.engine.history;

class EventChildWriter implements SerializationWriter {
  private static final long serialVersionUID = -7143658060171295697L;

  private final String text;
  private final Object renderingData;

  EventChildWriter(final String text, final Object renderingData) {
    this.text = text;
    this.renderingData = renderingData;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.addChildToEvent(new EventChild(text, renderingData));
  }
}
