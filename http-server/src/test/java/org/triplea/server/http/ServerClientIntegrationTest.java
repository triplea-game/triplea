package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.http.client.error.report.ErrorUploadClient;
import org.triplea.http.client.error.report.ErrorUploadRequest;
import org.triplea.http.client.moderator.toolbox.AddBadWordArgs;
import org.triplea.http.client.moderator.toolbox.LookupModeratorEventsArgs;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.moderator.toolbox.RemoveBadWordArgs;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;

import io.dropwizard.testing.DropwizardTestSupport;

/**
 * Exercises http clients against a live http server to verify the clients can get valid-looking
 * responses. Note, as an integration test we are only verifying general functionality and are not
 * testing logic (that is left for unit testing).
 * <p>
 * Note, we do all http clients in one test to avoid repeatedly starting/stopping server. The start/stop
 * makes tests slower and less reliable (server may not shutdown cleanly or startup fast enough).
 * </p>
 */
@Integration
@DataSet("integration.yml")
@ExtendWith(DBUnitExtension.class)
class ServerClientIntegrationTest {
  private static final URI LOCALHOST = URI.create("http://localhost:8080");

  private static final DropwizardTestSupport<AppConfig> SUPPORT =
      new DropwizardTestSupport<>(ServerApplication.class, "configuration-prerelease.yml");


  private static ModeratorToolboxClient moderatorToolboxClient;
  private static ErrorUploadClient errorUploadClient;
  private static final String moderatorApiKey = "password";

  @BeforeAll
  static void beforeClass() {
    SUPPORT.before();
    moderatorToolboxClient = ModeratorToolboxClient.newClient(LOCALHOST);
    errorUploadClient = ErrorUploadClient.newClient(LOCALHOST);
  }

  @AfterAll
  static void afterClass() {
    SUPPORT.after();
  }

  @Test
  void canSubmitErrorReport() {
    errorUploadClient.canSubmitErrorReport();
  }

  @Test
  void uploadErrorReport() {
    assertThat(
        errorUploadClient.uploadErrorReport(ErrorUploadRequest.builder()
            .body("body")
            .title("title")
            .build()),
        notNullValue());
  }

  @Test
  void validateApiKey() {
    assertThat(
        moderatorToolboxClient.validateApiKey(moderatorApiKey),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void validateApiKeyWithInvalidKey() {
    assertThat(
        moderatorToolboxClient.validateApiKey("not-valid"),
        not(is(ModeratorToolboxClient.SUCCESS)));
  }

  @Test
  void getBadWords() {
    assertThat(moderatorToolboxClient.getBadWords(moderatorApiKey), not(empty()));
    assertThrows(RuntimeException.class, () -> moderatorToolboxClient.getBadWords("badKey"));
  }

  @Test
  void addBadWord() {
    assertThat(
        moderatorToolboxClient.addBadWord(AddBadWordArgs.builder()
            .apiKey(moderatorApiKey)
            .badWord("bad-word " + Math.random())
            .build()),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void removeBadWord() {
    final String badWord = "bad-word" + Math.random();

    moderatorToolboxClient.addBadWord(AddBadWordArgs.builder()
        .apiKey(moderatorApiKey)
        .badWord(badWord)
        .build());

    assertThat(
        moderatorToolboxClient.removeBadWord(RemoveBadWordArgs.builder()
            .apiKey(moderatorApiKey)
            .badWord(badWord)
            .build()),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void lookupModeratorEvents() {
    final List<ModeratorEvent> results =
        moderatorToolboxClient.lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(moderatorApiKey)
            .rowCount(10)
            .rowStart(30)
            .build());

    assertThat(results, notNullValue());
  }
}
