package org.triplea.lobby.server.db;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import com.google.common.base.Preconditions;

/**
 * DAO used to lookup single-use keys, insert single-use keys, and insert (permanent) moderator api-keys.
 */
public interface ApiKeyRegistrationDao {

  String SINGLE_USE_KEY_EXPIRY = "'3 days'";

  @SqlQuery("select lobby_user_id\n"
      + "from moderator_single_use_key\n"
      + "where date_used is null\n"
      + "  and date_created > (now() - interval " + SINGLE_USE_KEY_EXPIRY + ")\n"
      + "  and api_key = :api_key")
  Optional<Integer> lookupModeratorBySingleUseKey(@Bind("api_key") String apiKey);


  @SqlUpdate("update moderator_single_use_key\n"
      + "set date_used = now()\n"
      + "where date_used is null"
      + "  and date_created > (now() - interval " + SINGLE_USE_KEY_EXPIRY + ")\n"
      + "  and api_key = :api_key")
  int invalidateSingleUseKey(@Bind("api_key") String apiKey);


  @SqlUpdate("insert into moderator_single_use_key\n"
      + "(lobby_user_id, api_key) values\n"
      + "(:lobby_user_id, :api_key)")
  int insertNewSingleUseKey(
      @Bind("lobby_user_id") long userId, @Bind("api_key") String hashedApiKey);

  @SqlUpdate("insert into moderator_api_key\n"
      + "(lobby_user_id, api_key) values\n"
      + "(:lobby_user_id, :api_key)")
  int insertNewApiKey(
      @Bind("lobby_user_id") long userId, @Bind("api_key") String hashedApiKey);


  @Transaction
  default void invalidateOldKeyAndInsertNew(
      final int userId, final String oldKey, final String newKey) {
    Preconditions.checkState(userId > 0);
    Preconditions.checkState(oldKey.length() == 128);
    Preconditions.checkState(newKey.length() == 128);
    Preconditions.checkState(!oldKey.equalsIgnoreCase(newKey));

    Preconditions.checkState(invalidateSingleUseKey(oldKey) == 1);
    Preconditions.checkState(insertNewApiKey(userId, newKey) == 1);
  }
}
