package games.strategy.engine.history;

import lombok.Getter;

/**
 * A history node that represents an event that occurred during the execution of a game step (e.g.
 * "Russia buys 8 Infantry").
 */
public class Event extends IndexedHistoryNode implements Renderable {
  private static final long serialVersionUID = -8382102990360177484L;
  @Getter private final String description;
  // additional data used for rendering this event
  private Object renderingData;

  Event(final String description, final int changeStartIndex) {
    super(description, changeStartIndex);
    this.description = description;
  }

  @Override
  public Object getRenderingData() {
    return renderingData;
  }

  public void setRenderingData(final Object renderingData) {
    this.renderingData = renderingData;
  }

  @Override
  public SerializationWriter getWriter() {
    return new EventHistorySerializer(description, renderingData);
  }
}
