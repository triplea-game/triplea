package org.triplea.ui.events.queue;

import java.util.function.UnaryOperator;

/**
 * Represents an event queue between a view object and a view-controller. Events consist of two
 * items, first an event type identifier and second a data payload. The data payload for view events
 * (to controller) is a snapshot of the UI data. The data payload for controller events (to view) is
 * a data change operator for the UI data. The latter is a bit interesting as controller events will
 * be items like "change field X to value Y", while UI events contain a full snapshot of the UI.
 *
 * <p>The UI data object shared between view and controller is meant to be a simplified version of
 * the UI. EG, any text fields would be represented as strings. See:
 * https://martinfowler.com/eaaDev/PresentationModel.html
 */
public interface ViewControllerEventQueue<
    ViewClassT extends ViewClass<ControllerEventTypeT, ViewDataT>,
    ViewEventTypeT,
    ViewDataT extends ViewData,
    ControllerEventTypeT,
    ControllerClassT extends ViewClassController<ViewEventTypeT, ViewDataT>> {

  /**
   * Adds a view that will listen for controller events. This method is here to help resolve a
   * circular relationship between queue and view, the 'addView' method should be called as close to
   * construction time as possible.
   */
  void addView(ViewClassT view);

  /**
   * Adds a controller that will listen for view events. This method is here to help resolve a
   * circular relationship between queue and controller, the 'addController' method should be called
   * as close to construction time as possible.
   */
  void addController(ControllerClassT controller);

  /**
   * When UI publishes an event, it should publish the full ui data state. This comes up as we will
   * often attach action listeners that publish a ui event. If we attach an action listener to a
   * text box then one would expect to emit the value of the text box and would _not_ expect to have
   * to read the whole UI. Though, the UI shall publish full UI, otherwise the UI is assuming what
   * data a controller needs, the decision of what to do next could easily depend on the value of
   * other fields present in the UI.
   */
  void publishUiEvent(ViewEventTypeT uiEvent, ViewDataT uiData);

  /**
   * Publishes a controller event, all views registered with this queue will receive the event.
   *
   * @param modelEvent Identifies the event type.
   * @param uiDataChange The output of the data change function is the desired state of the UI. The
   *     input to the data change function will generally be the current data state of the UI.
   */
  void publishControllerEvent(
      ControllerEventTypeT modelEvent, UnaryOperator<ViewDataT> uiDataChange);
}
