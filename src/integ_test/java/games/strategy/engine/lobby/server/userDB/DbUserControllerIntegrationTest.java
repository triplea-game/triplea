package games.strategy.engine.lobby.server.userDB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.function.Supplier;

import org.junit.Test;

import games.strategy.util.MD5Crypt;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Util;

public class DbUserControllerIntegrationTest {


  @Test
  public void testCreate() throws Exception {
    DbUser user = createUser();
    int startCount = getUserCount(() -> Database.getDerbyConnection());

    final DbUserController controller = new DbUserController();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.name)));

    int endCount = getUserCount(() -> Database.getDerbyConnection());
    assertEquals(endCount, startCount + 1);
    assertTrue(controller.doesUserExist(user.name));
  }

  private static DbUser createUser() {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    return new DbUser(
        new DbUser.UserName(name),
        new DbUser.UserEmail(email));
  }

  private static int getUserCount(final Supplier<Connection> connection) throws Exception {
    try (final Connection dbConnection = connection.get()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      int count = rs.getInt(1);
      return count;
    }
  }

  @Test
  public void testGet() throws Exception {
    final DbUser user = createUser();
    final DbUserController controller = new DbUserController();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.name)));

    final DbUser loadedUser = controller.getUserByName(user.name);

    assertEquals(loadedUser.name, user.name);
    assertEquals(user.email, user.email);
    assertEquals(user.admin, user.admin);
  }

  @Test
  public void doesUserExist() {
    final DbUser user = createUser();
    final DbUserController controller = new DbUserController();

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.name)));

    assertTrue(controller.doesUserExist(user.name));
  }

  @Test
  public void testCreateDupe() throws Exception {
    DbUser user = createUser();

    final DbUserController controller = new DbUserController();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.name)));

    try {
      controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.name)));
      fail("Should not be allowed to create a dupe user");
    } catch (final Exception expected) {
      // expected
    }
  }

  @Test
  public void testLogin() throws Exception {
    DbUser user = createUser();
    final DbUserController controller = new DbUserController();
    HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.name));
    controller.createUser(user, password);

    // advance the clock so we can see the login time
    long loginTimeMustBeAfter = System.currentTimeMillis();
    while (loginTimeMustBeAfter == System.currentTimeMillis()) {
      ThreadUtil.sleep(1);
    }
    loginTimeMustBeAfter = System.currentTimeMillis();
    assertTrue(controller.login(user.name, password));

    try (final Connection con = Database.getDerbyConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + user.name + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertTrue(rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
    }
    // make sure last login time was updated
  }


  @Test
  public void testUpdate() throws Exception {
    DbUser user = createUser();
    HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.name));
    final DbUserController controller = new DbUserController();
    controller.createUser(user, password);
    assertTrue(controller.doesUserExist(user.name));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DbUser(new DbUser.UserName(user.name), new DbUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (final Connection con = Database.getDerbyConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + user.name + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
    }
  }
}
