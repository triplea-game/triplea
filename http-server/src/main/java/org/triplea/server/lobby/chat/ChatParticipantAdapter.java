package org.triplea.server.lobby.chat;

import java.util.function.Function;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.lobby.server.db.dao.api.key.UserWithRoleRecord;
import org.triplea.lobby.server.db.data.UserRole;

public class ChatParticipantAdapter implements Function<UserWithRoleRecord, ChatParticipant> {
  @Override
  public ChatParticipant apply(final UserWithRoleRecord userWithRoleRecord) {
    return ChatParticipant.builder()
        .userName(UserName.of(userWithRoleRecord.getUsername()))
        .isModerator(
            userWithRoleRecord.getRole().equals(UserRole.ADMIN)
                || userWithRoleRecord.getRole().equals(UserRole.MODERATOR))
        .playerChatId(PlayerChatId.of(userWithRoleRecord.getPlayerChatId()))
        .build();
  }
}
