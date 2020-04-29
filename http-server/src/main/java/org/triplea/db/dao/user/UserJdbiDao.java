package org.triplea.db.dao.user;

import java.util.Optional;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.db.dao.user.role.UserRoleLookup;

/** Data access object for the users table. */
public interface UserJdbiDao {
  @SqlQuery("select id from lobby_user where username = :username")
  Optional<Integer> lookupUserIdByName(@Bind("username") String username);

  @SqlQuery("select bcrypt_password from lobby_user where username = :username")
  Optional<String> getPassword(@Bind("username") String username);

  @SqlUpdate("update lobby_user set last_login = now() where username = :username")
  int updateLastLoginTime(@Bind("username") String username);

  @SqlUpdate(
      "update lobby_user set password = null, bcrypt_password = :newPassword where id = :userId")
  int updatePassword(@Bind("userId") int userId, @Bind("newPassword") String newPassword);

  @SqlQuery("select email from lobby_user where id = :userId")
  String fetchEmail(@Bind("userId") int userId);

  @SqlUpdate("update lobby_user set email = :newEmail where id = :userId")
  int updateEmail(@Bind("userId") int userId, @Bind("newEmail") String newEmail);

  @SqlQuery(
      "select "
          + "   id as "
          + UserRoleLookup.USER_ID_COLUMN
          + ",   user_role_id as "
          + UserRoleLookup.USER_ROLE_ID_COLUMN
          + " from lobby_user "
          + " where username = :username")
  Optional<UserRoleLookup> lookupUserIdAndRoleIdByUserName(@Bind("username") String username);

  @SqlQuery(
      "select ur.name from user_role ur "
          + "join lobby_user lu on lu.user_role_id = ur.id "
          + "where lu.username = :username")
  Optional<String> lookupUserRoleByUserName(@Bind("username") String username);

  @SqlUpdate(
      "insert into lobby_user(username, email, bcrypt_password, user_role_id) "
          + "select "
          + ":username, :email, :password, (select id from user_role where name = '"
          + UserRole.PLAYER
          + "') as role_id")
  int createUser(
      @Bind("username") String username,
      @Bind("email") String email,
      @Bind("password") String cryptedPassword);

  @SqlQuery("select password from lobby_user where username = :username")
  Optional<String> getLegacyPassword(@Bind("username") String username);
}
