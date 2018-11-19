package org.triplea.game.client.ui.swing.laf;

import java.util.Collection;
import java.util.stream.Collectors;

import org.pushingpixels.substance.api.SubstanceCortex;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

/**
 * Default implementation of the {@link SubstanceLookAndFeelManager} service.
 */
public final class DefaultSubstanceLookAndFeelManager implements SubstanceLookAndFeelManager {
  @Override
  public String getDefaultLookAndFeelClassName() {
    return SubstanceGraphiteLookAndFeel.class.getName();
  }

  @Override
  public Collection<String> getInstalledLookAndFeelClassNames() {
    return SubstanceCortex.GlobalScope.getAllSkins().values().stream()
        .map(skinInfo -> getLookAndFeelClassNameForSkinClassName(skinInfo.getClassName()))
        .collect(Collectors.toList());
  }

  private static String getLookAndFeelClassNameForSkinClassName(final String skinClassName) {
    return skinClassName.replaceFirst("(?<=\\.)(\\w+)Skin$", "Substance$1LookAndFeel");
  }

  @Override
  public void initialize() {
    // workaround for https://github.com/kirill-grouchnikov/radiance/issues/102; remove when upgrading to Substance 1.5+
    SubstanceCortex.GlobalScope.setUseDefaultColorChooser();
  }
}
