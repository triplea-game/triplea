package org.triplea.swing;

import com.google.common.base.Preconditions;
import java.awt.event.ItemEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.swing.JComboBox;

/**
 * Builds a swing JComboBox that supports String values only. This is a pull down box with items
 * that can be selected. <br>
 * Example usage:
 *
 * <pre>
 * <code>
 * JComboBox&lt;String> comboBox = JComboBoxBuilder.builder(String.class)
 *     .items("option 1", "option 2")
 *     .itemSelectedAction(selection -> selection.equals("option 1") ? foo() : bar())
 *     .build();
 * </code>
 * </pre>
 *
 * @param <E> The type of the combo box items.
 */
public final class JComboBoxBuilder<E> {
  private final Class<E> itemType;
  private final List<E> items = new ArrayList<>();
  private @Nullable Consumer<E> itemSelectedAction;
  private boolean autoCompleteEnabled;
  private @Nullable E selectedItem;
  private @Nullable String toolTipText;

  private JComboBoxBuilder(final Class<E> itemType) {
    this.itemType = itemType;
  }

  /** Builds the swing component. */
  public JComboBox<E> build() {
    Preconditions.checkState(!items.isEmpty());

    @SuppressWarnings("unchecked")
    final E[] array = (E[]) Array.newInstance(itemType, items.size());
    final JComboBox<E> comboBox = new JComboBox<>(items.toArray(array));

    Optional.ofNullable(selectedItem).ifPresent(comboBox::setSelectedItem);
    Optional.ofNullable(toolTipText).ifPresent(comboBox::setToolTipText);
    Optional.ofNullable(itemSelectedAction)
        .ifPresent(
            myAction ->
                comboBox.addItemListener(
                    e -> {
                      // combo box will fire two events when you change selection, first a
                      // 'ItemEvent.DESELECTED' event and then
                      // a 'ItemEvent.SELECTED' event. We keep it simple for now and ignore the
                      // deselected event
                      if (e.getStateChange() == ItemEvent.SELECTED) {
                        final @Nullable E selectionValue = itemType.cast(e.getItem());
                        myAction.accept(selectionValue);
                      }
                    }));

    if (autoCompleteEnabled) {
      AutoCompletion.enable(comboBox);
    }

    return comboBox;
  }

  public static <E> JComboBoxBuilder<E> builder(final Class<E> itemType) {
    Preconditions.checkNotNull(itemType);
    return new JComboBoxBuilder<>(itemType);
  }

  /** Adds a set of items to be displayed in the combo box. */
  public JComboBoxBuilder<E> items(final Collection<E> items) {
    Preconditions.checkArgument(!items.isEmpty());
    this.items.addAll(items);
    return this;
  }

  /** Adds a single item to be displayed in the combo box (additive with any existing). */
  public JComboBoxBuilder<E> item(final E item) {
    Preconditions.checkNotNull(item);
    items.add(item);
    return this;
  }

  /**
   * Adds a listener that is fired when an item is selected. The input value to the passed in
   * consumer is the value selected.
   */
  public JComboBoxBuilder<E> itemSelectedAction(final Consumer<E> itemSelectedAction) {
    Preconditions.checkNotNull(itemSelectedAction);
    this.itemSelectedAction = itemSelectedAction;
    return this;
  }

  public JComboBoxBuilder<E> toolTipText(final String toolTipText) {
    Preconditions.checkNotNull(toolTipText);
    this.toolTipText = toolTipText;
    return this;
  }

  public JComboBoxBuilder<E> enableAutoComplete() {
    autoCompleteEnabled = true;
    return this;
  }

  public JComboBoxBuilder<E> selectedItem(final E selectedItem) {
    Preconditions.checkNotNull(selectedItem);
    this.selectedItem = selectedItem;
    return this;
  }

  public JComboBoxBuilder<E> nullableSelectedItem(final @Nullable E selectedItem) {
    return (selectedItem != null) ? selectedItem(selectedItem) : this;
  }
}
