package org.triplea.lobby.server.db.dao;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

/** DAO for managing moderator users. */
public interface ModeratorSingleUseKeyDao {

  String SINGLE_USE_KEY_EXPIRY = "'3 days'";

  @SqlQuery(
      "select lobby_user_id\n"
          + "from moderator_single_use_key\n"
          + "where date_used is null\n"
          + "  and date_created > (now() - interval "
          + SINGLE_USE_KEY_EXPIRY
          + ")\n"
          + "  and api_key = :api_key")
  Optional<Integer> lookupModeratorBySingleUseKey(@Bind("api_key") String apiKey);

  @SqlUpdate(
      "insert into moderator_single_use_key(lobby_user_id, api_key) values (:userId, :apiKey)")
  int insertSingleUseKey(@Bind("userId") int userId, @Bind("apiKey") String hashedApiKey);

  @SqlUpdate(
      "update moderator_single_use_key\n"
          + "set date_used = now()\n"
          + "where api_key = :api_key"
          + "  and date_created > (now() - interval "
          + SINGLE_USE_KEY_EXPIRY
          + ")\n"
          + "  and date_used is null")
  int invalidateSingleUseKey(@Bind("api_key") String apiKey);

  @SqlUpdate("delete from moderator_single_use_key where lobby_user_id = :userId")
  int deleteKeysByUserId(@Bind("userId") int userId);
}
