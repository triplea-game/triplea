package games.strategy.engine.history;

public class EventChild extends HistoryNode implements Renderable {
  private static final long serialVersionUID = 2436212909638449323L;
  public final String m_text;
  public final Object m_renderingData;

  public EventChild(final String text, final Object renderingData) {
    super(text);
    m_text = text;
    m_renderingData = renderingData;
  }

  @Override
  public Object getRenderingData() {
    return m_renderingData;
  }

  @Override
  public String toString() {
    return m_text;
  }

  @Override
  public SerializationWriter getWriter() {
    return new EventChildWriter(m_text, m_renderingData);
  }
}
