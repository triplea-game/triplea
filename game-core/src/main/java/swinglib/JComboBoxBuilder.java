package swinglib;

import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComboBox;

import com.google.common.base.Preconditions;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Builds a swing JComboBox that supports String values only. This is a pull down
 * box with menu options that can be selected.
 * <br />
 * Example usage:
 *
 * <pre>
 * <code>
 *   JComboBox combBox = JComboBoxBuilder.builder()
 *       .menuOptions("option 1", "option 2")
 *       .itemListener(selection -> selection.equals("option 1") ? fooBar())
 *       .build();
 * </code>
 * </pre>
 */
public final class JComboBoxBuilder {

  private final List<String> options = new ArrayList<>();

  private Consumer<String> selectionAction;

  private String settingKeyName;

  private ClientSetting clientSetting;


  private JComboBoxBuilder() {

  }

  /**
   * Builds the swing component.
   */
  public JComboBox<String> build() {
    Preconditions.checkState(options.size() > 0);
    final Consumer<String> myAction = selectionAction;

    final JComboBox<String> comboBox = new JComboBox<>(options.toArray(new String[options.size()]));

    if (clientSetting != null) {
      final String lastValue = clientSetting.value();

      if (options.contains(lastValue)) {
        comboBox.setSelectedItem(lastValue);
      } else {
        clientSetting.resetAndFlush();
      }
    }

    if (settingKeyName != null) {
      final String lastValue = ClientSetting.load(settingKeyName);

      if (options.contains(lastValue)) {
        // note: this will fire any item action listeners
        comboBox.setSelectedItem(lastValue);
      }
    }

    if (selectionAction != null) {
      comboBox.addItemListener(e -> {

        // combo box will fire two events when you change selection, first a 'ItemEvent.DESELECTED' event and then
        // a 'ItemEvent.SELECTED' event. We keep it simple for now and ignore the deselected event
        if (e.getStateChange() == ItemEvent.SELECTED) {
          final String selectionValue = (e.getItem() == null) ? null : e.getItem().toString();

          Preconditions.checkState(!((clientSetting != null) && (settingKeyName != null)),
              "Only one of ClientSetting or settingKey should be set at a type, they are"
                  + "are redundant to each other. One is a type safe enum, the other is a free form String value."
                  + " Client setting == null ? " + (clientSetting == null)
                  + ", settingKeyName == null ? " + (settingKeyName == null));

          if (clientSetting != null) {
            clientSetting.saveAndFlush(selectionValue);
          } else if (settingKeyName != null) {
            ClientSetting.save(settingKeyName, selectionValue);
            ClientSetting.flush();
          }

          myAction.accept(selectionValue);
        }
      });
    }

    return comboBox;
  }

  public static JComboBoxBuilder builder() {
    return new JComboBoxBuilder();
  }

  /**
   * Adds a set of options to be displayed in the combo box.
   */
  public JComboBoxBuilder menuOptions(final String... options) {
    Preconditions.checkArgument(options.length > 0);
    this.options.addAll(Arrays.asList(options));
    return this;
  }

  /**
   * Adds a listener that is fired when an item is selected. The input value
   * to the passed in consumer is the value selected.
   */
  public JComboBoxBuilder itemListener(final Consumer<String> selectionAction) {
    Preconditions.checkNotNull(selectionAction);
    this.selectionAction = selectionAction;
    return this;
  }


  public JComboBoxCompositeBuilder compositeBuilder() {
    return new JComboBoxCompositeBuilder(this);
  }


  /**
   * Toggles a behavior where the last selected value will be remembered as a default.
   * This uses ClientSettings, so it would use the windows registry to store these values.
   *
   * @param settingKeyName A key name to be used for storage. Unique names will overwrite each other.
   */
  public JComboBoxBuilder useLastSelectionAsFutureDefault(final String settingKeyName) {
    this.settingKeyName = settingKeyName;
    if (selectionAction == null) {
      selectionAction = value -> {
      };
    }
    return this;
  }

  /**
   * Toggles a behavior where last selected value is remembered. The ClientSetting passed in
   * as a parameter is used to 'back' this behavior. The value of the ClientSetting will
   * be read for a default value and will be saved back when the combo box selection has
   * been updated.
   */
  public JComboBoxBuilder useLastSelectionAsFutureDefault(final ClientSetting clientSetting) {
    this.clientSetting = clientSetting;
    if (selectionAction == null) {
      selectionAction = value -> {
      };
    }
    return this;
  }

}
