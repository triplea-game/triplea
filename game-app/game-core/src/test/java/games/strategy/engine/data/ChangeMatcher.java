package games.strategy.engine.data;

import org.hamcrest.TypeSafeMatcher;

/**
 * Parent class for all matchers for {@link Change} objects
 *
 * @param <T> The change object to match
 */
public abstract class ChangeMatcher<T> extends TypeSafeMatcher<T> {}
