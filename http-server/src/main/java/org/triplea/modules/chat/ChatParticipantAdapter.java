package org.triplea.modules.chat;

import java.util.function.Function;
import org.triplea.db.dao.api.key.UserWithRoleRecord;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ChatParticipant;

class ChatParticipantAdapter implements Function<UserWithRoleRecord, ChatParticipant> {
  @Override
  public ChatParticipant apply(final UserWithRoleRecord userWithRoleRecord) {
    return ChatParticipant.builder()
        .userName(userWithRoleRecord.getUsername())
        .isModerator(
            userWithRoleRecord.getRole().equals(UserRole.ADMIN)
                || userWithRoleRecord.getRole().equals(UserRole.MODERATOR))
        .playerChatId(userWithRoleRecord.getPlayerChatId())
        .build();
  }
}
