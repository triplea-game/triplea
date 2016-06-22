package games.strategy.triplea.ui.settings.scrolling;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.ui.settings.SettingInputComponent;
import games.strategy.triplea.ui.settings.SettingsTab;

import javax.swing.JTextField;

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
            "Arrow scroll speed", "Arrow key scrolling speed",
            new JTextField(String.valueOf(settings.getArrowKeyScrollSpeed()),5),
            arrowKeyUpdater),
        SettingInputComponent.build(
            "Arrow scroll speed multiplier",
            "Arrow key scroll speed increase when control is held down",
            new JTextField(String.valueOf(settings.getFasterArrowKeyScrollMultipler()), 5),
            arrowKeyMultiplierUpdater),
        SettingInputComponent.build(
            "Map Edge Scroll Speed",
            "",
            new JTextField(String.valueOf(settings.getMapEdgeScrollSpeed()), 5),
            mapEdgeScrollSpeedUpdater),
        SettingInputComponent.build(
            "Map Edge Scroll Zone Size",
            "",
            new JTextField(String.valueOf(settings.getMapEdgeScrollZoneSize()), 5),
            scrollZoneUpdater),
        SettingInputComponent.build(
            "Map Edge Faster Scroll Zone Size",
            "",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollZoneSize()), 5),
            fasterScrollZoneUpdater),
        SettingInputComponent.build(
            "Scroll Zone Multiplier",
            "",
            new JTextField(String.valueOf(settings.getMapEdgeFasterScrollMultiplier()), 5),
            scrollZoneMultiplierUpdater),
        SettingInputComponent.build(
            "Mouse Wheel Scroll Amount",
            "",
            new JTextField(String.valueOf(settings.getWheelScrollAmount()), 5),
            wheelScrollAmountUpdater)
    );
  }

  @Override public void setToDefault() {

  }

  @Override
  public ScrollSettings getSettingsObject() {
    return ClientContext.scrollSettings();
  }

}
