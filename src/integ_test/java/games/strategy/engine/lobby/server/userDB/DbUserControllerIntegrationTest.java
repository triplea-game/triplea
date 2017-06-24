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

  private static final DbTestConnection DERBY =
      new DbTestConnection(Database::getDerbyConnection, new DbUserController());

  private static final DbTestConnection POSTGRES =
      new DbTestConnection(Database::getPostgresConnection, new DbUserController(
          new UserController(
              UserDaoPrimarySecondary.Role.PRIMARY,
              Database::getPostgresConnection)));

  @Test
  public void testCreate() throws Exception {
    testCreate(DERBY);
  }

  private static void testCreate(final DbTestConnection dbTestConnection) throws Exception {
    final DBUser user = givenUser();
    final int startCount = getUserCount(dbTestConnection.connectionSupplier);

    dbTestConnection.controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    final int endCount = getUserCount(dbTestConnection.connectionSupplier);
    assertEquals(endCount, startCount + 1);
  }

  private static DBUser givenUser() {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    return new DBUser(
        new DBUser.UserName(name),
        new DBUser.UserEmail(email));
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
  public void testCreatePostgres() throws Exception {
    testCreate(POSTGRES);
  }

  @Test
  public void testGet() throws Exception {
    testGet(DERBY);
  }

  private static void testGet(final DbTestConnection dbTestConnection) throws Exception {
    final DBUser user = givenUser();
    dbTestConnection.controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    final DBUser loadedUser = dbTestConnection.controller.getUserByName(user.getName());

    assertEquals(loadedUser.getName(), user.getName());
    assertEquals(loadedUser.getEmail(), user.getEmail());
    assertEquals(loadedUser.isAdmin(), user.isAdmin());
  }

  @Test
  public void testGetPostgres() throws Exception {
    testGet(POSTGRES);
  }

  @Test
  public void testDoesUserExist() {
    testDoesUserExist(DERBY);
  }

  private static void testDoesUserExist(final DbTestConnection dbTestConnection) {
    final DBUser user = givenUser();

    dbTestConnection.controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    assertTrue(dbTestConnection.controller.doesUserExist(user.getName()));
  }

  @Test
  public void testDoesUserExistPostgres() {
    testDoesUserExist(POSTGRES);
  }

  @Test
  public void testCreateDupe() throws Exception {
    testCreateDupe(DERBY);
  }

  private static void testCreateDupe(final DbTestConnection dbTestConnection) throws Exception {
    final DBUser user = givenUser();

    dbTestConnection.controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));

    try {
      dbTestConnection.controller.createUser(user, new HashedPassword(MD5Crypt.crypt(user.getName())));
      fail("Should not be allowed to create a dupe user");
    } catch (final Exception expected) {
      // expected
    }
  }

  @Test
  public void testCreateDupePostgres() throws Exception {
    testCreateDupe(POSTGRES);
  }

  @Test
  public void testLogin() throws Exception {
    testLogin(DERBY);
  }

  private static void testLogin(final DbTestConnection dbTestConnection) throws Exception {
    final DBUser user = givenUser();
    final HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    dbTestConnection.controller.createUser(user, password);

    ThreadUtil.sleep(1);
    final long loginTimeMustBeAfter = System.currentTimeMillis();
    ThreadUtil.sleep(1);

    assertTrue(dbTestConnection.controller.login(user.getName(), password));

    try (final Connection con = dbTestConnection.connectionSupplier.get()) {
      final String sql = " select * from TA_USERS where userName = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertTrue(
          String.format("lastLogin %s, should be after %s",
              rs.getTimestamp("lastLogin").getTime(), loginTimeMustBeAfter),

          rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
    }
    // make sure last login time was updated
  }

  @Test
  public void testLoginPostgres() throws Exception {
    testLogin(POSTGRES);
  }

  @Test
  public void testUpdate() throws Exception {
    testUpdate(DERBY);
  }

  private static void testUpdate(final DbTestConnection dbTestConnection) throws Exception {
    final DBUser user = givenUser();
    final HashedPassword password = new HashedPassword(MD5Crypt.crypt(user.getName()));
    dbTestConnection.controller.createUser(user, password);
    assertTrue(dbTestConnection.controller.doesUserExist(user.getName()));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    dbTestConnection.controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (final Connection con = dbTestConnection.connectionSupplier.get()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
    }
  }

  @Test
  public void testUpdatePostgres() throws Exception {
    testUpdate(POSTGRES);
  }

  private static class DbTestConnection {
    final Supplier<Connection> connectionSupplier;
    final DbUserController controller;

    DbTestConnection(final Supplier<Connection> connectionSupplier,
        final DbUserController controller) {
      this.connectionSupplier = connectionSupplier;
      this.controller = controller;
    }
  }
}
