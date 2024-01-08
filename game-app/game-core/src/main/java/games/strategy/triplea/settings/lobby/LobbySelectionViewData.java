package games.strategy.triplea.settings.lobby;

import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.net.URI;
import java.util.Arrays;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.triplea.ui.events.queue.ViewData;

/** Represents the UI data for the lobby selection panel that is in preferences. */
@Builder(toBuilder = true)
@AllArgsConstructor
@Value
class LobbySelectionViewData implements ViewData {

  /**
   * The different choices a user has for which lobby they would like to connect to. Radio buttons
   * are used to create pre-filled in URIs to make it easy to switch between the different lobbies.
   * An 'other' option is available in case a different port number is in use, or if someone is
   * custom hosting a lobby.
   */
  @AllArgsConstructor
  @Getter
  enum LobbyChoice {
    PROD("Latest Stable", () -> UrlConstants.PROD_LOBBY, true),
    LOCAL("Local", () -> "http://localhost", true),
    OTHER("Other", () -> ClientSetting.lobbyUri.getValueOrThrow().toString(), false);

    private final String displayString;
    private final Supplier<String> uri;
    private final boolean readOnly;

    public String getUri() {
      return uri.get();
    }

    static LobbyChoice fromUri(final URI uri) {
      return Arrays.stream(values())
          .filter(lobbyChoice -> URI.create(lobbyChoice.uri.get()).equals(uri))
          .findAny()
          .orElse(OTHER);
    }

    static LobbyChoice fromDisplayText(final String selectedRadioButton) {
      return Arrays.stream(values())
          .filter(choice -> choice.getDisplayString().equals(selectedRadioButton))
          .findAny()
          .orElse(LobbyChoice.PROD);
    }

    LobbySelectionViewData toLobbySelectionViewData() {
      return LobbySelectionViewData.builder()
          .selectedRadioButton(this)
          .uriFieldValue(uri.get())
          .uriFieldEnabled(!readOnly)
          .build();
    }
  }

  /** Which lobby choice radio button is selected eg: prod / local / other */
  @Nonnull LobbyChoice selectedRadioButton;

  /** The string text in the uri field. */
  @Nonnull String uriFieldValue;

  /** Whether a user can edit the uri field. */
  @Nonnull Boolean uriFieldEnabled;

  /**
   * Initializes UI data, initial state is recalled from previous selections persisted in
   * ClientSettings, otherwise default selection is to use the 'prod' lobby.
   */
  LobbySelectionViewData() {
    uriFieldValue = ClientSetting.lobbyUri.getValue().orElseThrow().toString();
    selectedRadioButton = LobbyChoice.fromUri(ClientSetting.lobbyUri.getValue().orElseThrow());
    uriFieldEnabled = !selectedRadioButton.isReadOnly();
  }
}
