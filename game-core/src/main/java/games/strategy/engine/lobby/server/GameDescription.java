package games.strategy.engine.lobby.server;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.time.Instant;

import org.triplea.game.server.HeadlessGameServer;

import games.strategy.net.INode;
import games.strategy.net.Node;
import lombok.Builder;
import lombok.ToString;

// TODO: move this class to lobby.common upon next lobby-incompatible release; it is shared between client and server

/**
 * NOTE - this class is not thread safe. Modifications should be done holding an external lock.
 */
@ToString
public class GameDescription implements Externalizable, Cloneable {
  private static final long serialVersionUID = 508593169141567546L;

  /**
   * Represents the game states displayed to users looking at the list of available lobby games.
   */
  public enum GameStatus {
    LAUNCHING {
      @Override
      public String toString() {
        return "Launching";
      }
    },
    IN_PROGRESS {
      @Override
      public String toString() {
        return "In Progress";
      }
    },
    WAITING_FOR_PLAYERS {
      @Override
      public String toString() {
        return "Waiting For Players";
      }
    }
  }

  private INode hostedBy;

  /**
   * Kept for compatibility. Remove in the next lobby-incompatible release.
   *
   * @deprecated This field is redundant, the Node stored in hostedBy is completely sufficient.
   */
  @Deprecated
  private int port;

  /**
   * Represents when the game started, used to be displayed on lobby table, now no longer.
   *
   * @deprecated No longer used, waiting for non-compatible change opportunity to remove.
   */
  @Deprecated
  private Instant startDateTime;
  private String gameName;
  private int playerCount;
  private String round;
  private GameStatus status;
  private int version = Integer.MIN_VALUE;
  private String hostName;
  private String comment;
  private boolean passworded;
  /**
   * Engine version, used to be useful when multiple engine versions were in same lobby,
   * now that lobby has homogeneous versions and should going forward, this column is no longer useful.
   *
   * @deprecated No longer used, waiting for non-compatible change opportunity to remove.
   */
  @Deprecated
  private String engineVersion;
  private String gameVersion;

  // if you add a field, add it to write/read object as well for Externalizable
  public GameDescription() {}

  @Builder
  private GameDescription(
      final INode hostedBy,
      final int port,
      final Instant startDateTime,
      final String gameName,
      final int playerCount,
      final GameStatus status,
      final String round,
      final String hostName,
      final String comment,
      final boolean passworded,
      final String engineVersion,
      final String gameVersion) {
    this.hostName = hostName;
    this.hostedBy = hostedBy;
    this.port = port;
    this.startDateTime = startDateTime;
    this.gameName = gameName;
    this.playerCount = playerCount;
    this.status = status;
    this.round = round;
    this.comment = comment;
    this.passworded = passworded;
    this.engineVersion = engineVersion;
    this.gameVersion = gameVersion;
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new IllegalStateException("how did that happen");
    }
  }

  /**
   * The version number is updated after every change. This handles
   * synchronization problems where updates arrive out of order
   */
  public int getVersion() {
    return version;
  }

  public void setGameName(final String gameName) {
    version++;
    this.gameName = gameName;
  }

  public void setHostedBy(final INode hostedBy) {
    version++;
    this.hostedBy = hostedBy;
  }

  public void setPlayerCount(final int playerCount) {
    version++;
    this.playerCount = playerCount;
  }

  @Deprecated
  public void setPort(final int port) {
    version++;
    this.port = port;
  }

  public void setRound(final String round) {
    version++;
    this.round = round;
  }

  public void setStartDateTime(final Instant startDateTime) {
    version++;
    this.startDateTime = startDateTime;
  }

  public void setStatus(final GameStatus status) {
    version++;
    this.status = status;
  }

  public void setPassworded(final boolean passworded) {
    version++;
    this.passworded = passworded;
  }

  public boolean getPassworded() {
    return passworded;
  }

  public void setEngineVersion(final String engineVersion) {
    version++;
    this.engineVersion = engineVersion;
  }

  public void setGameVersion(final String gameVersion) {
    version++;
    this.gameVersion = gameVersion;
  }

  public String getEngineVersion() {
    return engineVersion;
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

  @Deprecated
  public int getPort() {
    return port;
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

  public void setHostName(final String hostName) {
    version++;
    this.hostName = hostName;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(final String comment) {
    version++;
    this.comment = comment;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException {
    hostedBy = new Node();
    ((Node) hostedBy).readExternal(in);
    port = in.readInt();
    startDateTime = Instant.ofEpochMilli(in.readLong());
    playerCount = in.readByte();
    round = in.readUTF();
    status = GameStatus.values()[in.readByte()];
    version = in.readInt();
    hostName = in.readUTF();
    comment = in.readUTF();
    gameName = in.readUTF();
    passworded = in.readBoolean();
    engineVersion = in.readUTF();
    gameVersion = in.readUTF();
    // TODO: was bot support email, delete this when ready to break 1.9.0 network compatibility
    in.readUTF();
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    ((Node) hostedBy).writeExternal(out);
    out.writeInt(port);
    out.writeLong(startDateTime.toEpochMilli());
    out.writeByte(playerCount);
    out.writeUTF(round);
    out.writeByte(status.ordinal());
    out.writeInt(version);
    out.writeUTF(hostName);
    out.writeUTF(comment);
    out.writeUTF(gameName);
    out.writeBoolean(passworded);
    out.writeUTF(engineVersion);
    out.writeUTF(gameVersion);
    // TODO: was bot support email, delete this when ready to break 1.9.0 network compatibility
    out.writeUTF("");
  }
}
