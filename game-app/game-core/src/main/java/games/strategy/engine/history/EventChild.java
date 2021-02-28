package games.strategy.engine.history;

/**
 * A history node that contains the details of an {@link Event} (e.g. for a battle event, the dice
 * rolled during each stage of the battle, the units lost during the battle, etc.).
 */
public class EventChild extends HistoryNode implements Renderable {
  private static final long serialVersionUID = 2436212909638449323L;
  private final String text;
  private final Object renderingData;

  public EventChild(final String text, final Object renderingData) {
    super(text);
    this.text = text;
    this.renderingData = renderingData;
  }

  @Override
  public Object getRenderingData() {
    return renderingData;
  }

  @Override
  public String toString() {
    return text;
  }

  @Override
  public SerializationWriter getWriter() {
    return new EventChildWriter(text, renderingData);
  }
}
