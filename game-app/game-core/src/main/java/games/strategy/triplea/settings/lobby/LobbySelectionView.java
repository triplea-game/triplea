package games.strategy.triplea.settings.lobby;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.SelectionComponent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.triplea.java.StringUtils;
import org.triplea.java.exception.UnhandledSwitchCaseException;
import org.triplea.swing.JTextFieldBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.ui.events.queue.ViewClass;

/**
 * UI with: <br>
 * 1) Radio buttons to select lobby, eg: prod / local / other <br>
 * 2) A text field for custom input of lobby URI, enabled only if 'other' is selected.<br>
 *
 * <p>The text field is pre-filled and disabled for each radio button choice except for 'other'
 * which will enable the uri field.
 */
class LobbySelectionView
    implements SelectionComponent<JComponent>,
        ViewClass<LobbySelectionViewController.Events, LobbySelectionViewData> {

  enum Events {
    /** If user selects one of the radio buttons. */
    RADIO_BUTTON_CHANGED,
    /** User clicks reset to default, values on screen should revert to factory settings */
    RESET_TO_DEFAULT,
    /** Any changes not yet saved should revert to last saved preference */
    RESET,
    // There should be a 'SAVE' action here, except UI will handle 'SAVE' action on its own,
    // (reason: difficult, UI is coupled to 'SaveContext')
  }

  private final LobbySelectionSwingEventQueue lobbySelectionEventQueue;
  private final JComponent uiComponent;
  private final JTextField uriField;
  private final List<JRadioButton> radioButtons;

  LobbySelectionView(final LobbySelectionSwingEventQueue lobbySelectionEventQueue) {
    this.lobbySelectionEventQueue = lobbySelectionEventQueue;
    final LobbySelectionViewData uiData = new LobbySelectionViewData();

    uriField = new JTextFieldBuilder().build();
    uriField.setText(uiData.getUriFieldValue());
    uriField.setEnabled(uiData.getUriFieldEnabled());

    final ButtonGroup buttonGroup = new ButtonGroup();
    radioButtons =
        // create a radio button for each of the lobby selection options
        Stream.of(LobbySelectionViewData.LobbyChoice.values())
            .map(
                choice -> {
                  final JRadioButton radioButton = new JRadioButton(choice.getDisplayString());
                  buttonGroup.add(radioButton);
                  radioButton.addActionListener(
                      e ->
                          lobbySelectionEventQueue.publishUiEvent(
                              Events.RADIO_BUTTON_CHANGED, readDataState()));

                  return radioButton;
                })
            .collect(Collectors.toList());

    uiComponent =
        new JPanelBuilder()
            .boxLayoutVertical()
            .add(new JPanelBuilder().addAll(new ArrayList<>(radioButtons)).build())
            .add(uriField)
            .build();
    updateUiToState(new LobbySelectionViewData());
  }

  /** Reads and returns data state of the UI components. */
  LobbySelectionViewData readDataState() {
    final String selectedRadioButton =
        radioButtons.stream()
            .filter(AbstractButton::isSelected)
            .findAny()
            .map(AbstractButton::getText)
            .orElseThrow();

    return LobbySelectionViewData.builder()
        .selectedRadioButton(
            LobbySelectionViewData.LobbyChoice.fromDisplayText(selectedRadioButton))
        .uriFieldValue(uriField.getText().trim())
        .uriFieldEnabled(uriField.isEnabled())
        .build();
  }

  private void updateUiToState(final LobbySelectionViewData uiData) {
    radioButtons.forEach(
        radioButton -> {
          final String currentSelection = uiData.getSelectedRadioButton().getDisplayString();
          radioButton.setSelected(radioButton.getText().equals(currentSelection));
        });
    uriField.setText(uiData.getUriFieldValue());
    uriField.setEnabled(uiData.getUriFieldEnabled());
  }

  @SuppressWarnings("SwitchStatementWithTooFewBranches")
  @Override
  public void handleEvent(
      final LobbySelectionViewController.Events controllerEvent,
      final UnaryOperator<LobbySelectionViewData> dataChangeFunction) {

    switch (controllerEvent) {
      case UPDATE_UI_DATA:
        final var updatedState = dataChangeFunction.apply(readDataState());
        updateUiToState(updatedState);
        break;
      default:
        throw new UnhandledSwitchCaseException(controllerEvent);
    }
  }

  @Override
  public JComponent getUiComponent() {
    return uiComponent;
  }

  @Override
  public void save(final SaveContext context) {
    final String uri = uriField.getText().trim();
    if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
      SwingComponents.showError(
          uiComponent,
          "Invalid Lobby URI",
          String.format("Invalid URI specified: %s\nMust start with 'https://' or 'http://'", uri));
    } else {
      try {
        // truncate trailing slash if present on the Uri field
        final URI newLobbyUri = new URI(StringUtils.truncateEnding(uri, "/"));
        context.setValue(ClientSetting.lobbyUri, newLobbyUri);
      } catch (final URISyntaxException e) {
        SwingComponents.showError(
            uiComponent,
            "Invalid Lobby URI",
            String.format("Invalid URI specified: %s\n%s", uri, e.getMessage()));
      }
    }
  }

  @Override
  public void resetToDefault() {
    lobbySelectionEventQueue.publishUiEvent(Events.RESET_TO_DEFAULT, readDataState());
  }

  @Override
  public void reset() {
    lobbySelectionEventQueue.publishUiEvent(Events.RESET, readDataState());
  }
}
