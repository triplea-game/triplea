package games.strategy.engine.lobby.server.db;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.Test;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.util.MD5Crypt;

public class UserControllerIntegrationTest {

  private static final Supplier<Connection> connectionSupplier = Database::getPostgresConnection;
  private final UserController controller = new UserController(connectionSupplier);

  @Test
  public void testCreate() throws Exception {
    final DBUser user = newUser();
    final int startCount = getUserCount();

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    final int endCount = getUserCount();
    assertEquals(endCount, startCount + 1);
  }

  @Test
  public void testGet() throws Exception {
    final DBUser user = newUser();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    final DBUser loadedUser = controller.getUserByName(user.getName());

    assertEquals(loadedUser.getName(), user.getName());
    assertEquals(loadedUser.getEmail(), user.getEmail());
    assertEquals(loadedUser.isAdmin(), user.isAdmin());
  }

  @Test
  public void testDoesUserExist() {
    final DBUser user = newUser();

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    assertTrue(controller.doesUserExist(user.getName()));
  }

  @Test
  public void testCreateDupe() throws Exception {
    final DBUser user = newUser();

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));
    catchException(() -> controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName()))));
    assertThat("Should not be allowed to create a dupe user", caughtException(), is(not(equalTo(null))));
  }

  @Test
  public void testLogin() throws Exception {
    final DBUser user = newUser();
    final HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    controller.createUser(user, password);

    assertTrue(controller.login(user.getName(), password));
  }

  @Test
  public void testUpdate() throws Exception {
    final DBUser user = newUser();
    final HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    controller.createUser(user, password);
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (final Connection con = connectionSupplier.get()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
    }
  }

  private static DBUser newUser() {
    final String name = UUID.randomUUID().toString().substring(0, 20);
    final String email = name + "@none.none";
    return new DBUser(
        new DBUser.UserName(name),
        new DBUser.UserEmail(email));
  }

  private static int getUserCount() throws Exception {
    try (final Connection dbConnection = connectionSupplier.get()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }
}
