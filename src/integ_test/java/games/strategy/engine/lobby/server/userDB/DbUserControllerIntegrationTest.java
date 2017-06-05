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
  public void create() throws Exception {
    testCreate(Database::getDerbyConnection, new DbUserController());
    testCreate(Database::getPostgresConnection, new DbUserController(new DerbyUserController(
        UserDaoPrimarySecondary.Role.PRIMARY, Database::getPostgresConnection)));
  }

  private void testCreate(Supplier<Connection> connectionSupplier, DbUserController controller) throws Exception {
    DbUser user = createUser();
    int startCount = getUserCount(connectionSupplier);

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    int endCount = getUserCount(connectionSupplier);
    assertEquals(endCount, startCount + 1);
    assertTrue(controller.doesUserExist(user.getName()));
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
      return rs.getInt(1);
    }
  }

  @Test
  public void testGet() throws Exception {
    final DbUser user = createUser();
    final DbUserController controller = new DbUserController();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    final DbUser loadedUser = controller.getUserByName(user.getName());

    assertEquals(loadedUser.getName(), user.getName());
    assertEquals(loadedUser.getEmail(), user.getEmail());
    assertEquals(loadedUser.isAdmin(), user.isAdmin());
  }

  @Test
  public void doesUserExist() {
    final DbUser user = createUser();
    final DbUserController controller = new DbUserController();

    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    assertTrue(controller.doesUserExist(user.getName()));
  }

  @Test
  public void testCreateDupe() throws Exception {
    DbUser user = createUser();

    final DbUserController controller = new DbUserController();
    controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    try {
      controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));
      fail("Should not be allowed to create a dupe user");
    } catch (final Exception expected) {
      // expected
    }
  }

  @Test
  public void testLogin() throws Exception {
    DbUser user = createUser();
    final DbUserController controller = new DbUserController();
    HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    controller.createUser(user, password);

    // advance the clock so we can see the login time
    long loginTimeMustBeAfter = System.currentTimeMillis();
    while (loginTimeMustBeAfter == System.currentTimeMillis()) {
      ThreadUtil.sleep(1);
    }
    loginTimeMustBeAfter = System.currentTimeMillis();
    assertTrue(controller.login(user.getName(), password));

    try (final Connection con = Database.getDerbyConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertTrue(rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
    }
    // make sure last login time was updated
  }


  @Test
  public void testUpdate() throws Exception {
    DbUser user = createUser();
    HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    final DbUserController controller = new DbUserController();
    controller.createUser(user, password);
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DbUser(new DbUser.UserName(user.getName()), new DbUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (final Connection con = Database.getDerbyConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
    }
  }
}
