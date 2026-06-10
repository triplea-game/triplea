package org.triplea.http.client.latest.version;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.ServerPaths;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Fetches latest version response from TripleA servers, used to determine if the current version is
 * out of date, and if the user should upgrade.
 */
@UtilityClass
@Slf4j
public class LatestVersionClient {

  public static Optional<LatestVersionResponse> fetchLatestVersion(
      URI serverUri, String currentVersion) {
    URI requestUri =
        URI.create(
            serverUri
                + ServerPaths.LATEST_VERSION_PATH
                + "?clientVersion="
                + URLEncoder.encode(currentVersion, StandardCharsets.UTF_8));
    HttpRequest.Builder request =
        HttpRequest.newBuilder(requestUri)
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json");
    AuthenticationHeaders.systemIdHeaders().forEach(request::header);

    HttpResponse<String> response = null;
    try {
      response =
          HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      log.info("Error sending request for latest version to server: {}", serverUri, e);
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }

    if (response.statusCode() >= 200 && response.statusCode() < 300) {
      return Optional.of(new Gson().fromJson(response.body(), LatestVersionResponse.class));
    } else {
      log.info("Latest Version Response error response, status Code: {}", response.statusCode());
      return Optional.empty();
    }
  }
}
