package games.strategy.engine.lobby.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;

import org.triplea.game.server.HeadlessGameServer;

import games.strategy.net.INode;
import games.strategy.net.Node;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

// TODO: move this class to lobby.common upon next lobby-incompatible release; it is shared between client and server

/**
 * NOTE - this class is not thread safe. Modifications should be done holding an external lock.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
// if you add a field, add it to write/read object as well for Externalizable
@NoArgsConstructor
@ToString
public class GameDescription implements Externalizable, Cloneable {
  private static final long serialVersionUID = 508593169141567546L;

  /**
   * Represents the game states displayed to users looking at the list of available lobby games.
   */
  @AllArgsConstructor
  public enum GameStatus {
    LAUNCHING("Launching"),
    IN_PROGRESS("In Progress"),
    WAITING_FOR_PLAYERS("Waiting For Players");

    private final String displayName;

    @Override
    public String toString() {
      return displayName;
    }
  }

  private INode hostedBy;
  private Instant startDateTime;
  private String gameName;
  private int playerCount;
  private String round;
  private GameStatus status;
  private String hostName;
  private String comment;
  private boolean passworded;
  private String gameVersion;

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new IllegalStateException("how did that happen");
    }
  }

  public void setGameName(final String gameName) {
    this.gameName = gameName;
  }

  public void setPlayerCount(final int playerCount) {
    this.playerCount = playerCount;
  }

  public void setRound(final String round) {
    this.round = round;
  }

  public void setStatus(final GameStatus status) {
    this.status = status;
  }

  public void setPassworded(final boolean passworded) {
    this.passworded = passworded;
  }

  public boolean getPassworded() {
    return passworded;
  }

  public void setGameVersion(final String gameVersion) {
    this.gameVersion = gameVersion;
  }

  public String getGameVersion() {
    return gameVersion;
  }

  public boolean isBot() {
    return hostName.startsWith(HeadlessGameServer.BOT_GAME_HOST_NAME_PREFIX)
        && HeadlessGameServer.BOT_GAME_HOST_COMMENT.equals(comment);
  }

  public String getRound() {
    return round;
  }

  public String getGameName() {
    return gameName;
  }

  public INode getHostedBy() {
    return hostedBy;
  }

  public int getPlayerCount() {
    return playerCount;
  }

  public Instant getStartDateTime() {
    return startDateTime;
  }

  public GameStatus getStatus() {
    return status;
  }

  public String getHostName() {
    return hostName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    this.comment = comment;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException {
    hostedBy = new Node();
    ((Node) hostedBy).readExternal(in);
    playerCount = in.readByte();
    round = in.readUTF();
    status = GameStatus.values()[in.readByte()];
    hostName = in.readUTF();
    comment = in.readUTF();
    gameName = in.readUTF();
    passworded = in.readBoolean();
    gameVersion = in.readUTF();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    ((Node) hostedBy).writeExternal(out);
    out.writeByte(playerCount);
    out.writeUTF(round);
    out.writeByte(status.ordinal());
    out.writeUTF(hostName);
    out.writeUTF(comment);
    out.writeUTF(gameName);
    out.writeBoolean(passworded);
    out.writeUTF(gameVersion);
  }
}
