package org.triplea.ui.events.queue;

/**
 * Marker interface for view-model data classes. Represents a simplified version of the data
 * preseneted in the UI. See: https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel
 *
 * <p>For example, a UI checkbox named "enabled" would be represented by a boolean variable, a text
 * field represented by a String.
 *
 * <p>This interface exists for additional type safety and to make it clear there is a specific
 * purpose for any value classes marked with this interface. The value class should be designed to
 * be immutable (use lombok 'toBuilder' to create a copy and make chaneges).
 *
 * <p>ViewData is 1:1 with a view class and the view class should be able to create a UiData object
 * by reading its UI component data state (for example, {@code boolean enabled =
 * enabledCheckbox.isSelected()}).
 *
 * <p>ViewData implementations may have a constructor to create a default UiData state, this might
 * involve loading settings from persistence to restore a remembered user selection.
 */
public interface ViewData {}
