package tools.map.making.ui.upload;

import java.nio.file.Path;
import javax.annotation.Nonnull;
import javax.swing.JButton;
import lombok.Builder;
import lombok.Getter;

@Builder
class UploadPanelState {
  @Nonnull private final JButton uploadMapButton;
  @Nonnull private final JButton validateMapButton;
  @Nonnull private final JButton selectMapButton;
  @Nonnull private final JButton lobbyLoginButton;

  @Getter private Path mapDirectory;

  void setMapDirectory(Path mapDirectory) {
    this.mapDirectory = mapDirectory;
    validateMapButton.setEnabled(true);
  }

  void unsetMapDirectory() {
    validateMapButton.setEnabled(false);
    lobbyLoginButton.setEnabled(false);
    uploadMapButton.setEnabled(false);
  }

  void setValidationStatus(final boolean valid) {
    if (valid) {
      lobbyLoginButton.setEnabled(true);
    } else {
      lobbyLoginButton.setEnabled(false);
      uploadMapButton.setEnabled(false);
    }
  }
}
