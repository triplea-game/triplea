package org.triplea.game.client.ui.swing.laf;

import java.util.Collection;
import java.util.stream.Collectors;
import javax.swing.UIManager;
import org.pushingpixels.substance.api.SubstanceCortex;
import org.pushingpixels.substance.api.skin.SkinInfo;
import org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel;

/** Default implementation of the {@link SubstanceLookAndFeelManager} service. */
public final class DefaultSubstanceLookAndFeelManager implements SubstanceLookAndFeelManager {
  @Override
  public String getDefaultLookAndFeelClassName() {
    return SubstanceGraphiteLookAndFeel.class.getName();
  }

  @Override
  public Collection<UIManager.LookAndFeelInfo> getInstalledLookAndFeels() {
    return SubstanceCortex.GlobalScope.getAllSkins().values().stream()
        .map(DefaultSubstanceLookAndFeelManager::newLookAndFeelInfoForSkin)
        .collect(Collectors.toList());
  }

  private static UIManager.LookAndFeelInfo newLookAndFeelInfoForSkin(final SkinInfo skinInfo) {
    return new UIManager.LookAndFeelInfo(
        "Substance " + skinInfo.getDisplayName(),
        skinInfo.getClassName().replaceFirst("(?<=\\.)(\\w+)Skin$", "Substance$1LookAndFeel"));
  }

  @Override
  public void initialize() {
    // workaround for https://github.com/kirill-grouchnikov/radiance/issues/102; remove when
    // upgrading to Substance 1.5+
    SubstanceCortex.GlobalScope.setUseDefaultColorChooser();
  }
}
