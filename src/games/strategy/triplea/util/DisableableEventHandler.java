package games.strategy.triplea.util;

import javafx.event.Event;
import javafx.event.EventHandler;

public class DisableableEventHandler<T extends Event> implements EventHandler<T> {

  private EventHandler<T> eventHandler;

  public DisableableEventHandler(EventHandler<T> eventHandler) {
    this.eventHandler = eventHandler;
  }

  boolean enabled = true;

  @Override
  public void handle(T event) {
    if (enabled) {
      eventHandler.handle(event);
    }
  }

  public void setEnabled(boolean bool) {
    enabled = bool;
  }
}
