package org.triplea.ui.events.queue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.swing.SwingUtilities;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.ui.events.queue.ViewControllerEventQueueTestData.ExampleEventQueue;
import org.triplea.ui.events.queue.ViewControllerEventQueueTestData.ExampleViewClass;
import org.triplea.ui.events.queue.ViewControllerEventQueueTestData.ExampleViewController;
import org.triplea.ui.events.queue.ViewControllerEventQueueTestData.ViewDataSample;

class ViewControllerEventQueueTest {
  final ExampleEventQueue exampleEventQueue = new ExampleEventQueue();
  final ExampleViewController controllerClassSample = new ExampleViewController();
  final ExampleViewClass exampleViewClass = new ExampleViewClass();

  @BeforeEach
  void setup() {
    exampleEventQueue.addController(controllerClassSample);
    exampleEventQueue.addView(exampleViewClass);
  }

  /** Verify that we can emit a controller event and our view class would receive it. */
  @Test
  void publishModelEvent() {
    assertThat(
        "pre-state, view should have received no controller events",
        exampleViewClass.hasReceivedControllerEvent(),
        is(false));

    // emit a controller event
    exampleEventQueue.publishControllerEvent(
        ExampleViewController.ControllerEventSample.CONTROLLER_EVENT_SAMPLE,
        uiData -> new ViewDataSample());

    // verify controller event received
    Awaitility.await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(exampleViewClass::hasReceivedControllerEvent);
  }

  /** Publish an event from UI and verify controller will receive it. */
  @Test
  void publishUiEvent() {
    assertThat(
        "pre-state, controller has yet to receive any events",
        controllerClassSample.hasReceivedUiEvent(),
        is(false));

    // publish UI event
    exampleEventQueue.publishUiEvent(
        ExampleViewClass.ViewEventSample.UI_EVENT, new ViewDataSample());

    // verify controller receives the event
    Awaitility.await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(controllerClassSample::hasReceivedUiEvent);
  }

  /**
   * Using the EDT thread, publish a UI event. This should be seen by the controller event handler
   * which asserts it was invoked _off_ of the EDT. To test this we then only need to assert that
   * the event was received, which implies the handler was invoked.
   *
   * <p>Note, the UI is already tested implicitly above that controller events are dispatched to the
   * EDT.
   */
  @Test
  void publishUiEvent_EventPublishedFromUiIsHandledOnNewThread() {
    SwingUtilities.invokeLater(
        () ->
            exampleEventQueue.publishUiEvent(
                ExampleViewClass.ViewEventSample.UI_EVENT, new ViewDataSample()));

    Awaitility.await()
        .atMost(Duration.of(2, ChronoUnit.SECONDS))
        .until(controllerClassSample::hasReceivedUiEvent);
  }
}
