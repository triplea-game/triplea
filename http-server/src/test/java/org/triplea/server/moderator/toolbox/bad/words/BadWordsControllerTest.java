package org.triplea.server.moderator.toolbox.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@ExtendWith(MockitoExtension.class)
class BadWordsControllerTest {
  private static final String TEST_VALUE = "some-value";
  private static final int MODERATOR_ID = 10;
  private static final ImmutableList<String> BAD_WORD_LIST =
      ImmutableList.of("bad-word", "another-bad-word");

  @Mock private ApiKeyValidationService apiKeyValidationService;
  @Mock private BadWordsService badWordsService;
  @InjectMocks private BadWordsController badWordsController;

  @Mock private HttpServletRequest servletRequest;

  @Test
  void removeBadWordNothingRemoved() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(servletRequest))
        .thenReturn(MODERATOR_ID);
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void removeBadWordSuccess() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(servletRequest))
        .thenReturn(MODERATOR_ID);
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  void addBadWordNothingAdded() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(servletRequest))
        .thenReturn(MODERATOR_ID);
    when(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.addBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void addBadWordSuccess() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(servletRequest))
        .thenReturn(MODERATOR_ID);
    when(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.addBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  void getBadWords() {
    when(badWordsService.getBadWords()).thenReturn(BAD_WORD_LIST);

    final Response response = badWordsController.getBadWords(servletRequest);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(BAD_WORD_LIST));
    verify(apiKeyValidationService).verifyApiKey(servletRequest);
  }
}
