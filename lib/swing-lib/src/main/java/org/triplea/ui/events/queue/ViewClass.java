package org.triplea.ui.events.queue;

import java.util.function.UnaryOperator;

public interface ViewClass<ControllerEventTypeT, ViewDataT extends ViewData> {
  /**
   * Handles an event from a view-controller. The event consits of a type and a
   * data-change-operation. The data-change-operation will indicate how the current UI data should
   * be changed, the UI is responsible for then rendering that difference.
   */
  void handleEvent(ControllerEventTypeT eventType, UnaryOperator<ViewDataT> modelOperation);
}
