package games.strategy.engine.lobby.server;

import java.util.logging.Logger;

import org.mindrot.jbcrypt.BCrypt;

import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.INode;

public class UserManager implements IUserManager {
  private static final Logger logger = Logger.getLogger(UserManager.class.getName());

  public void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, IUserManager.USER_MANAGER);
  }

  @Override
  public String updateUser(final String userName, final String emailAddress, final String hashedPassword) {
    final INode remote = MessageContext.getSender();
    if (!userName.equals(remote.getName())) {
      logger.severe("Tried to update user permission, but not correct user, userName:" + userName + " node:" + remote);
      return "Sorry, but I can't let you do that";
    }

    final DBUser user = new DBUser(
        new DBUser.UserName(userName),
        new DBUser.UserEmail(emailAddress));
    if (!user.isValid()) {
      return user.getValidationErrorMessage();
    }
    final HashedPassword password = new HashedPassword(hashedPassword);

    try {
      new UserController().updateUser(user,
          password.isHashedWithSalt() ? password : new HashedPassword(BCrypt.hashpw(hashedPassword, BCrypt.gensalt())));
    } catch (final IllegalStateException e) {
      return e.getMessage();
    }
    return null;
  }

  @Override
  public DBUser getUserInfo(final String userName) {
    final INode remote = MessageContext.getSender();
    if (!userName.equals(remote.getName())) {
      logger.severe("Tried to get user info, but not correct user, userName:" + userName + " node:" + remote);
      throw new IllegalStateException("Sorry, but I can't let you do that");
    }
    return new UserController().getUserByName(userName);
  }
}
