package games.strategy.engine.data.events;

/**
 * A ZoomMapListener will be notified of events that affect a map zoom in ViewMenu in onClick on OK
 * button.
 */
public interface ZoomMapListener {
  void zoomMapChanged(Integer newZoom);
}
