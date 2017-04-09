package games.strategy.triplea.settings;

/**
 * Wrapper API around a 'settings' object, allows for a GUI interface that allows a user to read descriptions about
 * each setting in the object, and to update the value.
 */
public interface SettingInputComponent<SettingsObjectType extends HasDefaults> {

  /**
   * @return Short two or three word label of the user setting. Use 'getDescription' to provide more detail.
   */
  String getLabel();


  /**
   * @return Detailed (but concise) text description of what the setting represents. This is a message to the user
   *         describing a specific setting, what it does, and which values they should change it to.
   */
  String getDescription();

  /**
   * Gets a description of the permissible range of values for the setting.
   *
   * @return A description of the permissible range of values for the setting or an empty string if the setting does not
   *         enforce a value range; never {@code null}.
   */
  String getValueRangeDescription();

  /**
   * @return The settings input object used to read user input from a Swing component.
   */
  SettingsInput getInputElement();

  /**
   * Return true if a valid setting can be read from the input component and applied to the 'settings' data object.
   *
   * @param toUpdate The 'Settings' data object to be updated.
   */
  boolean updateSettings(SettingsObjectType toUpdate);

  /**
   * Method to read the settings value from the SettingsObject that has the value saved.
   *
   * @param settingsType Settings object which has the current stored user setting value
   * @return An extracted value corresponding to the current setting from the 'settings' object.
   */
  String getValue(SettingsObjectType settingsType);

  /**
   * In cases where we try to update to an invalid value, set Value is called to restore the default/previous valid
   * value.
   */
  void setValue(String valueToSet);

  String getErrorMessage();
}
