package games.strategy.engine.history;

class EventChildWriter implements SerializationWriter {
  private static final long serialVersionUID = -7143658060171295697L;
  private final String m_text;
  private final Object m_renderingData;

  public EventChildWriter(final String text, final Object renderingData) {
    m_text = text;
    m_renderingData = renderingData;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.addChildToEvent(new EventChild(m_text, m_renderingData));
  }
}
