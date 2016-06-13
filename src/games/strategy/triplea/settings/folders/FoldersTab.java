package games.strategy.triplea.settings.folders;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.JTextField;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingsTab;
import games.strategy.triplea.settings.validators.InputValidator;

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
    Function<FolderSettings, String> saveGameExtractor = folderSettings -> folderSettings.getSaveGamePath();


    BiConsumer<FolderSettings, String> downloadPathUpdater =
        (((folderSettings, s) -> folderSettings.setDownloadedMapPath(s)));
    Function<FolderSettings, String> downloadPathExtractor = folderSettings -> folderSettings.getDownloadedMapPath();

    return Arrays.asList(
        SettingInputComponent.build("Save game path", "Location where games are saved by default",
            new JTextField(settings.getSaveGamePath()), saveGameUpdater, saveGameExtractor,
            InputValidator.IS_DIRECTORY),
        SettingInputComponent.build("Map Download Path", "Location where map downloads are found",
            new JTextField(settings.getDownloadedMapPath()), downloadPathUpdater, downloadPathExtractor,
            InputValidator.IS_DIRECTORY));
  }


  @Override
  public FolderSettings getSettingsObject() {
    return ClientContext.folderSettings();
  }

}
