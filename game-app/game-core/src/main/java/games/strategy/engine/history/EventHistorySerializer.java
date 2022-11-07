package games.strategy.engine.history;

class EventHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 6404070330823708974L;

  private final String eventName;
  private final Object renderingData;

  EventHistorySerializer(final String eventName, final Object renderingData) {
    this.eventName = eventName;
    this.renderingData = renderingData;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startEvent(eventName);
    if (renderingData != null) {
      writer.setRenderingData(renderingData);
    }
  }
}
