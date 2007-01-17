package games.strategy.engine.lobby.server.login;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.util.*;

import java.net.*;
import java.util.*;

import junit.framework.TestCase;

public class LobbyLoginValidatorTest extends TestCase
{

    
    public void testCreateNewUser()
    {
        LobbyLoginValidator validator = new LobbyLoginValidator();
        SocketAddress address = new  InetSocketAddress(5000);
        
        String name = Util.createUniqueTimeStamp();
        
        Map<String,String> properties = new HashMap<String,String>();
        properties.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
        
        properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("123", "foo"));
        properties.put(LobbyLoginValidator.EMAIL_KEY, "none@none.none");        
        properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        
        assertNull(
                new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));
        
        
        //try to create a duplicate user, should not work
        assertNotNull(new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));
    }
    
    public void testWrongVersion()
    {
        
        LobbyLoginValidator validator = new LobbyLoginValidator();
        SocketAddress address = new  InetSocketAddress(5000);
        
        String name = Util.createUniqueTimeStamp();
        
        Map<String,String> properties = new HashMap<String,String>();
        properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        
        properties.put(LobbyLoginValidator.LOBBY_VERSION, "0.1");
        
        assertNotNull(
                new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));

        
    
    
    }
    
    public void testAnonymousLogin()
    {
        LobbyLoginValidator validator = new LobbyLoginValidator();
        SocketAddress address = new  InetSocketAddress(5000);
        
        String name = Util.createUniqueTimeStamp();
        
        Map<String,String> properties = new HashMap<String,String>();
        properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        
        assertNull(
                new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));

        
        //create a user, verify we can't login with a username that already exists
        new DBUserController().createUser(name, "none@none.none", MD5Crypt.crypt("foo"), false);
        
        
        //we should not be able to login now
        assertNotNull(new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));

    }

    public void testAnonymousLoginBadName()
    {
        LobbyLoginValidator validator = new LobbyLoginValidator();
        SocketAddress address = new  InetSocketAddress(5000);
        
        String name = "bitCh" + Util.createUniqueTimeStamp();
        
        Map<String,String> properties = new HashMap<String,String>();
        properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        
        
        
 
        assertEquals(LobbyLoginValidator.THATS_NOT_A_NICE_NAME, new LobbyLoginValidator().verifyConnection(
                validator.getChallengeProperties(name, address),
                properties, name, address
                ));

    }
    
    public void testLogin()
    {
        LobbyLoginValidator validator = new LobbyLoginValidator();
        SocketAddress address = new  InetSocketAddress(5000);
        
        String name = Util.createUniqueTimeStamp();
        String email = "none@none.none";
        String password = "foo";
        String hashedPassword = MD5Crypt.crypt(password);
        
        new DBUserController().createUser(name, email, hashedPassword, false);
        
        
        Map<String,String> properties = new HashMap<String,String>();
        
        properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
        properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        
        Map<String, String> challengeProperties = validator.getChallengeProperties(name, address);
        
        assertEquals(challengeProperties.get(LobbyLoginValidator.SALT_KEY), MD5Crypt.getSalt(MD5Crypt.MAGIC, hashedPassword));
        
        assertNull(
                new LobbyLoginValidator().verifyConnection(
                challengeProperties,
                properties, name, address
                ));


        //with a bad password
        properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("wrong"));
        assertNotNull(
                new LobbyLoginValidator().verifyConnection(
                challengeProperties,
                properties, name, address
                ));

        
        //with a non existent user
        assertNotNull(
                new LobbyLoginValidator().verifyConnection(
                challengeProperties,
                properties, Util.createUniqueTimeStamp() , address
                ));
        

    }
    
    
    
}
