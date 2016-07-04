package games.strategy.triplea.settings.folders;

import java.util.Arrays;
import java.util.List;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.InputValidator;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;

public class FoldersTab implements SettingsTab<FolderSettings> {
  private final List<SettingInputComponent<FolderSettings>> inputs;

  public FoldersTab(FolderSettings settings) {
    inputs = Arrays.asList(
        SettingInputComponent.build("Save game path",
            "Default save game folder",
            new JTextField(settings.getSaveGamePath()),
            (((folderSettings, s) -> folderSettings.setSaveGamePath(s))),
            (folderSettings -> folderSettings.getSaveGamePath()),
            InputValidator.IS_DIRECTORY),
        SettingInputComponent.build("Map Download Path",
            "Location where maps are downloaded and found",
            new JTextField(settings.getDownloadedMapPath()),
            (((folderSettings, s) -> folderSettings.setDownloadedMapPath(s))),
            (folderSettings -> folderSettings.getDownloadedMapPath()),
            InputValidator.IS_DIRECTORY)
    );
  }

  @Override
  public String getTabTitle() {
    return "Folders";
  }

  @Override
  public List<SettingInputComponent<FolderSettings>> getInputs() {
    return inputs;
  }

  @Override
  public FolderSettings getSettingsObject() {
    return ClientContext.folderSettings();
  }
}
