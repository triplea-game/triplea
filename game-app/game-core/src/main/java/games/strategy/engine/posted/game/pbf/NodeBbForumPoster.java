package games.strategy.engine.posted.game.pbf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NonNls;
import org.triplea.awt.OpenFileUtility;
import org.triplea.yaml.YamlReader;

/**
 * Posts turn summaries to a NodeBB based forum of your choice.
 *
 * <p>URL format is {@code https://your.forumurl.com/api/v2/topics/<topicID>}.
 */
public class NodeBbForumPoster {

  @NonNls
  public static final String AXIS_AND_ALLIES_ORG_DISPLAY_NAME = "www.axisandallies.org/forums/";

  @NonNls public static final String TRIPLEA_FORUM_DISPLAY_NAME = "forums.triplea-game.org";

  private final int topicId;
  private final String token;
  private final String forumUrl;

  @Builder
  public static class ForumPostingParameters {
    @Nonnull private final Integer topicId;
    @Nonnull private final char[] token;
    @Nonnull private final String forumUrl;
  }

  @Builder
  public static class SaveGameParameter {
    @Nonnull private final Path path;
    @Nonnull private final String displayName;
  }

  private NodeBbForumPoster(final ForumPostingParameters forumPostingParameters) {
    this.topicId = forumPostingParameters.topicId;
    this.forumUrl = forumPostingParameters.forumUrl;
    this.token = new String(forumPostingParameters.token);
  }

  /**
   * Creates a {@link NodeBbForumPoster} instance based on the given arguments and the configured
   * settings.
   */
  public static NodeBbForumPoster newInstanceByName(final String name, final int topicId) {
    switch (name) {
      case NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME:
        return new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .token(ClientSetting.tripleaForumToken.getValue().orElse(new char[0]))
                .forumUrl(UrlConstants.TRIPLEA_FORUM)
                .build());
      case NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME:
        return new NodeBbForumPoster(
            ForumPostingParameters.builder()
                .topicId(topicId)
                .token(ClientSetting.aaForumToken.getValue().orElse(new char[0]))
                .forumUrl(UrlConstants.AXIS_AND_ALLIES_FORUM)
                .build());
      default:
        throw new IllegalArgumentException(String.format("String '%s' must be a valid name", name));
    }
  }

  public static ImmutableSet<String> availablePosters() {
    return ImmutableSet.of(
        NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME,
        NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME);
  }

  /**
   * Called when the turn summary should be posted.
   *
   * @param summary the forum summary
   * @param title the forum title
   */
  public CompletableFuture<String> postTurnSummary(
      final String summary, final String title, @Nullable final SaveGameParameter saveGame) {
    try (CloseableHttpClient client = HttpClients.custom().disableCookieManagement().build()) {
      post(client, token, "### " + title + "\n" + summary, saveGame);
      return CompletableFuture.completedFuture("Successfully posted!");
    } catch (final IOException | IllegalStateException e) {
      final CompletableFuture<String> result = new CompletableFuture<>();
      result.completeExceptionally(e);
      return result;
    }
  }

  private void post(
      final CloseableHttpClient client,
      final String token,
      String turnSummaryText,
      @Nullable final SaveGameParameter saveGame)
      throws IOException {
    final HttpPost post = new HttpPost(forumUrl + "/api/v2/topics/" + topicId);
    addTokenHeader(post, token);

    if (saveGame != null) {
      final String saveGameUrl = uploadSaveGame(client, token, saveGame);
      turnSummaryText += "\n[Savegame](" + saveGameUrl + ")";
    }

    post.setEntity(
        new UrlEncodedFormEntity(
            List.of(new BasicNameValuePair("content", turnSummaryText)), StandardCharsets.UTF_8));
    HttpProxy.addProxy(post);
    try (CloseableHttpResponse response = client.execute(post)) {
      final int code = response.getStatusLine().getStatusCode();
      if (code != HttpURLConnection.HTTP_OK) {
        final var message =
            response.getEntity() == null
                ? ""
                : " and message '" + EntityUtils.toString(response.getEntity()) + '\'';
        throw new IllegalStateException(
            String.format("Forum responded with code %s%s", code, message));
      }
    }
  }

  private String uploadSaveGame(
      final CloseableHttpClient client, final String token, final SaveGameParameter saveGame)
      throws IOException {
    final HttpPost fileUpload = new HttpPost(forumUrl + "/api/v2/util/upload");
    fileUpload.setEntity(
        MultipartEntityBuilder.create()
            .addBinaryBody(
                "files[]",
                saveGame.path.toFile(),
                ContentType.APPLICATION_OCTET_STREAM,
                saveGame.displayName)
            .build());
    HttpProxy.addProxy(fileUpload);
    addTokenHeader(fileUpload, token);
    try (CloseableHttpResponse response = client.execute(fileUpload)) {
      final int status = response.getStatusLine().getStatusCode();
      if (status == HttpURLConnection.HTTP_OK) {
        final String json = EntityUtils.toString(response.getEntity());
        return parseSaveGameUrlFromJsonResponse(json);
      }
      throw new IllegalStateException(
          "Failed to upload save game, server returned Error Code "
              + status
              + "\nMessage:\n"
              + EntityUtils.toString(response.getEntity()));
    }
  }

  @VisibleForTesting
  @SuppressWarnings("unchecked")
  static String parseSaveGameUrlFromJsonResponse(String json) {
    try {
      // unchecked casts are fine here because we "consume" the types immediately, so
      // we will get a ClassCastException when the format is unexpected on an inner level.
      // (top level mismatches i.e. list instead of map will throw an exception in the YamlReader
      // class)
      return ((Map<String, List<Map<String, String>>>)
              YamlReader.readMap(json).get("response")) // first get response element
          .get("images") // get images element of the response
          .get(0) // get first element of the images list (we expect only one)
          .get("url");
    } catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e) {
      throw new UnableToParseResponseException(json, e);
    } catch (YamlReader.InvalidYamlFormatException ignored) {
      // This is expected for older forum versions that return a list as top level item
    }

    try {
      Map<String, Object> responsePayload = YamlReader.readList(json).get(0);
      return (String) responsePayload.get("url");
    } catch (YamlReader.InvalidYamlFormatException
        | NullPointerException
        | IndexOutOfBoundsException
        | ClassCastException e) {
      throw new UnableToParseResponseException(json, e);
    }
  }

  private static class UnableToParseResponseException extends IllegalStateException {
    private static final long serialVersionUID = -4435748866723675027L;

    UnableToParseResponseException(String json, Exception cause) {
      super(
          "Unexpected forum response when uploading save game.\n"
              + "Please report this to TripleA.\n"
              + "Unable to properly attach save-game to forum post.\n"
              + "You will need to manually attach a save-game to the latest forum post.\n"
              + "Forums response was: "
              + json,
          cause);
    }
  }

  /** Opens a browser and go to the forum post, identified by the forumId. */
  public void viewPosted() {
    OpenFileUtility.openUrl(null, forumUrl + "/topic/" + topicId);
  }

  private static void addTokenHeader(final HttpRequestBase request, final String token) {
    request.addHeader("Authorization", "Bearer " + token);
  }
}
