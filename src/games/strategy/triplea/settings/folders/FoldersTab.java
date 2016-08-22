package games.strategy.triplea.settings.folders;

import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.InputValidator;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingInputComponentFactory;
import games.strategy.triplea.settings.SettingsTab;

public class FoldersTab implements SettingsTab<FolderSettings> {
  private final List<SettingInputComponent<FolderSettings>> inputs;

  public FoldersTab(final FolderSettings settings) {
    JTextField saveGamePathField = new JTextField(settings.getSaveGamePath());
    JPanel holderPanel = new JPanel();
    holderPanel.add(saveGamePathField);

    inputs = Arrays.asList(
        SettingInputComponentFactory.buildTextComponent(
            "Save game path",
            "Default save game folder",
            saveGamePathField,
            FolderSettings::getSaveGamePath,
            FolderSettings::setSaveGamePath,
            InputValidator.IS_DIRECTORY),
        SettingInputComponentFactory.buildTextComponent(
            "Map Download Path",
            "Location where maps are downloaded and found",
            new JTextField(settings.getDownloadedMapPath()),
            FolderSettings::getDownloadedMapPath,
            FolderSettings::setDownloadedMapPath,
            InputValidator.IS_DIRECTORY));
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
