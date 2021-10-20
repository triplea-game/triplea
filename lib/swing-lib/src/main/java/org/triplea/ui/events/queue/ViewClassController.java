package org.triplea.ui.events.queue;

public interface ViewClassController<ViewEventTypeT, ViewDataT> {
  /**
   * Handle an event coming from the view, with parameters indicating the event type and a snapshot
   * of UI data at the time of the event.
   */
  void handleEvent(ViewEventTypeT eventType, ViewDataT viewData);
}
