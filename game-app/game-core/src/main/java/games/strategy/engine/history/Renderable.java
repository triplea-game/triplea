package games.strategy.engine.history;

/**
 * An adapter that converts an object that can not be directly rendered in the UI to another
 * representation that can be rendered.
 */
public interface Renderable {
  Object getRenderingData();
}
