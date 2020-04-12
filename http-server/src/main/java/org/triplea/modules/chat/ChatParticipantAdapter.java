package org.triplea.modules.chat;

import java.util.function.Function;
import org.triplea.db.dao.api.key.ApiKeyLookupRecord;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ChatParticipant;

class ChatParticipantAdapter implements Function<ApiKeyLookupRecord, ChatParticipant> {
  @Override
  public ChatParticipant apply(final ApiKeyLookupRecord apiKeyLookupRecord) {
    return ChatParticipant.builder()
        .userName(apiKeyLookupRecord.getUsername())
        .isModerator(
            apiKeyLookupRecord.getRole().equals(UserRole.ADMIN)
                || apiKeyLookupRecord.getRole().equals(UserRole.MODERATOR))
        .playerChatId(apiKeyLookupRecord.getPlayerChatId())
        .build();
  }
}
