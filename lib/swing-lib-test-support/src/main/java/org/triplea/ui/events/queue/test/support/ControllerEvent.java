package org.triplea.ui.events.queue.test.support;

import java.util.function.UnaryOperator;
import lombok.Value;
import org.triplea.ui.events.queue.ViewData;

@Value
public class ControllerEvent<ControllerEventTypeT, ViewDataT extends ViewData> {

  /** Simple tuple representing controller events received. */
  ControllerEventTypeT eventType;

  UnaryOperator<ViewDataT> viewDataUpdate;

  @SuppressWarnings("unchecked")
  public ControllerEvent(final Object argument, final Object argument1) {
    this.eventType = (ControllerEventTypeT) argument;
    this.viewDataUpdate = (UnaryOperator<ViewDataT>) argument1;
  }
}
