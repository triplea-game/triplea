package org.triplea.modules.moderation.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.modules.TestData;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class BadWordsControllerTest {
  private static final String TEST_VALUE = "some-value";

  private static final AuthenticatedUser AUTHENTICATED_USER = TestData.AUTHENTICATED_USER;
  private static final ImmutableList<String> BAD_WORD_LIST =
      ImmutableList.of("bad-word", "another-bad-word");

  @Mock private BadWordsService badWordsService;
  @InjectMocks private BadWordsController badWordsController;

  @Test
  void removeBadWordNothingRemoved() {
    when(badWordsService.removeBadWord(AUTHENTICATED_USER.getUserId(), TEST_VALUE))
        .thenReturn(false);

    final Response response = badWordsController.removeBadWord(AUTHENTICATED_USER, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void removeBadWordSuccess() {
    when(badWordsService.removeBadWord(AUTHENTICATED_USER.getUserId(), TEST_VALUE))
        .thenReturn(true);

    final Response response = badWordsController.removeBadWord(AUTHENTICATED_USER, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  void addBadWordNothingAdded() {
    when(badWordsService.addBadWord(AUTHENTICATED_USER.getUserId(), TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.addBadWord(AUTHENTICATED_USER, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void addBadWordSuccess() {
    when(badWordsService.addBadWord(AUTHENTICATED_USER.getUserId(), TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.addBadWord(AUTHENTICATED_USER, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  void getBadWords() {
    when(badWordsService.getBadWords()).thenReturn(BAD_WORD_LIST);

    final Response response = badWordsController.getBadWords();

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(BAD_WORD_LIST));
  }
}
