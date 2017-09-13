package games.strategy.engine.history;

class EventHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 6404070330823708974L;
  private final String m_eventName;
  private final Object m_renderingData;

  public EventHistorySerializer(final String eventName, final Object renderingData) {
    m_eventName = eventName;
    m_renderingData = renderingData;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startEvent(m_eventName);
    if (m_renderingData != null) {
      writer.setRenderingData(m_renderingData);
    }
  }
}
