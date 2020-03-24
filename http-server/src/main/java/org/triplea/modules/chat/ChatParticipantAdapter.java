package org.triplea.modules.chat;

import java.util.function.Function;
import org.triplea.db.dao.api.key.UserWithRoleRecord;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;

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
