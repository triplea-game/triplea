package games.strategy.engine.auto.update;

import games.strategy.triplea.settings.ClientSetting;
import java.awt.Component;
import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.http.client.latest.version.LatestVersionClient;
import org.triplea.http.client.latest.version.LatestVersionResponse;

@UtilityClass
final class EngineVersionCheck {
  static void checkForLatestEngineVersionOut(final Component parentComponent) {
    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(Instant.now().toEpochMilli());

    LatestVersionClient.fetchLatestVersion(
            ClientSetting.lobbyUri.getValueOrThrow(),
            ProductVersionReader.getCurrentVersion().toString())
        .filter(
            response ->
                !LatestVersionResponse.RecommendedAction.NO_UPDATE
                    .toString()
                    .equals(response.getRecommendedUpdateAction()))
        .ifPresent(response -> OutOfDateDialog.showOutOfDateComponent(parentComponent, response));
  }
}
