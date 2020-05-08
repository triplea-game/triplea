package org.triplea.db.dao.api.key;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.java.Postconditions;

/** Maps ResultSet data when querying for a users API key. */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ApiKeyLookupRecord {
  @Nonnull private Integer apiKeyId;
  @Nullable private Integer userId;
  @Nonnull private String username;
  @Nonnull private String playerChatId;
  @Nonnull private String role;

  /** Returns a JDBI row mapper used to convert a ResultSet into an instance of this bean object. */
  public static RowMapper<ApiKeyLookupRecord> buildResultMapper() {
    return (rs, ctx) -> {
      final var apiKeyLookupRecord =
          ApiKeyLookupRecord.builder()
              .apiKeyId(rs.getInt("api_key_id"))
              .userId(rs.getInt("user_id"))
              .playerChatId(Preconditions.checkNotNull(rs.getString("player_chat_id")))
              .role(Preconditions.checkNotNull(rs.getString("user_role")))
              .username(rs.getString("username"))
              .build();

      verifyState(apiKeyLookupRecord);
      return apiKeyLookupRecord;
    };
  }

  public Integer getUserId() {
    return (userId == null || userId == 0) ? null : userId;
  }

  @VisibleForTesting
  static void verifyState(final ApiKeyLookupRecord apiKeyLookupRecord) {
    Postconditions.assertState(!apiKeyLookupRecord.role.equals(UserRole.HOST));

    if (apiKeyLookupRecord.role.equals(UserRole.ANONYMOUS)) {
      Postconditions.assertState(apiKeyLookupRecord.userId == null);
    } else {
      Postconditions.assertState(apiKeyLookupRecord.userId != null);
    }
  }
}
