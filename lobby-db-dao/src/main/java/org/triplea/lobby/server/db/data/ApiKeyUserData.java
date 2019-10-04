package org.triplea.lobby.server.db.data;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.RowMapper;

/** Maps ResultSet data when querying for a users API key. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ApiKeyUserData {

  public static final String ROLE_COLUMN = "role";
  public static final String USER_ID_COLUMN = "id";

  private @Nullable Integer userId;
  private String role;

  /** Returns a JDBI row mapper used to convert a ResultSet into an instance of this bean object. */
  public static RowMapper<ApiKeyUserData> buildResultMapper() {
    return (rs, ctx) ->
        ApiKeyUserData.builder()
            .userId(rs.getInt(USER_ID_COLUMN) == 0 ? null : rs.getInt(USER_ID_COLUMN))
            .role(rs.getString(ROLE_COLUMN))
            .build();
  }
}
