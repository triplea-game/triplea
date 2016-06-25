package games.strategy.triplea.settings.scrolling;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.settings.InputValidator;

public class ScrollSettingsTab implements SettingsTab<ScrollSettings> {

  private final ScrollSettings settings;
  private final JTextField arrowScrollSpeedField;

  public ScrollSettingsTab(ScrollSettings settings) {
    this.settings = settings;
    arrowScrollSpeedField = new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()), 5);

  }

  @Override
  public String getTabTitle() {
    return "Scrolling";
  }


  @Override
  public List<SettingInputComponent<ScrollSettings>> getInputs() {
    BiConsumer<ScrollSettings, String> arrowKeyUpdater =
        ((scrollSettings, s) -> scrollSettings.setArrowKeyScrollSpeed(s));
    Function<ScrollSettings, String> arrowKeyExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getArrowKeyScrollSpeed());

    BiConsumer<ScrollSettings, String> arrowKeyMultiplierUpdater =
        ((scrollSettings, s) -> scrollSettings.setFasterArrowKeyScrollMultiplier(s));
    Function<ScrollSettings, String> arrowKeyMultiplierExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getFasterArrowKeyScrollMultiplier());


    BiConsumer<ScrollSettings, String> mapEdgeScrollSpeedUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollSpeed(s));
    Function<ScrollSettings, String> mapEdgeScrollSpeedExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollSpeed());

    BiConsumer<ScrollSettings, String> scrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeScrollZoneSize(s));
    Function<ScrollSettings, String> scrollZoneExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getMapEdgeScrollZoneSize());

    BiConsumer<ScrollSettings, String> fasterScrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollZoneSize(s));
    Function<ScrollSettings, String> fasterScrollZoneExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollZoneSize());

    BiConsumer<ScrollSettings, String> scrollZoneMultiplierUpdater =
        ((scrollSettings, s) -> scrollSettings.setMapEdgeFasterScrollMultiplier(s));
    Function<ScrollSettings, String> scrollZoneMultiplierExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getMapEdgeFasterScrollMultiplier());

    BiConsumer<ScrollSettings, String> wheelScrollAmountUpdater =
        ((scrollSettings, s) -> scrollSettings.setWheelScrollAmount(s));
    Function<ScrollSettings, String> wheelScrollAmountExtractor =
        scrollSettings -> String.valueOf(scrollSettings.getWheelScrollAmount());

    return Arrays.asList(
        SettingInputComponent.build(
            "Arrow scroll speed", "(10-500) Arrow key scrolling speed",
            arrowScrollSpeedField,
            arrowKeyUpdater, arrowKeyExtractor,
            InputValidator.inRange(10, 500)),
        SettingInputComponent.build(
            "Arrow scroll speed multiplier",
            "(1-100) Arrow key scroll speed increase when control is held down",
            new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultiplier()), 5),
            arrowKeyMultiplierUpdater, arrowKeyMultiplierExtractor,
            InputValidator.inRange(1, 100)),
        SettingInputComponent.build(
            "Map Edge Scroll Speed",
            "(10-500) How fast the map scrolls when the mouse cursor is placed close to the edge of the map",
            new JTextField(String.valueOf(settings.getMapEdgeScrollSpeed()), 5),
            mapEdgeScrollSpeedUpdater, mapEdgeScrollSpeedExtractor,
            InputValidator.inRange(10, 500)),
        SettingInputComponent.build(
            "Map Edge Scroll Zone Size",
            "(0-500) How close the mouse cursor must be to the edge of the map for the map to scroll",
            new JTextField(String.valueOf(settings.getMapEdgeScrollZoneSize()), 5),
            scrollZoneUpdater, scrollZoneExtractor,
            InputValidator.inRange(0, 500)),
        SettingInputComponent.build(
            "Map Edge Faster Scroll Zone Size",
            "(0-500) How close the mouse curose must be to the edge of the map for the map to scroll faster",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollZoneSize()), 5),
            fasterScrollZoneUpdater, fasterScrollZoneExtractor,
            InputValidator.inRange(0, 500)),
        SettingInputComponent.build(
            "Scroll Zone Multiplier",
            "(1-50) How much faster the map scrolls when the mouse is closer to the edge",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollMultiplier()), 5),
            scrollZoneMultiplierUpdater, scrollZoneMultiplierExtractor,
            InputValidator.inRange(1, 50)),
        SettingInputComponent.build(
            "Mouse Wheel Scroll Amount",
            "(10-500) The distance the map is scrolled when the mouse wheel is used",
            new JTextField(String.valueOf(settings.getWheelScrollAmount()), 5),
            wheelScrollAmountUpdater, wheelScrollAmountExtractor,
            InputValidator.inRange(10, 500)));
  }

  @Override
  public ScrollSettings getSettingsObject() {
    return ClientContext.scrollSettings();
  }


}
