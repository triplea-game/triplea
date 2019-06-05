package org.triplea.http.client.moderator.toolbox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.HttpCommunicationException;

@ExtendWith(MockitoExtension.class)
class ModeratorToolboxClientTest {

  private static final String TEST_VALUE = "test-value";
  private static final String RETURN_VALUE = "return-value";
  private static final String EXCEPTION_MESSAGE = "exception-message";
  private static final HttpCommunicationException EXCEPTION = new HttpCommunicationException(500, EXCEPTION_MESSAGE);
  private static final List<String> BAD_WORDS = Arrays.asList("word1", "word2");

  private static final String API_KEY = "api-key";

  private static final Map<String, Object> expectedHeader;

  static {
    expectedHeader = new HashMap<>();
    expectedHeader.put(ModeratorToolboxClient.MODERATOR_API_KEY_HEADER, API_KEY);
  }

  @Mock
  private ModeratorToolboxFeignClient moderatorToolboxFeignClient;

  @InjectMocks
  private ModeratorToolboxClient moderatorToolboxClient;

  @Test
  void validateApiKey() {
    when(moderatorToolboxFeignClient.validateApiKey(TEST_VALUE))
        .thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.validateApiKey(TEST_VALUE),
        is(RETURN_VALUE));
  }

  @Test
  void validateApiKeyWithException() {
    when(moderatorToolboxFeignClient.validateApiKey(TEST_VALUE)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.validateApiKey(TEST_VALUE),
        containsString(EXCEPTION_MESSAGE));
  }

  @Test
  void addBadWord() {
    when(moderatorToolboxFeignClient.addBadWord(expectedHeader, TEST_VALUE)).thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.addBadWord(AddBadWordArgs.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        is(RETURN_VALUE));
  }


  @Test
  void addBadWordWithException() {
    when(moderatorToolboxFeignClient.addBadWord(expectedHeader, TEST_VALUE)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.addBadWord(AddBadWordArgs.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        containsString(EXCEPTION_MESSAGE));
  }


  @Test
  void removeBadWord() {
    when(moderatorToolboxFeignClient.removeBadWord(expectedHeader, TEST_VALUE)).thenReturn(RETURN_VALUE);

    assertThat(
        moderatorToolboxClient.removeBadWord(RemoveBadWordArgs.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        is(RETURN_VALUE));
  }


  @Test
  void removeBadWordWithException() {
    when(moderatorToolboxFeignClient.removeBadWord(expectedHeader, TEST_VALUE)).thenThrow(EXCEPTION);

    assertThat(
        moderatorToolboxClient.removeBadWord(RemoveBadWordArgs.builder()
            .apiKey(API_KEY)
            .badWord(TEST_VALUE)
            .build()),
        containsString(EXCEPTION_MESSAGE));
  }

  @Test
  void getBadWords() {
    when(moderatorToolboxFeignClient.getBadWords(expectedHeader)).thenReturn(BAD_WORDS);

    assertThat(
        moderatorToolboxClient.getBadWords(API_KEY),
        is(BAD_WORDS));
  }

  @Test
  void getBadWordsWithException() {
    when(moderatorToolboxFeignClient.getBadWords(expectedHeader)).thenThrow(EXCEPTION);

    assertThrows(EXCEPTION.getClass(), () -> moderatorToolboxClient.getBadWords(API_KEY));
  }
}
