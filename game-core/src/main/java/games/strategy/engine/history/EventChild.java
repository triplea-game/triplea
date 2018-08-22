package games.strategy.engine.history;

/**
 * A history node that contains the details of an {@link Event} (e.g. for a battle event, the dice rolled during each
 * stage of the battle, the units lost during the battle, etc.).
 */
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
