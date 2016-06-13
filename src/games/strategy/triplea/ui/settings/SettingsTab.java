package games.strategy.triplea.ui.settings;

import java.util.ArrayList;
import java.util.List;

import games.strategy.ui.SwingComponents;
import games.strategy.util.Tuple;

public interface SettingsTab<T> {
  String getTabTitle();

  List<SettingInputComponent<T>> getInputs();

  default void updateSettings(List<SettingInputComponent<T>> inputs) {
    List<Tuple<SettingInputComponent, String>> valuesUpdated = new ArrayList<>();

    inputs.forEach(input -> {
      System.out.println("Text value = " + input.getInputElement().getText());
      if (input.updateSettings(getSettingsObject(), input.getInputElement())) {
        valuesUpdated.add(Tuple.of(input, input.getInputElement().getText()));
      }
    });

    StringBuilder msg = new StringBuilder("No values updated");
    if (!valuesUpdated.isEmpty()) {
      msg = new StringBuilder("Updated values: ");
      for (Tuple<SettingInputComponent, String> value : valuesUpdated) {
        msg.append("\n").append(value.getFirst().getLabel()).append(" -> ").append(value.getSecond());
      }
    }
    SwingComponents.showDialog(msg.toString());

  }

  void setToDefault();

  T getSettingsObject();
}
