package org.triplea.http.client.latest.version;

import com.google.gson.Gson;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
    HttpUrl url =
        HttpUrl.get(serverUri)
            .newBuilder()
            .addPathSegments(ServerPaths.LATEST_VERSION_PATH)
            .addQueryParameter("clientVersion", currentVersion)
            .build();

    Request.Builder request = new Request.Builder().url(url).header("Accept", "application/json");
    AuthenticationHeaders.systemIdHeaders().forEach(request::header);

    OkHttpClient client = new OkHttpClient.Builder().callTimeout(Duration.ofSeconds(20)).build();

    try (Response response = client.newCall(request.build()).execute()) {
      if (response.isSuccessful() && response.body() != null) {
        return Optional.of(
            new Gson().fromJson(response.body().string(), LatestVersionResponse.class));
      } else {
        log.info("Latest Version Response error response, status Code: {}", response.code());
        return Optional.empty();
      }
    } catch (IOException e) {
      log.info("Error sending request for latest version to server: {}", serverUri, e);
      return Optional.empty();
    }
  }
}
