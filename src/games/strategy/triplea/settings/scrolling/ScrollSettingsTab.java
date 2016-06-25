package games.strategy.triplea.settings.scrolling;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.InputValidator;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;

public class ScrollSettingsTab implements SettingsTab<ScrollSettings> {

  private final List<SettingInputComponent<ScrollSettings>> inputs;

  public ScrollSettingsTab(ScrollSettings settings) {
    inputs = Arrays.asList(
        SettingInputComponent.build(
            "Arrow scroll speed", "(10-500) Arrow key scrolling speed",
            new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()), 5),
            ((scrollSettings, s) -> scrollSettings.setArrowKeyScrollSpeed(s)),
            scrollSettings -> String.valueOf(scrollSettings.getArrowKeyScrollSpeed()),
            InputValidator.inRange(10, 500)),

        SettingInputComponent.build(
            "Arrow scroll speed multiplier",
            "(1-100) Arrow key scroll speed increase when control is held down",
            new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultiplier()), 5),
            ((scrollSettings, s) -> scrollSettings.setFasterArrowKeyScrollMultiplier(s)),
            scrollSettings -> String.valueOf(scrollSettings.getFasterArrowKeyScrollMultiplier()),
            InputValidator.inRange(1, 100)),

        SettingInputComponent.build(
            "Map Edge Scroll Speed",
            "(10-500) How fast the map scrolls when the mouse cursor is placed close to the edge of the map",
            new JTextField(String.valueOf(settings.getMapEdgeScrollSpeed()), 5),
            ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollSpeed(s)),
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollSpeed()),
            InputValidator.inRange(10, 500)),

        SettingInputComponent.build(
            "Map Edge Scroll Zone Size",
            "(0-500) How close the mouse cursor must be to the edge of the map for the map to scroll",
            new JTextField(String.valueOf(settings.getMapEdgeScrollZoneSize()), 5),
            ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollZoneSize(s)),
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollZoneSize()),
            InputValidator.inRange(0, 500)),

        SettingInputComponent.build(
            "Map Edge Faster Scroll Zone Size",
            "(0-500) How close the mouse curose must be to the edge of the map for the map to scroll faster",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollZoneSize()), 5),
            ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollZoneSize(s)),
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollZoneSize()),
            InputValidator.inRange(0, 500)),

        SettingInputComponent.build(
            "Scroll Zone Multiplier",
            "(1-50) How much faster the map scrolls when the mouse is closer to the edge",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollMultiplier()), 5),
            ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollMultiplier(s)),
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollMultiplier()),
            InputValidator.inRange(1, 50)),

        SettingInputComponent.build(
            "Mouse Wheel Scroll Amount",
            "(10-500) The distance the map is scrolled when the mouse wheel is used",
            new JTextField(String.valueOf(settings.getWheelScrollAmount()), 5),
            ((scrollSettings, s) -> scrollSettings.setWheelScrollAmount(s)),
            scrollSettings -> String.valueOf(scrollSettings.getWheelScrollAmount()),
            InputValidator.inRange(10, 500)));
  }

  @Override
  public String getTabTitle() {
    return "Scrolling";
  }

  @Override
  public List<SettingInputComponent<ScrollSettings>> getInputs() {
    return inputs;
  }

  @Override
  public ScrollSettings getSettingsObject() {
    return ClientContext.scrollSettings();
  }
}
