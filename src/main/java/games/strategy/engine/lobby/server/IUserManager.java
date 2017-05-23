package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.DbUser;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;

public interface IUserManager extends IRemote {
  RemoteName USER_MANAGER =
      new RemoteName("games.strategy.engine.lobby.server.USER_MANAGER", IUserManager.class);

  String updateUser(String userName, String emailAddress, String hashedPassword);

  DbUser getUserInfo(String userName);
}
