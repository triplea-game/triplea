package org.triplea.game.client.ui.swing.laf;

import java.util.Collection;
import javax.swing.UIManager;

/** A service for managing the Substance look and feel library. */
public interface SubstanceLookAndFeelManager {
  /** Returns the class name of the default Substance look and feel. */
  String getDefaultLookAndFeelClassName();

  /** Returns a collection of all installed Substance look and feels. */
  Collection<UIManager.LookAndFeelInfo> getInstalledLookAndFeels();

  /**
   * Initializes the Substance look and feel library. Should be called before any Substance look and
   * feel is created.
   */
  void initialize();
}
