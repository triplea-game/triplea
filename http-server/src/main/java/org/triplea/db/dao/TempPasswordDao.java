package org.triplea.db.dao;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;

/**
 * DAO for CRUD operations on temp password table. A table that stores temporary passwords issued to
 * them with the 'forgot password' feature.
 */
public interface TempPasswordDao {
  String TEMP_PASSWORD_EXPIRATION = "1 day";

  @SqlQuery(
      "select temp_password"
          + " from temp_password_request t"
          + " join lobby_user lu on lu.id = t.lobby_user_id"
          + " where lu.username = :username"
          + "   and t.date_created >  (now() - '"
          + TEMP_PASSWORD_EXPIRATION
          + "'::interval)"
          + "   and t.date_invalidated is null")
  Optional<String> fetchTempPassword(@Bind("username") String username);

  @SqlQuery("select id from lobby_user where username = :username")
  Optional<Integer> lookupUserIdByUsername(@Bind("username") String username);

  @SqlUpdate(
      "insert into temp_password_request"
          + " (lobby_user_id, temp_password)"
          + " values (:userId, :password)")
  void insertPassword(@Bind("userId") int userId, @Bind("password") String password);

  @SqlQuery("select id from lobby_user where username = :username and email = :email")
  Optional<Integer> lookupUserIdByUsernameAndEmail(
      @Bind("username") String username, @Bind("email") String email);

  @SqlUpdate(
      "update temp_password_request"
          + " set date_invalidated = now()"
          + " where lobby_user_id = (select id from lobby_user where username = :playerName)"
          + "   and date_invalidated is null")
  int invalidateTempPasswords(@Bind("playerName") String playerName);

  @Transaction
  default boolean insertTempPassword(
      final String username, final String email, final String password) {
    return lookupUserIdByUsernameAndEmail(username, email)
        .map(
            userId -> {
              invalidateTempPasswords(username);
              insertPassword(userId, password);
              return true;
            })
        .orElse(false);
  }
}
