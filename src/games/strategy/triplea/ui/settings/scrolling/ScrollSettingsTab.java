package games.strategy.triplea.ui.settings.scrolling;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.ui.settings.SettingInputComponent;
import games.strategy.triplea.ui.settings.SettingsTab;

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
        ((scrollSettings, s) -> scrollSettings.setFasterArrowKeyScrollMultipler(s));


    BiConsumer<ScrollSettings, String> scrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setScrollZoneSizeInPixels(s));
    BiConsumer<ScrollSettings, String> fasterScrollZoneUpdater =
        ((scrollSettings, s) -> scrollSettings.setFasterScrollZoneSizeInPixels(s));
    BiConsumer<ScrollSettings, String> scrollZoneMultiplierUpdater =
        ((scrollSettings, s) -> scrollSettings.setFasterScrollMultipler(s));

    return Arrays.asList(
        SettingInputComponent.build(
            "Arrow scroll speed", "Arrow key scrolling speed", settings.getArrowKeyScrollSpeed(),
            arrowKeyUpdater),
        SettingInputComponent.build(
            "Arrow scroll speed multiplier",
            "Arrow key scroll speed increase when control is held down", settings.getFasterArrowKeyScrollMultipler(),
            arrowKeyMultiplierUpdater),
        SettingInputComponent.build(
            "Scroll Zone Size",
            "", settings.getMapScrollZoneSizeInPixels(),
            scrollZoneUpdater),
        SettingInputComponent.build(
            "Faster Scroll Zone Size",
            "", settings.getMapFasterScrollZoneSizeInPixels(),
            fasterScrollZoneUpdater),
        SettingInputComponent.build(
            "Scroll Zone Multiplier",
            "", settings.getFasterSpeedMultipler(),
            scrollZoneMultiplierUpdater));
  }

  @Override
  public ScrollSettings getSettingsObject() {
    return ClientContext.scrollSettings();
  }

}
