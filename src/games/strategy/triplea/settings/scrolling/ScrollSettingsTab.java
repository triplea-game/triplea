package games.strategy.triplea.settings.scrolling;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.settings.validators.InputValidator;

public class ScrollSettingsTab implements SettingsTab<ScrollSettings> {

  private final ScrollSettings settings;

  public ScrollSettingsTab(ScrollSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getTabTitle() {
    return "Scrolling";
  }


  @Override
  public List<SettingInputComponent<ScrollSettings>> getInputs() {
    BiConsumer<ScrollSettings, String> arrowKeyUpdater =
        ((scrollSettings, s) -> scrollSettings.setArrowKeyScrollSpeed(s));

    BiConsumer<ScrollSettings, String> arrowKeyMultiplierUpdater =
        ((scrollSettings, s) -> scrollSettings.setFasterArrowKeyScrollMultiplier(s));


    BiConsumer<ScrollSettings, String> mapEdgeScrollSpeedUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollSpeed(s));

    BiConsumer<ScrollSettings, String> scrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollZoneSize(s));

    BiConsumer<ScrollSettings, String> fasterScrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollZoneSize(s));

    BiConsumer<ScrollSettings, String> scrollZoneMultiplierUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollMultiplier(s));

    BiConsumer<ScrollSettings, String> wheelScrollAmountUpdater =
        ((scrollSettings, s) -> scrollSettings.setWheelScrollAmount(s));

    return Arrays.asList(
        SettingInputComponent.build(
            "Arrow scroll speed", "(10-500) Arrow key scrolling speed",
            new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()), 5),
            arrowKeyUpdater,
            InputValidator.inRange(10, 500)),
        SettingInputComponent.build(
            "Arrow scroll speed multiplier",
            "(1-100) Arrow key scroll speed increase when control is held down",
            new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultiplier()), 5),
            arrowKeyMultiplierUpdater,
            InputValidator.inRange(1, 100)),
        SettingInputComponent.build(
            "Map Edge Scroll Speed",
            "(10-500) How fast the map scrolls when the mouse cursor is placed close to the edge of the map",
            new JTextField(String.valueOf(settings.getMapEdgeScrollSpeed()), 5),
            mapEdgeScrollSpeedUpdater,
            InputValidator.inRange(10, 500)),
        SettingInputComponent.build(
            "Map Edge Scroll Zone Size",
            "(0-500) How close the mouse cursor must be to the edge of the map for the map to scroll",
            new JTextField(String.valueOf(settings.getMapEdgeScrollZoneSize()), 5),
            scrollZoneUpdater,
            InputValidator.inRange(0, 500)),
        SettingInputComponent.build(
            "Map Edge Faster Scroll Zone Size",
            "(0-500) How close the mouse curose must be to the edge of the map for the map to scroll faster",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollZoneSize()), 5),
            fasterScrollZoneUpdater,
            InputValidator.inRange(0, 500)),
        SettingInputComponent.build(
            "Scroll Zone Multiplier",
            "(1-50) How much faster the map scrolls when the mouse is closer to the edge",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollMultiplier()), 5),
            scrollZoneMultiplierUpdater,
            InputValidator.inRange(1, 50)),
        SettingInputComponent.build(
            "Mouse Wheel Scroll Amount",
            "(10-500) The distance the map is scrolled when the mouse wheel is used",
            new JTextField(String.valueOf(settings.getWheelScrollAmount()), 5),
            wheelScrollAmountUpdater,
            InputValidator.inRange(10, 500)));
  }

  @Override
  public ScrollSettings getSettingsObject() {
    return ClientContext.scrollSettings();
  }


}
