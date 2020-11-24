package org.triplea.storage.server.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.annotation.Nonnull;
import lombok.Builder;

/**
 * Http client class for interacting with a storage server, primarily to replicate uploaded files to
 * the storage server. Note, the expected user of this class would be the 'maps-server' and not
 * game-clients.
 *
 * <p>Storage server clients require a fixed secret key to authenticate with the storage server.
 */
@Builder
public class StorageServerUploadClient {
  public static final String UPLOAD_PATH = "/upload";
  public static final String FOLDER_PARAM = "folder";
  public static final String FILE_NAME_PARAM = "fileName";

  @Nonnull private final URI storageServerUri;
  @Nonnull private final String secretApiKey;
  @Builder.Default private final Duration timeout = Duration.ofMinutes(15);

  /**
   * Uploads a given file to storage server.
   *
   * @param folderName Files are organized in folders, this parameter is the name of the folder to
   *     be created on the storage server where the uploaded file will be written.
   * @param fileName The name of the file to be written on the storage server.
   * @param dataPayload The data contents of the file to be written.
   * @return A response object from the server, generally a successful response will have a body and
   *     a 200 status code, errors will either have a 500 status code with a body (containing an
   *     error message), or will have an expection with null body and null status code.
   */
  public UploadResponse upload(
      final String folderName, final String fileName, final byte[] dataPayload) {

    final HttpClient client = HttpClient.newHttpClient();

    final HttpRequest httpRequest =
        HttpRequest.newBuilder()
            .timeout(timeout)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(dataPayload))
            .uri(
                URI.create(
                    String.format(
                        "%s%s?%s=%s&%s=%s",
                        storageServerUri,
                        UPLOAD_PATH,
                        FOLDER_PARAM,
                        folderName,
                        FILE_NAME_PARAM,
                        fileName)))
            .header("Authorization", "Bearer " + secretApiKey)
            .build();

    try {
      final HttpResponse<String> response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      return UploadResponse.builder()
          .bodyResponse(response.body())
          .statusCode(response.statusCode())
          .build();
    } catch (final InterruptedException e) {
      return UploadResponse.builder().exception(e).build();
    } catch (final IOException e) {
      Thread.currentThread().interrupt();
      return UploadResponse.builder().exception(e).build();
    }
  }
}
