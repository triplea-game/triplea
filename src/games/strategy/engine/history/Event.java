package games.strategy.engine.history;

public class Event extends IndexedHistoryNode implements Renderable {
  private static final long serialVersionUID = -8382102990360177484L;
  private final String m_description;
  // additional data used for rendering this event
  private Object m_renderingData;

  public String getDescription() {
    return m_description;
  }

  Event(final String description, final int changeStartIndex) {
    super(description, changeStartIndex);
    m_description = description;
  }

  @Override
  public Object getRenderingData() {
    return m_renderingData;
  }

  public void setRenderingData(final Object data) {
    m_renderingData = data;
  }

  @Override
  public SerializationWriter getWriter() {
    return new EventHistorySerializer(m_description, m_renderingData);
  }
}


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
