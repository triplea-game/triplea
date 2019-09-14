package org.triplea.server.moderator.toolbox.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BadWordsControllerTest {
  private static final String TEST_VALUE = "some-value";
  // TODO: Project#12 fix test expectation of MODERATOR_ID being hardcoded as zero
  private static final int MODERATOR_ID = 0;
  private static final ImmutableList<String> BAD_WORD_LIST =
      ImmutableList.of("bad-word", "another-bad-word");

  @Mock private BadWordsService badWordsService;
  @InjectMocks private BadWordsController badWordsController;

  @Mock private HttpServletRequest servletRequest;

  @Test
  void removeBadWordNothingRemoved() {
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void removeBadWordSuccess() {
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
  }

  @Test
  void addBadWordNothingAdded() {
    when(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.addBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void addBadWordSuccess() {
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
  }
}
