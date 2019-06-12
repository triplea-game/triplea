package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
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
import org.triplea.http.client.moderator.toolbox.LookupModeratorEventsArgs;
import org.triplea.http.client.moderator.toolbox.ModeratorEvent;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.http.client.moderator.toolbox.UpdateBadWordsArg;
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
class ClientServerIntegrationTest {
  private static final URI LOCALHOST = URI.create("http://localhost:8080");

  private static final DropwizardTestSupport<AppConfig> SUPPORT =
      new DropwizardTestSupport<>(ServerApplication.class, "configuration-prerelease.yml");


  private static ModeratorToolboxClient moderatorToolboxClient;
  private static ErrorUploadClient errorUploadClient;
  private static final String MODERATOR_API_KEY = "pass";
  private static final String PASSWORD = "word";
  private static final String SINGLE_USE_KEY = "password";

  @BeforeAll
  static void beforeClass() {

    try {
      SUPPORT.before();
    } catch (final Exception e) {
      // ignore server is already started
      // This is here to support an already running server to be integration tested.
      // Some care should be taken to ensure the server is restarted when/as expected.
    }
    moderatorToolboxClient = ModeratorToolboxClient.newClient(LOCALHOST, PASSWORD);
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
        moderatorToolboxClient.validateApiKey(MODERATOR_API_KEY),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void validateApiKeyWithInvalidKey() {
    assertThat(
        moderatorToolboxClient.validateApiKey("not-valid"),
        not(is(ModeratorToolboxClient.SUCCESS)));
  }

  @Test
  void registerApiKey() {
    assertThat(
        moderatorToolboxClient.registerNewKey(SINGLE_USE_KEY).getNewApiKey(),
        notNullValue());
  }

  @Test
  void getBadWords() {
    assertThat(moderatorToolboxClient.getBadWords(MODERATOR_API_KEY), notNullValue());
    assertThrows(RuntimeException.class, () -> moderatorToolboxClient.getBadWords("badKey"));
  }

  @Test
  void addBadWord() {
    assertThat(
        moderatorToolboxClient.addBadWord(UpdateBadWordsArg.builder()
            .apiKey(MODERATOR_API_KEY)
            .badWord("bad-word " + Math.random())
            .build()),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void removeBadWord() {
    final String badWord = "bad-word" + Math.random();

    moderatorToolboxClient.addBadWord(UpdateBadWordsArg.builder()
        .apiKey(MODERATOR_API_KEY)
        .badWord(badWord)
        .build());

    assertThat(
        moderatorToolboxClient.removeBadWord(UpdateBadWordsArg.builder()
            .apiKey(MODERATOR_API_KEY)
            .badWord(badWord)
            .build()),
        is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void lookupModeratorEvents() {
    final List<ModeratorEvent> results =
        moderatorToolboxClient.lookupModeratorEvents(LookupModeratorEventsArgs.builder()
            .apiKey(MODERATOR_API_KEY)
            .rowCount(10)
            .rowStart(0)
            .build());

    assertThat(results, notNullValue());
  }
}
