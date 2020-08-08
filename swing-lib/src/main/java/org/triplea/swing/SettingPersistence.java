package org.triplea.swing;

public interface SettingPersistence {
  void saveSetting(boolean value);

  boolean getSetting();
}
