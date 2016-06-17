package games.strategy.engine.lobby.server.userDB;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.ResultSet;

import org.junit.Test;

import games.strategy.util.MD5Crypt;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Util;

public class DBUserControllerTest {

  @Test
  public void testCreate() throws Exception {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    final String password = MD5Crypt.crypt(name);
    final DBUserController controller = new DBUserController();
    controller.createUser(name, email, password, false);
    assertTrue(controller.doesUserExist(name));

    try (final Connection con = Database.getConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + name + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email, rs.getString("email"));
      assertEquals(password, rs.getString("password"));
    }
  }

  @Test
  public void testGet() throws Exception {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    final String password = MD5Crypt.crypt(name);
    final DBUserController controller = new DBUserController();
    controller.createUser(name, email, password, false);
    assertTrue(controller.doesUserExist(name));
    final DBUser user = controller.getUser(name);
    assertEquals(user.getName(), name);
    assertEquals(user.getEmail(), email);
    assertEquals(user.isAdmin(), false);
  }

  @Test
  public void testCreateDupe() throws Exception {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    final String password = MD5Crypt.crypt(name);
    final DBUserController controller = new DBUserController();
    controller.createUser(name, email, password, false);
    try {
      controller.createUser(name, email, password, false);
      fail("Allowed to create dupe");
    } catch (final Exception e) {
      // expected
    }
  }

  @Test
  public void testLogin() throws Exception {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    final String password = MD5Crypt.crypt(name);
    final DBUserController controller = new DBUserController();
    controller.createUser(name, email, password, false);
    // advance the clock so we can see the login time
    long loginTimeMustBeAfter = System.currentTimeMillis();
    while (loginTimeMustBeAfter == System.currentTimeMillis()) {
      ThreadUtil.sleep(1);
    }
    loginTimeMustBeAfter = System.currentTimeMillis();
    assertTrue(controller.login(name, password));

    try (final Connection con = Database.getConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + name + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertTrue(rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
    }
    // make sure last login time was updated
  }

  @Test
  public void testUpdate() throws Exception {
    final String name = Util.createUniqueTimeStamp();
    final String email = name + "@none.none";
    final String password = MD5Crypt.crypt(name);
    final DBUserController controller = new DBUserController();
    controller.createUser(name, email, password, false);
    assertTrue(controller.doesUserExist(name));
    final String password2 = MD5Crypt.crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(name, email2, password2, false);
    try (final Connection con = Database.getConnection()) {
      final String sql = " select * from TA_USERS where userName = '" + name + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
    }
  }
}
