package org.triplea.server.lobby.chat;

import java.util.function.Function;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;

public class ChatParticipantAdapter implements Function<ApiKeyUserData, ChatParticipant> {
  @Override
  public ChatParticipant apply(final ApiKeyUserData apiKeyUserData) {
    return ChatParticipant.builder()
        .playerName(PlayerName.of(apiKeyUserData.getUsername()))
        .isModerator(
            apiKeyUserData.getRole().equals(UserRole.ADMIN)
                || apiKeyUserData.getRole().equals(UserRole.MODERATOR))
        .build();
  }
}
