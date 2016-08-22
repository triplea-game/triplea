package games.strategy.triplea.settings.scrolling;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.IntegerValueRange;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingInputComponentFactory;
import games.strategy.triplea.settings.SettingsTab;

public class ScrollSettingsTab implements SettingsTab<ScrollSettings> {

  private final List<SettingInputComponent<ScrollSettings>> inputs;

  public ScrollSettingsTab(final ScrollSettings settings) {
    inputs = Arrays.asList(
        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(2, 200, ScrollSettings.DEFAULT_ARROW_KEY_SCROLL_SPEED),
            "Arrow scroll speed",
            "How fast the map scrolls when using the arrow keys",
            new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()), 5),
            ScrollSettings::setArrowKeyScrollSpeed,
            (scrollSettings) -> String.valueOf(scrollSettings.getArrowKeyScrollSpeed())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(2, 4, ScrollSettings.DEFAULT_FASTER_ARROW_KEY_SCROLL_MULTIPLIER),
            "Arrow scroll speed multiplier",
            "When holding control down, the arrow key map scroll speed is multiplied by this amount",
            new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultiplier()), 5),
            ScrollSettings::setFasterArrowKeyScrollMultiplier,
            (scrollSettings) -> String.valueOf(scrollSettings.getFasterArrowKeyScrollMultiplier())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(10, 250, ScrollSettings.DEFAULT_MAP_EDGE_SCROLL_SPEED),
            "Map Edge Scroll Speed",
            "How fast the map scrolls when the mouse cursor is placed close to the edge of the map",
            new JTextField(String.valueOf(settings.getMapEdgeScrollSpeed()), 5),
            ScrollSettings::setMapEdgeScrollSpeed,
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollSpeed())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(0, 150, ScrollSettings.DEFAULT_MAP_EDGE_SCROLL_ZONE_SIZE),
            "Map Edge Scroll Zone Size",
            "How close the mouse cursor must be to the edge of the map for the map to scroll",
            new JTextField(String.valueOf(settings.getMapEdgeScrollZoneSize()), 5),
            ScrollSettings::setMapEdgeFasterScrollZoneSize,
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollZoneSize())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(0, 100, ScrollSettings.DEFAULT_MAP_EDGE_FASTER_SCROLL_ZONE_SIZE),
            "Map Edge Faster Scroll Zone Size",
            "How close the mouse cursor must be to the edge of the map for the map to scroll faster",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollZoneSize()), 5),
            ScrollSettings::setMapEdgeFasterScrollZoneSize,
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollZoneSize())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(2, 5, ScrollSettings.DEFAULT_MAP_EDGE_FASTER_SCROLL_MULTIPLIER),
            "Scroll Zone Multiplier",
            "Multiplier for how much faster the map scrolls when the mouse is closest to the edge",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollMultiplier()), 5),
            ScrollSettings::setMapEdgeFasterScrollMultiplier,
            scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollMultiplier())),

        SettingInputComponentFactory.buildIntegerText(
            new IntegerValueRange(10, 400, ScrollSettings.DEFAULT_WHEEL_SCROLL_AMOUNT),
            "Mouse Wheel Scroll Amount",
            "The distance the map is scrolled when the mouse wheel is used",
            new JTextField(String.valueOf(settings.getWheelScrollAmount()), 5),
            ScrollSettings::setWheelScrollAmount,
            scrollSettings -> String.valueOf(scrollSettings.getWheelScrollAmount())));
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
