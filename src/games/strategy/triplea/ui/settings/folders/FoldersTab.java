package games.strategy.triplea.ui.settings.folders;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.ui.settings.SettingInputComponent;
import games.strategy.triplea.ui.settings.SettingsTab;

import javax.swing.JTextField;

public class FoldersTab implements SettingsTab<FolderSettings> {
  private final FolderSettings settings;

  public FoldersTab(FolderSettings settings) {
    this.settings = settings;
  }

  @Override
  public String getTabTitle() {
    return "Folders";
  }

  @Override
  public List<SettingInputComponent<FolderSettings>> getInputs() {
    BiConsumer<FolderSettings, String> saveGameUpdater = (((folderSettings, s) -> folderSettings.setSaveGamePath(s)));
    BiConsumer<FolderSettings, String> downloadPathUpdater =
        (((folderSettings, s) -> folderSettings.setDownloadedMapPath(s)));

    return Arrays.asList(
        SettingInputComponent.build("Save game path", "Location where games are saved by default",
            new JTextField(settings.getSaveGamePath()), saveGameUpdater),
        SettingInputComponent.build("Map Download Path", "Location where map downloads are found",
            new JTextField(settings.getDownloadedMapPath()), downloadPathUpdater));
  }

  @Override public void setToDefault() {

  }

  @Override
  public FolderSettings getSettingsObject() {
    return ClientContext.folderSettings();
  }

}
