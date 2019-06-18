package org.triplea.lobby.server.db.dao;

import static org.triplea.lobby.server.db.data.ApiKeyDaoData.DATE_LAST_USED_COLUMN;
import static org.triplea.lobby.server.db.data.ApiKeyDaoData.LAST_USED_HOST_ADDRESS_COLUMN;
import static org.triplea.lobby.server.db.data.ApiKeyDaoData.PUBLIC_ID_COLUMN;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.lobby.server.db.data.ApiKeyDaoData;

/**
 * DAO used to lookup single-use keys, insert single-use keys, and insert (permanent) moderator api-keys.
 */
public interface ModeratorApiKeyDao {

  @SqlQuery("select "
      + PUBLIC_ID_COLUMN + ", "
      + DATE_LAST_USED_COLUMN + ", "
      + LAST_USED_HOST_ADDRESS_COLUMN + "\n"
      + "from moderator_api_key\n"
      + "where lobby_user_id = :userId\n"
      + "order by " + PUBLIC_ID_COLUMN)
  List<ApiKeyDaoData> getKeysByUserId(@Bind("userId") int userId);


  @SqlUpdate("insert into moderator_api_key\n"
      + "(public_id, lobby_user_id, last_used_host_address, api_key) values\n"
      + "(:publicId, :lobbyUserId, :machineIp, :apiKey)")
  int insertNewApiKey(
      @Bind("publicId") String publicId,
      @Bind("lobbyUserId") int userId,
      @Bind("machineIp") String machineIp,
      @Bind("apiKey") String hashedApiKey);

  @SqlUpdate("delete from moderator_api_key where public_id = :publicKeyId")
  int deleteKey(@Bind("publicKeyId") String keyId);


  @SqlQuery("select lobby_user_id from moderator_api_key where api_key = :api_key")
  Optional<Integer> lookupModeratorIdByApiKey(@Bind("api_key") String cryptedValue);

  /**
   * Returns the moderator name if the given key matches the API key of a super-mod.
   */
  @SqlQuery("select u.id from lobby_user u\n"
      + "join moderator_api_key m on m.lobby_user_id = u.id\n"
      + "where \n"
      + "  m.api_key = :apiKey\n"
      + "  and u.super_mod = true")
  Optional<Integer> lookupSuperModeratorIdByApiKey(@Bind("apiKey") String apiKey);

  @SqlUpdate("update moderator_api_key\n"
      + "set date_last_used = now(), last_used_host_address = :usedByHostAddress\n"
      + "where api_key = :apiKey")
  int recordKeyUsage(@Bind("apiKey") String cryptedValue, @Bind("usedByHostAddress") String usedByHostAddress);

  @SqlUpdate("delete from moderator_api_key where lobby_user_id = :userId")
  int deleteKeysByUserId(@Bind("userId") int userId);
}
