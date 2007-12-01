package games.strategy.engine.lobby.server.userDB;

import java.sql.*;

import junit.framework.TestCase;

import games.strategy.util.*;

public class DBUserControllerTest extends TestCase
{
    public void testCreate() throws Exception
    {
        String name = Util.createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
        assertTrue(controller.doesUserExist(name));
        
        Connection con = Database.getConnection();
        try
        {
            
            String sql = " select * from TA_USERS where userName = '" + name + "'";
            ResultSet rs = con.createStatement().executeQuery(sql);
            assertTrue(rs.next());
            
            assertEquals(email, rs.getString("email"));
            assertEquals(password, rs.getString("password"));
            
        }
        finally
        {
            con.close();    
        }
    }
    
    public void testGet() throws Exception
    {
        String name = Util.createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
        assertTrue(controller.doesUserExist(name));
        
        DBUser user = controller.getUser(name);
        assertEquals(user.getName(), name);
        assertEquals(user.getEmail(), email);
        assertEquals(user.isAdmin(), false);
    }
    
    
    public void testCreateDupe() throws Exception
    {
        String name = Util.createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
        try
        {
            controller.createUser(name, email, password, false);
            fail("Allowed to create dupe");
        }
        catch(Exception e)
        {
            //expected
        }
    }
    
    public void testLogin() throws Exception
    {
        String name = Util.createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
        
        //advance the clock so we can see the login time
        long loginTimeMustBeAfter = System.currentTimeMillis();
        while(loginTimeMustBeAfter == System.currentTimeMillis())
        {
            try
            {
                Thread.sleep(1);
            } catch (InterruptedException e)
            {}
        }
        loginTimeMustBeAfter = System.currentTimeMillis();
        
        assertTrue(controller.login(name, password));
        
        Connection con = Database.getConnection();
        try
        {
            
            String sql = " select * from TA_USERS where userName = '" + name + "'";
            ResultSet rs = con.createStatement().executeQuery(sql);
            assertTrue(rs.next());
            
            assertTrue(rs.getTimestamp("lastLogin").getTime() >= loginTimeMustBeAfter);
        }
        finally
        {
            con.close();    
        }
        
        //make sure last login time was updated
    }

    
    
    public void testUpdate() throws Exception
    {
        String name = Util.createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
        assertTrue(controller.doesUserExist(name));
        
        
        String password2 = MD5Crypt.crypt("foo");
        String email2 = "foo@foo.foo";
        
        controller.updateUser(name, email2, password2, false);
        
        
        Connection con = Database.getConnection();
        try
        {
            
            String sql = " select * from TA_USERS where userName = '" + name + "'";
            ResultSet rs = con.createStatement().executeQuery(sql);
            assertTrue(rs.next());
            
            assertEquals(email2, rs.getString("email"));
            assertEquals(password2, rs.getString("password"));
            
        }
        finally
        {
            con.close();    
        }
    }
}
