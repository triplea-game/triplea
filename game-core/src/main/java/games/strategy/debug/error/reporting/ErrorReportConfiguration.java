package games.strategy.debug.error.reporting;

import java.net.URI;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JFrame;

import org.triplea.http.client.error.report.ErrorReportClientFactory;

import com.google.common.annotations.VisibleForTesting;

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
    return newReportHandler(ClientSetting.httpLobbyUriOverride::getValue);
  }

  @VisibleForTesting
  static BiConsumer<JFrame, UserErrorReport> newReportHandler(final Supplier<Optional<String>> clientSettingProvider) {
    return httpLobbyUri(clientSettingProvider)
        .map(ErrorReportConfiguration::uploadAction)
        .orElse(ErrorReportUploadAction.OFFLINE_STRATEGY);
  }

  private static Optional<URI> httpLobbyUri(final Supplier<Optional<String>> clientSettingProvider) {
    return Optional.ofNullable(
        clientSettingProvider.get()
            .map(URI::create)
            .orElseGet(() -> LobbyPropertyFetcherConfiguration.lobbyServerPropertiesFetcher()
                .fetchLobbyServerProperties()
                .map(LobbyServerProperties::getHttpServerUri)
                .orElse(null)));
  }

  private static BiConsumer<JFrame, UserErrorReport> uploadAction(final URI uri) {
    return new ErrorReportUploadAction(
        ErrorReportClientFactory.newErrorUploader(uri),
        new ConfirmationDialogController());
  }
}
