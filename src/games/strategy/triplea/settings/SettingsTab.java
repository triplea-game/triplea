package games.strategy.triplea.settings;

import java.util.ArrayList;
import java.util.List;

import games.strategy.ui.SwingComponents;

public interface SettingsTab<T extends HasDefaults> {
  String getTabTitle();

  List<SettingInputComponent<T>> getInputs();

  T getSettingsObject();

  default void updateSettings(final List<SettingInputComponent<T>> inputs) {
    final StringBuilder msg = new StringBuilder();
    final StringBuilder failMsg = new StringBuilder();


    // keep track explicitly of the status..
    boolean somethingSaved = false;
    boolean somethingInvalid = false;

    final List<String> invalidValues = new ArrayList<>();
    for (final SettingInputComponent<T> input : inputs) {
      final T settingsObject = getSettingsObject();

      final String oldValue = input.getValue(settingsObject);
      if (input.updateSettings(settingsObject)) {
        final String newValue = input.getValue(settingsObject);

        if (!newValue.equals(oldValue)) {
          if (!msg.toString().isEmpty()) {
            msg.append("\n");
          }
          msg.append(input.getLabel()).append(": ").append(oldValue).append(" -> ").append(newValue);
          somethingSaved = true;
        }
      } else {
        if (!failMsg.toString().isEmpty()) {
          failMsg.append("\n");
        }
        failMsg.append(input.getLabel()).append(": ").append(input.getErrorMessage());

        invalidValues.add(input.getInputElement().getText());
        input.setValue(oldValue);
        somethingInvalid = true;
      }
    }


    final String title;
    final String message;
    if (!somethingSaved && !somethingInvalid) {
      // TODO: Save button should not be enabled unless something is updated, so we would never fall into this case.
      title = "Nothing changed";
      message = "No values updated";
    } else if (somethingSaved && !somethingInvalid) {
      title = "Settings Saved";
      message = msg.toString();
    } else if (!somethingSaved && somethingInvalid) {
      title = "Failed to Save Settings";
      message = failMsg.toString();
    } else {
      title = "Some Settings Saved";
      message = "Successfully updated\n" + msg.toString() + "\n\nNOT Updated\n" + failMsg.toString();
    }
    SwingComponents.showDialog(title, message);
  }

}
