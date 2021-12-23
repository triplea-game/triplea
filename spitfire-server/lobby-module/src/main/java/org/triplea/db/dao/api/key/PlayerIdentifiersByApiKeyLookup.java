package org.triplea.db.dao.api.key;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jdbi.v3.core.mapper.reflect.ColumnName;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;

@Getter
@EqualsAndHashCode
public class PlayerIdentifiersByApiKeyLookup {
  private final UserName userName;
  private final SystemId systemId;
  private final String ip;

  @Builder
  public PlayerIdentifiersByApiKeyLookup(
      @ColumnName("user_name") final String userName,
      @ColumnName("system_id") final String systemId,
      @ColumnName("ip") final String ip) {
    this.userName = UserName.of(userName);
    this.systemId = SystemId.of(systemId);
    this.ip = ip;
  }
}
