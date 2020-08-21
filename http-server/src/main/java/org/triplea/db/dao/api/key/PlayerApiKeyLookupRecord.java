package org.triplea.db.dao.api.key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.java.Postconditions;

/** Maps ResultSet data when querying for a users API key. */
@Getter
@EqualsAndHashCode
@ToString
public class PlayerApiKeyLookupRecord {
  private final int apiKeyId;
  @Nullable private final Integer userId;
  @Nonnull private final String username;
  @Nonnull private final String playerChatId;
  @Nonnull private final String userRole;

  @Builder(toBuilder = true)
  public PlayerApiKeyLookupRecord(
      @ColumnName("api_key_id") final int apiKeyId,
      @Nullable @ColumnName("user_id") final Integer userId,
      @ColumnName("player_chat_id") final String playerChatId,
      @ColumnName("user_role") final String userRole,
      @ColumnName("username") final String username) {
    this.apiKeyId = apiKeyId;
    this.userId = userId;
    this.playerChatId = playerChatId;
    this.userRole = userRole;
    this.username = username;

    verifyState();
  }

  public Integer getUserId() {
    return (userId == null || userId == 0) ? null : userId;
  }

  private void verifyState() {
    Postconditions.assertState(!userRole.equals(UserRole.HOST));

    if (userRole.equals(UserRole.ANONYMOUS)) {
      Postconditions.assertState(userId == null || userId == 0);
    } else {
      Postconditions.assertState(
          userId != null && userId > 0,
          String.format(
              "Non anonymouse users must have a user id, user id: %s, user role: %s",
              userId, userRole));
    }
  }
}
