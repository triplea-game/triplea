package games.strategy.engine.lobby.server.db;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import games.strategy.engine.lobby.server.login.RsaAuthenticator;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class UserControllerIntegrationTest {

  private static final Supplier<Connection> connectionSupplier = Database::getPostgresConnection;
  private final UserController controller = new UserController(connectionSupplier);

  @Test
  public void testCreate() throws Exception {
    final int startCount = getUserCount();

    createUserWithMd5CryptHash();
    assertEquals(getUserCount(), startCount + 1);

    createUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 2);
  }

  @Test
  public void testGet() throws Exception {
    final DBUser user = createUserWithMd5CryptHash();

    final DBUser loadedUser = controller.getUserByName(user.getName());

    assertEquals(loadedUser.getName(), user.getName());
    assertEquals(loadedUser.getEmail(), user.getEmail());
    assertEquals(loadedUser.isAdmin(), user.isAdmin());
  }

  @Test
  public void testDoesUserExist() {
    assertTrue(controller.doesUserExist(createUserWithMd5CryptHash().getName()));
    assertTrue(controller.doesUserExist(createUserWithBCryptHash().getName()));
  }

  @Test
  public void testCreateDupe() throws Exception {
    catchException(() -> controller.createUser(createUserWithMd5CryptHash(),
        new HashedPassword(MD5Crypt.crypt(Util.createUniqueTimeStamp()))));
    assertThat("Should not be allowed to create a dupe user", caughtException(), is(not(equalTo(null))));
  }

  @Test
  public void testLogin() throws Exception {
    final String password = MD5Crypt.crypt(Util.createUniqueTimeStamp());
    final DBUser user = createUserWithHash(password, s -> s);
    controller.updateUser(user,
        new HashedPassword(BCrypt.hashpw(RsaAuthenticator.hashPasswordWithSalt(password), BCrypt.gensalt())));
    assertTrue(controller.login(user.getName(), new HashedPassword(password)));
    assertTrue(controller.login(user.getName(), new HashedPassword(RsaAuthenticator.hashPasswordWithSalt(password))));
  }

  @Test
  public void testUpdate() throws Exception {
    final DBUser user = createUserWithMd5CryptHash();
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(bcrypt(obfuscate(Util.createUniqueTimeStamp()))));
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (final Connection con = connectionSupplier.get()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
      assertNull(rs.getString("bcrypt_password"));
    }
  }

  private DBUser createUserWithMd5CryptHash() {
    return createUserWithHash(Util.createUniqueTimeStamp(), MD5Crypt::crypt);
  }

  private DBUser createUserWithBCryptHash() {
    return createUserWithHash(Util.createUniqueTimeStamp(), s -> BCrypt.hashpw(s, BCrypt.gensalt()));
  }

  private DBUser createUserWithHash(final @Nullable String password, final Function<String, String> hashingMethod) {
    final String name = UUID.randomUUID().toString().substring(0, 20);
    final DBUser user = new DBUser(
        new DBUser.UserName(name),
        new DBUser.UserEmail(name + "@none.none"));
    controller.createUser(user, new HashedPassword(hashingMethod.apply(password)));
    return user;
  }

  private static int getUserCount() throws Exception {
    try (final Connection dbConnection = connectionSupplier.get()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }

  private static String bcrypt(final String string) {
    return BCrypt.hashpw(string, BCrypt.gensalt());
  }

  private static String obfuscate(final String string) {
    return RsaAuthenticator.hashPasswordWithSalt(string);
  }
}
