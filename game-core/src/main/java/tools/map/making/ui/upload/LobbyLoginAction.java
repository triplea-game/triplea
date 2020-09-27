package tools.map.making.ui.upload;

import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LoginMode;
import games.strategy.engine.lobby.client.login.LoginResult;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Optional;
import javax.swing.JFrame;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.live.servers.ServerProperties;

@AllArgsConstructor
class LobbyLoginAction implements ActionListener {

  private final JFrame parentWindow;
  private final UploadPanelState uploadPanelState;


  @Override
  public void actionPerformed(final ActionEvent actionEvent) {
    lobbyLogin()
        .ifPresentOrElse(
            apiKey -> uploadPanelState.setApiKey(apiKey),
            () -> uploadPanelState.setApiKey(null)
        );
  }

  Optional<ApiKey> lobbyLogin() {
    return LiveServersFetcher.fetch() //
        .flatMap(this::promptLogin)
        .map(LoginResult::getApiKey);
  }

  private Optional<LoginResult> promptLogin(final ServerProperties serverProperties) {
    return new LobbyLogin(parentWindow, serverProperties)
        .promptLogin(LoginMode.REGISTRATION_REQUIRED);
  }

}
