package org.triplea.lobby.server.db.dao;

import java.util.Optional;
import javax.annotation.Nullable;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.ApiKeyUserData;
import org.triplea.lobby.server.db.data.UserRole;

/**
 * Dao for interacting with api_key table. Api_key table stores keys that are generated on login.
 * For non-anonymous accounts, the key is linked back to the players account which is used to
 * determine the users 'Role'. Anonymous users are still granted API keys, they have no user id and
 * given role 'ANONYMOUS'.
 */
public interface ApiKeyDao {
  @SqlUpdate("insert into api_key(lobby_user_id, key) values(:userId, :apiKey)")
  void storeKey(@Nullable @Bind("userId") Integer playerId, @Bind("apiKey") String key);

  @SqlQuery(
      "select lu.id, coalesce(lu.role, '"
          + UserRole.ANONYMOUS
          + "') as role "
          + "from api_key a "
          + "left join lobby_user lu on lu.id = a.lobby_user_id "
          + "where a.key = :apiKey")
  Optional<ApiKeyUserData> lookupByApiKey(@Bind("apiKey") String apiKey);

  @SqlUpdate("delete from api_key where date_created < (now() - '7 days'::interval)")
  void deleteOldKeys();
}
