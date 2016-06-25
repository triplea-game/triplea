package games.strategy.triplea.settings;

import java.util.ArrayList;
import java.util.List;

import games.strategy.ui.SwingComponents;

public interface SettingsTab<T extends HasDefaults> {
  String getTabTitle();

  List<SettingInputComponent<T>> getInputs();

  T getSettingsObject();

  default void updateSettings(List<SettingInputComponent<T>> inputs) {
    final StringBuilder msg = new StringBuilder();
    final List<String> invalidValues = new ArrayList<>();
    inputs.forEach(input -> {
      T settingsObject = getSettingsObject();

      String oldValue = input.getValue(settingsObject);
      if (input.updateSettings(settingsObject, input.getInputElement())) {
        String newValue = input.getValue(settingsObject);
        if(!newValue.equals(oldValue)) {
          msg.append("\n").append(input.getLabel()).append(": ").append(oldValue).append(" -> ").append(newValue);
        }
      } else {
        invalidValues.add(input.getInputElement().getText());
      }
    });

    String message = msg.toString();
    if(message.isEmpty()) {
      message = "No values updated";
    } else {
      message = "Updated values:" + msg.toString();
    }

    if( !invalidValues.isEmpty() ) {
      message += "\nValues out of range, not updated: ";
      for (String invalidValue : invalidValues) {
        message += "\n" + invalidValue;
      }
    }
    SwingComponents.showDialog(message);
  }

}
