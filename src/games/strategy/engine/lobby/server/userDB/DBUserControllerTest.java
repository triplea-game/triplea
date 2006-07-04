package games.strategy.engine.lobby.server.userDB;

import java.sql.*;

import junit.framework.TestCase;

import games.strategy.util.MD5Crypt;

public class DBUserControllerTest extends TestCase
{
    private String createUniqueTimeStamp()
    {
        long time = System.currentTimeMillis();
        while(time == System.currentTimeMillis())
        {
            try
            {
                Thread.sleep(1);
            } catch (InterruptedException e)
            {
                
            }
        }
        return "" +  System.currentTimeMillis();
    }

    
    public void testCreate() throws Exception
    {
        String name = createUniqueTimeStamp();
        String email = name +  "@none.none";
        String password = MD5Crypt.crypt(name);
        
        DBUserController controller = new DBUserController();
        controller.createUser(name, email, password, false);
        
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
    
    public void testCreateDupe() throws Exception
    {
        String name = createUniqueTimeStamp();
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
        String name = createUniqueTimeStamp();
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
    
}
