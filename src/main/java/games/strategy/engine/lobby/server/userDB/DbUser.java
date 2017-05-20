package games.strategy.engine.lobby.server.userDB;

import java.io.Serializable;
import java.util.Date;

public class DbUser implements Serializable {
  private static final long serialVersionUID = -5289923058375302916L;
  private final String name;
  private final String email;
  private final boolean isAdmin;
  private final Date lastLogin;
  private final Date joined;

  public DbUser(final String name, final String email, final boolean isAdmin, final Date lastLogin, final Date joined) {
    this.name = name;
    this.email = email;
    this.isAdmin = isAdmin;
    this.lastLogin = lastLogin;
    this.joined = joined;
  }

  public String getEmail() {
    return email;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public Date getJoined() {
    return joined;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public String getName() {
    return name;
  }
}
