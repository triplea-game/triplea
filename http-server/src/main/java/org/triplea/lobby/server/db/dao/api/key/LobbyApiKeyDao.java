package org.triplea.lobby.server.db.dao.api.key;

import java.util.Optional;
import javax.annotation.Nullable;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/**
 * Dao for interacting with api_key table. Api_key table stores keys that are generated on login.
 * For non-anonymous accounts, the key is linked back to the players account which is used to
 * determine the users 'Role'. Anonymous users are still granted API keys, they have no user id and
 * given role 'ANONYMOUS'.
 */
interface LobbyApiKeyDao {
  @SqlUpdate(
      "insert into lobby_api_key("
          + "   username, lobby_user_id, user_role_id, player_chat_id, key, system_id, ip) "
          + "values "
          + "(:username, :userId, :userRoleId, :playerChatId, :apiKey, :systemId, :ip::inet)")
  int storeKey(
      @Bind("username") String username,
      @Nullable @Bind("userId") Integer userId,
      @Bind("userRoleId") int userRoleId,
      @Bind("playerChatId") String playerChatId,
      @Bind("apiKey") String key,
      @Bind("systemId") String systemId,
      @Bind("ip") String ipAddress);

  @SqlQuery(
      "select lu.id as "
          + UserWithRoleRecord.USER_ID_COLUMN
          + ", ak.username as "
          + UserWithRoleRecord.USERNAME_COLUMN
          + ", ur.name as "
          + UserWithRoleRecord.ROLE_COLUMN
          + ", ak.player_chat_id  as "
          + UserWithRoleRecord.PLAYER_CHAT_ID_COLUMN
          + " from lobby_api_key ak "
          + " join user_role ur on ur.id = ak.user_role_id "
          + " left join lobby_user lu on lu.id = ak.lobby_user_id "
          + " where ak.key = :apiKey")
  Optional<UserWithRoleRecord> lookupByApiKey(@Bind("apiKey") String apiKey);

  @SqlUpdate("delete from lobby_api_key where date_created < (now() - '7 days'::interval)")
  void deleteOldKeys();

  @SqlQuery(
      "select "
          + PlayerIdLookup.PLAYER_NAME_COLUMN
          + ", "
          + PlayerIdLookup.SYSTEM_ID_COLUMN
          + ", "
          + PlayerIdLookup.IP_COLUMN
          + " "
          + "from lobby_api_key "
          + "where player_chat_id = :playerChatId")
  Optional<PlayerIdLookup> lookupByPlayerChatId(@Bind("playerChatId") String playerChatId);
}
