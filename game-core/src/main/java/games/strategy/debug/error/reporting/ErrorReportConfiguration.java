package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiConsumer;

import javax.swing.JFrame;

import org.triplea.http.client.error.report.ErrorReportClientFactory;

import games.strategy.engine.lobby.client.login.LobbyPropertyFetcherConfiguration;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.triplea.settings.ClientSetting;

class ErrorReportConfiguration {

  /**
   * Creates a 'report handler', this is the component invoked when we have collected all data from
   * user and wish to then send that data. The report handler is responsible for showing a waiting
   * dialog and sending error report data to the remote http server.
   */
  static BiConsumer<JFrame, UserErrorReport> newReportHandler() {
    return newFromUserOverride()
        .orElseGet(ErrorReportConfiguration::newFromRemoteProperties);
  }

  private static Optional<BiConsumer<JFrame, UserErrorReport>> newFromUserOverride() {
    return ClientSetting.httpLobbyUriOverride
        .getValue()
        .map(URI::create)
        .map(ErrorReportClientFactory::newErrorUploader)
        .map(ErrorReportUploadAction::new);
  }

  private static BiConsumer<JFrame, UserErrorReport> newFromRemoteProperties() {
    final LobbyServerProperties props =
        LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher().fetchLobbyServerProperties();
    return new ErrorReportUploadAction(ErrorReportClientFactory.newErrorUploader(props.getHttpServerUri()));
  }
}
