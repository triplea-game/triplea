package games.strategy.engine.lobby.server;

import java.util.logging.Logger;

import games.strategy.engine.lobby.server.userDB.*;
import games.strategy.engine.message.*;
import games.strategy.net.INode;

public class UserManager implements IUserManager
{
    private final static Logger s_logger = Logger.getLogger(UserManager.class.getName());

    public void register(IRemoteMessenger messenger)
    {
        messenger.registerRemote(IUserManager.class, this, IUserManager.USER_MANAGER);
    }

    /**
     * Update hte user info, returning an error string if an error occurs
     */
    public String updateUser(String userName, String emailAddress, String hashedPassword)
    {
        INode remote = MessageContext.getSender();
        if(!userName.equals(remote.getName()))
        {
            s_logger.severe("Tried to update user permission, but not correct user, userName:" + userName + " node:" + remote);
            return "Sorry, but I can't let you do that";
        }
        
        try
        {
            new DBUserController().updateUser(userName, emailAddress, hashedPassword, false);
        }
        catch(IllegalStateException e)
        {
            return e.getMessage();
        }
        return null;
        
    }
    
    
    /**
     * Update hte user info, returning an error string if an error occurs
     */
    public DBUser getUserInfo(String userName)
    {
        INode remote = MessageContext.getSender();
        if(!userName.equals(remote.getName()))
        {
            s_logger.severe("Tried to get user info, but not correct user, userName:" + userName + " node:" + remote);
            throw new IllegalStateException("Sorry, but I can't let you do that");
        }

        return new DBUserController().getUser(userName);
    }
    
    
    
    
}
