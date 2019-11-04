package org.triplea.lobby.server.db.dao.api.key;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.UserWithRoleRecord;

/**
 * Dao for interacting with api_key table. Api_key table stores keys that are generated on login.
 * For non-anonymous accounts, the key is linked back to the players account which is used to
 * determine the users 'Role'. Anonymous users are still granted API keys, they have no user id and
 * given role 'ANONYMOUS'.
 */
interface LobbyApiKeyDao {
  @SqlUpdate(
      "insert into api_key(lobby_user_id, username, key, ip, user_role_id) "
          + "values (:userId, :username, :apiKey, :ip::inet, :userRoleId)")
  int storeKey(
      @Bind("userId") Integer userId,
      @Bind("username") String username,
      @Bind("apiKey") String key,
      @Bind("ip") String ipAddress,
      @Bind("userRoleId") int userRoleId);

  @SqlQuery(
      "select lu.id, "
          + "ak.username as "
          + UserWithRoleRecord.USERNAME_COLUMN
          + ", ur.name as "
          + UserWithRoleRecord.ROLE_COLUMN
          + " from api_key ak "
          + " join user_role ur on ur.id = ak.user_role_id "
          + " left join lobby_user lu on lu.id = ak.lobby_user_id "
          + " where ak.key = :apiKey")
  Optional<UserWithRoleRecord> lookupByApiKey(@Bind("apiKey") String apiKey);

  @SqlUpdate("delete from api_key where date_created < (now() - '7 days'::interval)")
  void deleteOldKeys();
}
