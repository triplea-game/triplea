package org.triplea.http.client.lobby.chat.messages.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ascii;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import lombok.Value;
import org.triplea.domain.data.PlayerName;

@Value
public class ChatMessage {
  @VisibleForTesting static final int MAX_MESSAGE_LENGTH = 200;
  @VisibleForTesting static final int MAX_LINE_LENGTH = 100;
  @VisibleForTesting static final String ELLIPSES = "..";

  private final PlayerName from;
  private final String message;

  public ChatMessage(final PlayerName from, final String message) {
    this.from = Preconditions.checkNotNull(from);
    this.message =
        Joiner.on("\n")
            .join(
                Splitter.fixedLength(MAX_LINE_LENGTH)
                    .splitToList(
                        Ascii.truncate(
                            Preconditions.checkNotNull(message), MAX_MESSAGE_LENGTH, ELLIPSES)));
  }
}
