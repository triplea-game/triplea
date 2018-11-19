package org.triplea.game.client.ui.swing.laf;

import org.pushingpixels.substance.api.SubstanceCortex;

/**
 * Default implementation of the {@link SubstanceLookAndFeelManager} service.
 */
public final class DefaultSubstanceLookAndFeelManager implements SubstanceLookAndFeelManager {
  @Override
  public void initialize() {
    // workaround for https://github.com/kirill-grouchnikov/radiance/issues/102; remove when upgrading to Substance 1.5+
    SubstanceCortex.GlobalScope.setUseDefaultColorChooser();
  }
}
