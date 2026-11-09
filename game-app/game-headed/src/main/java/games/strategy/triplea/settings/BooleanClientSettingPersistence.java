package games.strategy.triplea.settings;

import org.triplea.swing.SettingPersistence;

/** Binds a {@link BooleanClientSetting} to a Swing check box builder. */
public record BooleanClientSettingPersistence(BooleanClientSetting setting)
    implements SettingPersistence {

  @Override
  public void saveSetting(final boolean value) {
    setting.saveSetting(value);
  }

  @Override
  public boolean getSetting() {
    return setting.getSetting();
  }
}
