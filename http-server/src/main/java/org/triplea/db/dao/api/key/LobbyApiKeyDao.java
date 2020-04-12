package org.triplea.db.dao.api.key;

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
          + ApiKeyLookupRecord.USER_ID_COLUMN
          + ", ak.username as "
          + ApiKeyLookupRecord.USERNAME_COLUMN
          + ", ur.name as "
          + ApiKeyLookupRecord.ROLE_COLUMN
          + ", ak.player_chat_id  as "
          + ApiKeyLookupRecord.PLAYER_CHAT_ID_COLUMN
          + " from lobby_api_key ak "
          + " join user_role ur on ur.id = ak.user_role_id "
          + " left join lobby_user lu on lu.id = ak.lobby_user_id "
          + " where ak.key = :apiKey")
  Optional<ApiKeyLookupRecord> lookupByApiKey(@Bind("apiKey") String apiKey);

  @SqlUpdate("delete from lobby_api_key where date_created < (now() - '7 days'::interval)")
  void deleteOldKeys();

  @SqlQuery(
      "select "
          + GamePlayerLookup.PLAYER_NAME_COLUMN
          + ", "
          + GamePlayerLookup.SYSTEM_ID_COLUMN
          + ", "
          + GamePlayerLookup.IP_COLUMN
          + " "
          + "from lobby_api_key "
          + "where player_chat_id = :playerChatId")
  Optional<GamePlayerLookup> lookupByPlayerChatId(@Bind("playerChatId") String playerChatId);
}
