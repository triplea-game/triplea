package games.strategy.engine.lobby.server.userDB;

import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.sql.Connection;
import java.sql.ResultSet;

import junit.framework.TestCase;

public class DBUserControllerTest extends TestCase
{
	public void testCreate() throws Exception
	{
		final String name = Util.createUniqueTimeStamp();
		final String email = name + "@none.none";
		final String password = MD5Crypt.crypt(name);
		final DBUserController controller = new DBUserController();
		controller.createUser(name, email, password, false);
		assertTrue(controller.doesUserExist(name));
		final Connection con = Database.getConnection();
		try
		{
			final String sql = " select * from TA_USERS where userName = '" + name + "'";
			final ResultSet rs = con.createStatement().executeQuery(sql);
			assertTrue(rs.next());
			assertEquals(email, rs.getString("email"));
			assertEquals(password, rs.getString("password"));
		} finally
		{
			con.close();
		}
	}
	
	public void testGet() throws Exception
	{
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
	
	public void testCreateDupe() throws Exception
	{
		final String name = Util.createUniqueTimeStamp();
		final String email = name + "@none.none";
		final String password = MD5Crypt.crypt(name);
		final DBUserController controller = new DBUserController();
		controller.createUser(name, email, password, false);
		try
		{
			controller.createUser(name, email, password, false);
			fail("Allowed to create dupe");
		} catch (final Exception e)
		{
			// expected
		}
	}
	
	public void testLogin() throws Exception
	{
		final String name = Util.createUniqueTimeStamp();
		final String email = name + "@none.none";
		final String password = MD5Crypt.crypt(name);
		final DBUserController controller = new DBUserController();
		controller.createUser(name, email, password, false);
		// advance the clock so we can see the login time
		long loginTimeMustBeAfter = System.currentTimeMillis();
		while (loginTimeMustBeAfter == System.currentTimeMillis())
		{
			try
			{
				Thread.sleep(1);
			} catch (final InterruptedException e)
			{
			}
		}
		loginTimeMustBeAfter = System.currentTimeMillis();
		assertTrue(controller.login(name, password));
		final Connection con = Database.getConnection();
		try
		{
			final String sql = " select * from TA_USERS where userName = '" + name + "'";
			final ResultSet rs = con.createStatement().executeQuery(sql);
			assertTrue(rs.next());
			assertTrue(rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
		} finally
		{
			con.close();
		}
		// make sure last login time was updated
	}
	
	public void testUpdate() throws Exception
	{
		final String name = Util.createUniqueTimeStamp();
		final String email = name + "@none.none";
		final String password = MD5Crypt.crypt(name);
		final DBUserController controller = new DBUserController();
		controller.createUser(name, email, password, false);
		assertTrue(controller.doesUserExist(name));
		final String password2 = MD5Crypt.crypt("foo");
		final String email2 = "foo@foo.foo";
		controller.updateUser(name, email2, password2, false);
		final Connection con = Database.getConnection();
		try
		{
			final String sql = " select * from TA_USERS where userName = '" + name + "'";
			final ResultSet rs = con.createStatement().executeQuery(sql);
			assertTrue(rs.next());
			assertEquals(email2, rs.getString("email"));
			assertEquals(password2, rs.getString("password"));
		} finally
		{
			con.close();
		}
	}
}
