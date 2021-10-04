package org.triplea.ui.events.queue.test.support;

import com.google.common.base.Preconditions;
import org.hamcrest.Matcher;
import org.triplea.test.common.CustomMatcher;
import org.triplea.ui.events.queue.ViewData;

/**
 * Assertions that can be used with 'EventQueueTestSupport'. A typical test will set up the queue
 * test support, an event queue and controller. The queue test support will be registered as a view
 * with the event queue. Then, the test will publish a view event to the event queue. Test support
 * is then used to verify any response events placed back on the queue by the controller.
 */
public class EventQueueAssertions {

  /**
   * Asserts that a given controller event has:<br>
   * A) a given event-type <br>
   * B) the data transform payload when given an input data will produce an expected output data.
   *
   * <p>Example usage: <code>
   *   <pre>
   * assertThat(
   *    eventQueueTestSupport.popFirstControllerEvent(),
   *    controllerEventIs(
   *         ControllerEvents.EVENT_TYPE,
   *         inputData,
   *         expectedOutputData));
   *   </pre>
   * </code>
   */
  public static <ControllerEventTypeT, ViewDataT extends ViewData>
      Matcher<ControllerEvent<ControllerEventTypeT, ViewDataT>> controllerEventIs(
          final ControllerEventTypeT expectedEventType,
          final ViewDataT inputData,
          final ViewDataT expectedOutputData) {

    Preconditions.checkNotNull(expectedEventType);
    return CustomMatcher.<ControllerEvent<ControllerEventTypeT, ViewDataT>>builder()
        .debug(
            controllerEvent ->
                String.format(
                    "Type received: %s, data change operation yielded (input -> output):\n%s\n->\n%s",
                    controllerEvent.getEventType(),
                    inputData,
                    controllerEvent.getViewDataUpdate().apply(expectedOutputData)))
        .description(
            String.format(
                "Expected event type %s with output:\n%s", expectedEventType, expectedOutputData))
        .checkCondition(
            controllerEvent ->
                controllerEvent.getEventType() == expectedEventType
                    && controllerEvent
                        .getViewDataUpdate()
                        .apply(inputData)
                        .equals(expectedOutputData))
        .build();
  }
}
