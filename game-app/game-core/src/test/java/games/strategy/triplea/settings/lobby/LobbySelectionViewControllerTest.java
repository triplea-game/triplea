package games.strategy.triplea.settings.lobby;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.triplea.ui.events.queue.test.support.EventQueueAssertions.controllerEventIs;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.lobby.LobbySelectionViewData.LobbyChoice;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.triplea.ui.events.queue.test.support.EventQueueTestSupport;

class LobbySelectionViewControllerTest extends AbstractClientSettingTestCase {
  final EventQueueTestSupport<
          LobbySelectionSwingEventQueue,
          LobbySelectionViewController.Events,
          LobbySelectionViewData>
      eventQueueTestSupport = new EventQueueTestSupport<>(LobbySelectionSwingEventQueue.class);

  final LobbySelectionViewController lobbySelectionViewController =
      new LobbySelectionViewController(eventQueueTestSupport.getEventQueue());

  /** Ensure we handle all possible events. */
  @ParameterizedTest
  @EnumSource(LobbySelectionView.Events.class)
  void allEventTypesAreHandled(final LobbySelectionView.Events event) {
    lobbySelectionViewController.handleEvent(
        event, LobbySelectionViewControllerTestData.lobbySelectionViewData);
  }

  /** Simulate user clicking radio buttons to select different lobbies. */
  @ParameterizedTest
  @EnumSource(LobbyChoice.class)
  void radioButtonChanged(final LobbyChoice lobbyChoice) {
    final var inputData =
        LobbySelectionViewData.builder()
            .selectedRadioButton(lobbyChoice)
            .uriFieldEnabled(true)
            .uriFieldValue("some value")
            .build();

    // local radio button is being changed from 'other' to 'local'
    // note how uri field value is empty and enabled, should be updated by controller
    // to local host URI and disabled
    lobbySelectionViewController.handleEvent(
        LobbySelectionView.Events.RADIO_BUTTON_CHANGED, inputData);

    assertThat(
        eventQueueTestSupport.popFirstControllerEvent(),
        controllerEventIs(
            LobbySelectionViewController.Events.UPDATE_UI_DATA,
            inputData,
            LobbySelectionViewData.builder()
                .selectedRadioButton(lobbyChoice)
                .uriFieldEnabled(!lobbyChoice.isReadOnly())
                .uriFieldValue(lobbyChoice.getUri())
                .build()));
  }

  /**
   * Send a 'reset to default event', controller should respond with data change event that sets all
   * fields back to default lobby URI values.
   */
  @Test
  void resetToDefault() {
    final LobbySelectionViewData inputData =
        LobbySelectionViewData.builder()
            .selectedRadioButton(LobbyChoice.LOCAL)
            .uriFieldEnabled(true)
            .uriFieldValue("")
            .build();
    lobbySelectionViewController.handleEvent(LobbySelectionView.Events.RESET_TO_DEFAULT, inputData);

    final LobbyChoice defaultLobbyChoice =
        LobbyChoice.fromUri(ClientSetting.lobbyUri.getDefaultValue().orElseThrow());

    assertThat(
        eventQueueTestSupport.popFirstControllerEvent(),
        controllerEventIs(
            LobbySelectionViewController.Events.UPDATE_UI_DATA,
            inputData,
            LobbySelectionViewData.builder()
                .selectedRadioButton(defaultLobbyChoice)
                .uriFieldEnabled(!defaultLobbyChoice.isReadOnly())
                .uriFieldValue(defaultLobbyChoice.getUri())
                .build()));
  }

  /**
   * User has saved an 'other' lobby URI. The response to a reset should be to revert back to the
   * 'other' setting that was saved.
   */
  @Test
  void reset_CaseOtherValueSaved() {
    // by setting 'ClientSetting.lobbyUri' to a custom value,
    // we are setting the Lobby Choice value to 'OTHER'
    ClientSetting.lobbyUri.setValueAndFlush(URI.create("http://127.0.0.1:3000"));

    final LobbySelectionViewData inputData =
        LobbySelectionViewData.builder()
            .selectedRadioButton(LobbyChoice.PROD)
            .uriFieldEnabled(false)
            .uriFieldValue(LobbyChoice.PROD.getUri())
            .build();

    lobbySelectionViewController.handleEvent(LobbySelectionView.Events.RESET, inputData);

    assertThat(
        eventQueueTestSupport.popFirstControllerEvent(),
        controllerEventIs(
            LobbySelectionViewController.Events.UPDATE_UI_DATA,
            inputData,
            LobbySelectionViewData.builder()
                .selectedRadioButton(LobbyChoice.OTHER)
                .uriFieldEnabled(true)
                .uriFieldValue(URI.create("http://127.0.0.1:3000").toString())
                .build()));
  }

  /**
   * User has saved 'local' as lobby selection. The response should update UI to the 'local' lobby
   * preset.
   */
  @Test
  void reset_CaseLocalLobbySaved() {
    ClientSetting.lobbyUri.setValueAndFlush(URI.create(LobbyChoice.LOCAL.getUri()));

    final LobbySelectionViewData inputData =
        LobbySelectionViewData.builder()
            .selectedRadioButton(LobbyChoice.OTHER)
            .uriFieldEnabled(true)
            .uriFieldValue("http://127.0.0.1")
            .build();

    lobbySelectionViewController.handleEvent(LobbySelectionView.Events.RESET, inputData);

    assertThat(
        eventQueueTestSupport.popFirstControllerEvent(),
        controllerEventIs(
            LobbySelectionViewController.Events.UPDATE_UI_DATA,
            inputData,
            LobbySelectionViewData.builder()
                .selectedRadioButton(LobbyChoice.LOCAL)
                .uriFieldEnabled(false)
                .uriFieldValue(LobbyChoice.LOCAL.getUri())
                .build()));
  }
}
