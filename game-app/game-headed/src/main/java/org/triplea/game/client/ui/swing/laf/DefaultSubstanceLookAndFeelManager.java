package org.triplea.game.client.ui.swing.laf;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.util.Collection;
import java.util.List;
import javax.swing.UIManager;

/** Look-and-feel manager providing FlatLaf themes as available options. */
public final class DefaultSubstanceLookAndFeelManager implements SubstanceLookAndFeelManager {
  @Override
  public String getDefaultLookAndFeelClassName() {
    return FlatDarkLaf.class.getName();
  }

  @Override
  public Collection<UIManager.LookAndFeelInfo> getInstalledLookAndFeels() {
    return List.of(
        new UIManager.LookAndFeelInfo("FlatLaf Light", FlatLightLaf.class.getName()),
        new UIManager.LookAndFeelInfo("FlatLaf Dark", FlatDarkLaf.class.getName()),
        new UIManager.LookAndFeelInfo("FlatLaf IntelliJ", FlatIntelliJLaf.class.getName()),
        new UIManager.LookAndFeelInfo("FlatLaf Darcula", FlatDarculaLaf.class.getName()));
  }

  @Override
  public void initialize() {
    // FlatLaf requires no pre-initialization before UIManager.setLookAndFeel()
  }
}
