package org.triplea.ui.events.queue.test.support;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.triplea.ui.events.queue.ViewControllerEventQueue;
import org.triplea.ui.events.queue.ViewData;

/**
 * EventQueueTestSupport creates a fake event queue which can be used to verify view-controller
 * responses.
 *
 * <p>Example usage where we send a view-controller a view-event and then check the controller
 * response:
 *
 * <pre>
 *   <code>
 *   final EventQueueTestSupport<...> eventQueueTestSupport =
 *       new EventQueueTestSupport<>(LobbySelectionSwingEventQueue.class);
 *
 *   final var controller =
 *       new InstanceOfViewController(eventQueueTestSupport.getEventQueue());
 *
 *   // send controller a view event
 *   controller.handleEvent(EventType.Type, uiData);
 *
 *   // verify controller response event
 *   assertThat(
 *     eventQueueTestSupport.popFirstControllerEvent(),
 *     EventQueueAssertions.controllerEventIs(
 *        EventType.Type,
 *        uiData,
 *        uiData.toBuilder()....build()
 *     ));
 *
 *   </code>
 * </pre>
 */
@Getter
public class EventQueueTestSupport<
    EventQueueTypeT extends ViewControllerEventQueue<?, ?, ViewDataT, ControllerEventTypeT, ?>,
    ControllerEventTypeT,
    ViewDataT extends ViewData> {

  private final EventQueueTypeT eventQueue;

  /** List of all controller events received. */
  private final Deque<ControllerEvent<ControllerEventTypeT, ViewDataT>> controllerEvents =
      new LinkedList<>();

  /**
   * Sets up a view listener that will store any events received. This 'view listener' is available
   * via the 'getViewListener' method and can be added as a view to an event-queue.
   */
  public <T extends EventQueueTypeT> EventQueueTestSupport(final Class<T> eventQueueClassType) {
    this.eventQueue = Mockito.mock(eventQueueClassType);
    Mockito.doAnswer(
            invocation ->
                controllerEvents.add(
                    new ControllerEvent<>(
                        invocation.getArguments()[0], invocation.getArguments()[1])))
        .when(eventQueue)
        .publishControllerEvent(Mockito.any(), Mockito.any());
  }

  public ControllerEvent<ControllerEventTypeT, ViewDataT> popFirstControllerEvent() {
    try {
      Awaitility.waitAtMost(2, TimeUnit.SECONDS)
          .pollInterval(1, TimeUnit.MILLISECONDS)
          .pollDelay(1, TimeUnit.MICROSECONDS)
          .until(() -> !controllerEvents.isEmpty());
    } catch (final ConditionTimeoutException e) {
      Assertions.fail("Timeout, no events on queue");
    }

    return controllerEvents.removeFirst();
  }
}
