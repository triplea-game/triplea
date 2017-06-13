package games.strategy.engine.lobby.server;

import java.util.logging.Logger;

import games.strategy.engine.lobby.server.userDB.DbUser;
import games.strategy.engine.lobby.server.userDB.DbUserController;
import games.strategy.engine.lobby.server.userDB.HashedPassword;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.INode;

public class UserManager implements IUserManager {
  private static final Logger s_logger = Logger.getLogger(UserManager.class.getName());

  public void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, IUserManager.USER_MANAGER);
  }

  /**
   * Update the user info, returning an error string if an error occurs.
   */
  @Override
  public String updateUser(final String userName, final String emailAddress, final String hashedPassword) {
    final INode remote = MessageContext.getSender();
    if (!userName.equals(remote.getName())) {
      s_logger
          .severe("Tried to update user permission, but not correct user, userName:" + userName + " node:" + remote);
      return "Sorry, but I can't let you do that";
    }

    DbUser user = new DbUser(
        new DbUser.UserName(userName),
        new DbUser.UserEmail(emailAddress));
    if (!user.isValid()) {
      return user.getValidationErrorMessage();
    }
    HashedPassword password = new HashedPassword(hashedPassword);
    if (!password.isValidSyntax()) {
      return "Password is not hashed correctly";
    }

    try {
      new DbUserController().updateUser(user, password);
    } catch (final IllegalStateException e) {
      return e.getMessage();
    }
    return null;
  }

  /**
   * Update the user info, returning an error string if an error occurs.
   */
  @Override
  public DbUser getUserInfo(final String userName) {
    final INode remote = MessageContext.getSender();
    if (!userName.equals(remote.getName())) {
      s_logger.severe("Tried to get user info, but not correct user, userName:" + userName + " node:" + remote);
      throw new IllegalStateException("Sorry, but I can't let you do that");
    }
    return new DbUserController().getUserByName(userName);
  }
}
