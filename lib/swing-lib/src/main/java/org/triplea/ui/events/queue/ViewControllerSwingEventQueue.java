package org.triplea.ui.events.queue;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;
import javax.swing.SwingUtilities;
import lombok.Setter;
import org.triplea.java.ThreadRunner;

/**
 * Adapted for Swing!
 *
 * <p>Queue to pass events between a Swing UI and a view-model (AKA presentation layer).
 *
 * <p>UIs publish UI events and Model publish model events. UI listen for model events and models
 * listen for UI events. The model event callbacks (UI) are executed on the Swing EDT thread, and UI
 * event callbacks are executed in a new thread (off of the EDT).
 *
 * @param <ViewClassT> Class that builds Swing UI components, emits ViewEvents and listens for
 *     controller events.
 * @param <ViewEventTypeT> Event type emitted by view. Typically will be an enum defined with the
 *     the view class.
 * @param <ViewDataT> Data class representing the data state of the UI. A UI class can create an
 *     instance of this class by reading the current value of its UI components, and vice versa the
 *     data state of those components can be updated to match a 'ViewDataT' <param>
 * @param <ControllerEventTypeT>> Typically an enum defined with the view-controller, represents the
 *     different events that can emitted by the view-controller back to the view.
 * @param <ControllerClassT> Class to handle events from the view and can send controller events
 *     back to the view to for example show error messages, confirmation messages, or simply to
 *     update UI state.
 */
@Setter
public class ViewControllerSwingEventQueue<
        ViewClassT extends ViewClass<ControllerEventTypeT, ViewDataT>,
        ViewEventTypeT,
        ViewDataT extends ViewData,
        ControllerEventTypeT,
        ControllerClassT extends ViewClassController<ViewEventTypeT, ViewDataT>>
    implements ViewControllerEventQueue<
        ViewClassT, ViewEventTypeT, ViewDataT, ControllerEventTypeT, ControllerClassT> {

  private Collection<ControllerClassT> controllers = new CopyOnWriteArrayList<>();
  private Collection<ViewClassT> views = new CopyOnWriteArrayList<>();

  @Override
  public void addView(final ViewClassT view) {
    views.add(view);
  }

  @Override
  public void addController(final ControllerClassT controller) {
    controllers.add(controller);
  }

  @Override
  public void publishUiEvent(final ViewEventTypeT uiEvent, final ViewDataT uiData) {
    Preconditions.checkState(!controllers.isEmpty());

    final Runnable publishTask =
        () ->
            controllers.stream()
                .parallel()
                .forEach(controller -> controller.handleEvent(uiEvent, uiData));
    // Typically, UI events are published from the EDT thread and we want the handling
    // of UI events to be done off of the EDT.
    if (SwingUtilities.isEventDispatchThread()) {
      ThreadRunner.runInNewThread(publishTask);
    } else {
      publishTask.run();
    }
  }

  @Override
  public void publishControllerEvent(
      final ControllerEventTypeT controllerEvent, final UnaryOperator<ViewDataT> uiDataChange) {
    Preconditions.checkState(!views.isEmpty());
    // controller events we expect to always be emitted from a non-EDT thread.
    Preconditions.checkState(!SwingUtilities.isEventDispatchThread());

    SwingUtilities.invokeLater(
        () -> views.forEach(view -> view.handleEvent(controllerEvent, uiDataChange)));
  }
}
