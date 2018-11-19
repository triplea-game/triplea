package org.triplea.game.client.ui.swing.laf;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.pushingpixels.substance.api.SubstanceCortex;

/**
 * Default implementation of the {@link SubstanceLookAndFeelManager} service.
 */
public final class DefaultSubstanceLookAndFeelManager implements SubstanceLookAndFeelManager {
  @Override
  public String getDefaultLookAndFeelClassName() {
    return getClassNameForSkin("Graphite");
  }

  private static String getClassNameForSkin(final String skinName) {
    return "org.pushingpixels.substance.api.skin.Substance" + skinName + "LookAndFeel";
  }

  @Override
  public Collection<String> getInstalledLookAndFeelClassNames() {
    return Arrays.asList(
        "Autumn",
        "BusinessBlackSteel",
        "BusinessBlueSteel",
        "Business",
        "Cerulean",
        "CremeCoffee",
        "Creme",
        "DustCoffee",
        "Dust",
        "Gemini",
        "GraphiteAqua",
        "GraphiteChalk",
        "GraphiteGlass",
        "GraphiteGold",
        "Graphite",
        "Magellan",
        "Mariner",
        "MistAqua",
        "MistSilver",
        "Moderate",
        "NebulaBrickWall",
        "Nebula",
        "OfficeBlack2007",
        "OfficeBlue2007",
        "OfficeSilver2007",
        "Raven",
        "Sahara",
        "Twilight").stream()
        .map(DefaultSubstanceLookAndFeelManager::getClassNameForSkin)
        .collect(Collectors.toList());
  }

  @Override
  public void initialize() {
    // workaround for https://github.com/kirill-grouchnikov/radiance/issues/102; remove when upgrading to Substance 1.5+
    SubstanceCortex.GlobalScope.setUseDefaultColorChooser();
  }
}
