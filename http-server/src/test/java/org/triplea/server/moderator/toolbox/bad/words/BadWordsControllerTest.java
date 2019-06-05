package org.triplea.server.moderator.toolbox.bad.words;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeySecurityService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
class BadWordsControllerTest {
  private static final String TEST_VALUE = "some-value";
  private static final int MODERATOR_ID = 10;
  private static final List<String> BAD_WORD_LIST = ImmutableList.of("bad-word", "another-bad-word");

  @Mock
  private ApiKeySecurityService apiKeySecurityService;
  @Mock
  private ApiKeyValidationService validationService;
  @Mock
  private BadWordsService badWordsService;
  @InjectMocks
  private BadWordsController badWordsController;

  @Mock
  private HttpServletRequest servletRequest;


  private List<Supplier<Response>> responseSupplier;

  @BeforeEach
  void setup() {
    responseSupplier = Arrays.asList(
        () -> badWordsController.addBadWord(servletRequest, TEST_VALUE),
        () -> badWordsController.removeBadWord(servletRequest, TEST_VALUE),
        () -> badWordsController.getBadWords(servletRequest));
  }


  @Test
  void validationNotAllowed() {
    when(apiKeySecurityService.allowValidation(servletRequest)).thenReturn(false);

    responseSupplier.forEach(response -> assertThat(response.get().getStatus(), is(401)));

    verify(validationService, never()).lookupModeratorIdByApiKey(any());
    verify(badWordsService, never()).removeBadWord(anyInt(), any());
    verify(badWordsService, never()).addBadWord(anyInt(), any());
    verify(badWordsService, never()).getBadWords();
  }

  @Test
  void invalidKey() {
    when(apiKeySecurityService.allowValidation(servletRequest)).thenReturn(true);
    when(validationService.lookupModeratorIdByApiKey(servletRequest)).thenReturn(Optional.empty());

    responseSupplier.forEach(response -> assertThat(response.get().getStatus(), is(403)));

    verify(badWordsService, never()).removeBadWord(anyInt(), any());
    verify(badWordsService, never()).addBadWord(anyInt(), any());
    verify(badWordsService, never()).getBadWords();
  }


  @Test
  void removeBadWordNothingRemoved() {
    givenValidKey();
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void removeBadWordSuccess() {
    givenValidKey();
    when(badWordsService.removeBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.removeBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(ModeratorToolboxClient.SUCCESS));
  }

  private void givenValidKey() {
    when(apiKeySecurityService.allowValidation(servletRequest)).thenReturn(true);
    when(validationService.lookupModeratorIdByApiKey(servletRequest)).thenReturn(Optional.of(MODERATOR_ID));
  }


  @Test
  void addBadWordNothingAdded() {
    givenValidKey();
    when(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(false);

    final Response response = badWordsController.addBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(400));
  }

  @Test
  void addBadWordSuccess() {
    givenValidKey();
    when(badWordsService.addBadWord(MODERATOR_ID, TEST_VALUE)).thenReturn(true);

    final Response response = badWordsController.addBadWord(servletRequest, TEST_VALUE);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(ModeratorToolboxClient.SUCCESS));

  }

  @Test
  void getBadWords() {
    givenValidKey();
    when(badWordsService.getBadWords()).thenReturn(BAD_WORD_LIST);

    final Response response = badWordsController.getBadWords(servletRequest);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(BAD_WORD_LIST));
  }
}
