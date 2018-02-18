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
