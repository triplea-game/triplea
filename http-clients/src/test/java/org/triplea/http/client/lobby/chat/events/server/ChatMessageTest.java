package org.triplea.http.client.lobby.chat.events.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;

import com.google.common.base.Strings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.PlayerName;

class ChatMessageTest {

  @Test
  @DisplayName("Verify a message under max line length is left untouched")
  void messageUnderMaxLineLength() {
    final String testString = createStringWithLength(ChatMessage.MAX_LINE_LENGTH - 1);

    final String result = chatMessage(testString);

    assertThat("expecting no difference, string is under the length limit", result, is(testString));
  }

  @Test
  @DisplayName("Verify a message at max line length is left untouched")
  void messageAtMaxLineLength() {
    final String testString = createStringWithLength(ChatMessage.MAX_LINE_LENGTH);

    final String result = chatMessage(testString);

    assertThat("expecting no difference, string is at the limit", result, is(testString));
  }

  @Test
  @DisplayName("Verify a message over max line length is split untouched")
  void messageOverMaxLineLength() {
    final String testString = createStringWithLength(ChatMessage.MAX_LINE_LENGTH + 1);

    final String result = chatMessage(testString);

    assertThat(
        "String is expected to be split, should see a newline at the max line length",
        String.valueOf(result.charAt(ChatMessage.MAX_LINE_LENGTH)),
        is("\n"));
    assertThat(
        "Expecting a single new line to have been inserted",
        result.length(),
        is(testString.length() + "\n".length()));
  }

  @Test
  @DisplayName("Verify message at max length is not truncated")
  void messageAtMaxLimit() {
    final String testString = createStringWithLength(ChatMessage.MAX_MESSAGE_LENGTH);

    final String result = chatMessage(testString);

    assertThat(result, not(endsWith(ChatMessage.ELLIPSES)));
  }

  @Test
  @DisplayName("Verify message over max length is truncated")
  void messageOverMaxLimit() {
    final String testString = createStringWithLength(ChatMessage.MAX_MESSAGE_LENGTH + 1);

    final String result = chatMessage(testString);

    assertThat(result, endsWith(ChatMessage.ELLIPSES));
  }

  private static String createStringWithLength(final int length) {
    return Strings.repeat("a", length);
  }

  private static String chatMessage(final String inputMessage) {
    return new ChatMessage(PlayerName.of("player-name"), inputMessage).getMessage();
  }
}
