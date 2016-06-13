package games.strategy.triplea.settings;

import java.util.List;

import games.strategy.ui.SwingComponents;

public interface SettingsTab<T extends HasDefaults> {
  String getTabTitle();

  List<SettingInputComponent<T>> getInputs();

  default void updateSettings(List<SettingInputComponent<T>> inputs) {
    final StringBuilder msg = new StringBuilder();
    inputs.forEach(input -> {
      T settingsObject = getSettingsObject();

      String oldValue = input.getValue(settingsObject);
      if (input.updateSettings(settingsObject, input.getInputElement())) {
        String newValue = input.getValue(settingsObject);
        if(!newValue.equals(oldValue)) {
          msg.append("\n").append(input.getLabel()).append(": ").append(oldValue).append(" -> ").append(newValue);
        }
      }
    });

    String message = msg.toString();
    if(message.isEmpty()) {
      message = "No values updated";
    } else {
      message = "Updated values:" + msg.toString();
    }
    SwingComponents.showDialog(message);
  }

  T getSettingsObject();
}
