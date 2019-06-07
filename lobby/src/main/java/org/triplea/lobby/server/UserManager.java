package org.triplea.lobby.server;

import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.IUserManager;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.HashedPassword;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.INode;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

@Log
@AllArgsConstructor
final class UserManager implements IUserManager {
  private final DatabaseDao database;

  void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, REMOTE_NAME);
  }

  @Override
  public String updateUser(final String userName, final String emailAddress, final String hashedPassword) {
    final INode remote = MessageContext.getSender();
    if (!userName.equals(remote.getName())) {
      log.severe("Tried to update user permission, but not correct user, userName:" + userName + " node:" + remote);
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
      database.getUserDao().updateUser(
          user.getName(),
          user.getEmail(),
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
      log.severe("Tried to get user info, but not correct user, userName:" + userName + " node:" + remote);
      throw new IllegalStateException("Sorry, but I can't let you do that");
    }
    return database.getUserDao().getUserByName(userName);
  }
}
