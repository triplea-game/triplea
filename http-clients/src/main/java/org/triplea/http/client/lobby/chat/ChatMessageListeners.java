package org.triplea.http.client.lobby.chat;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;

@Builder
@Getter
public class ChatMessageListeners {
  @Nonnull private final Consumer<StatusUpdate> playerStatusListener;
  @Nonnull private final Consumer<UserName> playerLeftListener;
  @Nonnull private final Consumer<ChatParticipant> playerJoinedListener;
  @Nonnull private final Consumer<PlayerSlapped> playerSlappedListener;
  @Nonnull private final Consumer<ChatMessage> chatMessageListener;
  @Nonnull private final Consumer<ChatterList> connectedListener;
  @Nonnull private final Consumer<String> chatEventListener;
  @Nonnull private final Consumer<String> serverErrorListener;
}
