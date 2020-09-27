package tools.map.making.ui.upload;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.swing.JButton;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;

@Builder
public class UploadPanelState {
  @Nonnull private final JButton uploadMapButton;
  @Nonnull private final JButton validateMapButton;
  @Nonnull private final JButton selectMapButton;
  @Nonnull private final JButton lobbyLoginButton;

  @Getter private Path mapDirectory;
  private ApiKey apiKey;

  void setMapDirectory(final Path mapDirectory) {
    this.mapDirectory = mapDirectory;
    validateMapButton.setEnabled(true);
  }

  void unsetMapDirectory() {
    validateMapButton.setEnabled(false);
    lobbyLoginButton.setEnabled(false);
    uploadMapButton.setEnabled(false);
  }

  public void setValidationStatus(final boolean valid) {
    lobbyLoginButton.setEnabled(valid);
    uploadMapButton.setEnabled(valid);
  }

  void setApiKey(final ApiKey apiKey) {
    this.apiKey = apiKey;
    uploadMapButton.setEnabled(apiKey != null);
  }
}
