package org.triplea.http.client.web.socket.messages.envelopes.chat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;

import com.google.common.base.Strings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.UserName;

class ChatReceivedMessageTest {

  @Test
  @DisplayName("Verify message at max length is not truncated")
  void messageAtMaxLimit() {
    final String testString = createStringWithLength(ChatReceivedMessage.MAX_MESSAGE_LENGTH);

    final String result = chatMessage(testString);

    assertThat(result, is(testString));
    assertThat(result, not(endsWith(ChatReceivedMessage.ELLIPSES)));
  }

  @Test
  @DisplayName("Verify message over max length is truncated")
  void messageOverMaxLimit() {
    final String testString = createStringWithLength(ChatReceivedMessage.MAX_MESSAGE_LENGTH + 1);

    final String result = chatMessage(testString);

    assertThat(result.length(), is(ChatReceivedMessage.MAX_MESSAGE_LENGTH));
    assertThat(result, endsWith(ChatReceivedMessage.ELLIPSES));
  }

  private static String createStringWithLength(final int length) {
    return Strings.repeat("a", length);
  }

  private static String chatMessage(final String inputMessage) {
    return new ChatReceivedMessage(UserName.of("player-name"), inputMessage).getMessage();
  }
}
