package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.IRemote;

public interface IUserManager extends IRemote
{

    public String updateUser(String userName, String emailAddress, String hashedPassword);
    public DBUser getUserInfo(String userName);
    
}
