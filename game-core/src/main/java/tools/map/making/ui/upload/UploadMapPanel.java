package tools.map.making.ui.upload;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;
import tools.map.making.ui.upload.map.validators.ValidateMapAction;

/**
 * Panel that prompts a user through a map upload, actions begin with selecting a map to eventually
 * a map upload.
 */
public class UploadMapPanel {

  public static JPanel build(final JFrame parentWindow) {
    final JButton selectMapButton = new JButton("Select Map");
    final JTextArea mapSelectionStatus = buildTextArea();

    final JTextArea validationStatus = buildTextArea();
    final JButton validateMapButton = new JButtonBuilder("Validate Map").enabled(false).build();

    final JTextArea loginStatus = buildTextArea();
    final JButton loginButton = new JButtonBuilder("Lobby Login").enabled(false).build();

    final JButton uploadButton = new JButtonBuilder("Upload").enabled(false).build();
    final JTextArea uploadStatus = buildTextArea();

    final UploadPanelState uploadPanelState =
        UploadPanelState.builder()
            .selectMapButton(selectMapButton)
            .validateMapButton(validateMapButton)
            .lobbyLoginButton(loginButton)
            .uploadMapButton(uploadButton)
            .build();

    selectMapButton.addActionListener(
        new MapSelectionAction(parentWindow, uploadPanelState, mapSelectionStatus));
    validateMapButton.addActionListener(new ValidateMapAction(uploadPanelState, validationStatus));
    loginButton.addActionListener(new LobbyLoginAction(parentWindow, uploadPanelState));
    uploadButton.addActionListener(new UploadAction(uploadPanelState));

    return new JPanelBuilder()
        .borderLayout()
        .addCenter(
            new JPanelBuilder()
                .gridLayout(8, 2)
                .add(new JPanel())
                .add(new JPanel())
                .add(selectMapButton)
                .add(mapSelectionStatus)
                .add(new JPanel())
                .add(new JPanel())
                .add(validateMapButton)
                .add(validationStatus)
                .add(new JPanel())
                .add(new JPanel())
                .add(loginButton)
                .add(loginStatus)
                .add(new JPanel())
                .add(new JPanel())
                .add(uploadButton)
                .add(uploadStatus)
                .build())
        .build();
  }

  private static JTextArea buildTextArea() {
    return new JTextAreaBuilder().rows(3).columns(20).readOnly().build();
  }
}
