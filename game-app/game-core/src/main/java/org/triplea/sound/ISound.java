package org.triplea.sound;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A sound channel allowing sounds normally played on the server (for example: in a delegate, such
 * as a the move delegate) to also be played on clients.
 */
public interface ISound extends IChannelSubscriber {

  /**
   * You will want to call this from things that the server only runs (like delegates), and not call
   * this from user interface elements (because all users have these).
   *
   * @param clipName The name of the sound clip to play, found in SoundPath.java
   * @param gamePlayer The player who's sound we want to play (ie: russians infantry might make
   *     different sounds from german infantry, etc). Can be null.
   */
  @RemoteActionCode(0)
  void playSoundForAll(String clipName, GamePlayer gamePlayer);

  /** Typed-dispatch payload for {@link #playSoundForAll(String, GamePlayer)}. */
  class PlaySoundForAllMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8371246170014820001L;

    public static final MessageType<PlaySoundForAllMessage> TYPE =
        MessageType.of(PlaySoundForAllMessage.class);

    private final String clipName;
    @Nullable private final EntityRef gamePlayer;

    public PlaySoundForAllMessage(final String clipName, @Nullable final GamePlayer gamePlayer) {
      this.clipName = clipName;
      this.gamePlayer = gamePlayer == null ? null : EntityRef.of(gamePlayer);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final ISound sound, final GameData gameData) {
      sound.playSoundForAll(
          clipName, gamePlayer == null ? null : gamePlayer.resolvePlayer(gameData));
    }
  }

  /**
   * You will want to call this from things that the server only runs (like delegates), and not call
   * this from user interface elements (because all users have these).
   *
   * @param clipName The name of the sound clip to play, found in SoundPath.java
   * @param playersToSendTo The machines controlling these PlayerId's who we want to hear this
   *     sound.
   * @param butNotThesePlayers The machines controlling these PlayerId's who we do not want to hear
   *     this sound. If the machine controls players in both playersToSendTo and butNotThesePlayers,
   *     they will not hear a sound. (Can be null.)
   * @param includeObservers Whether to include non-playing machines
   */
  @RemoteActionCode(1)
  void playSoundToPlayers(
      String clipName,
      Collection<GamePlayer> playersToSendTo,
      Collection<GamePlayer> butNotThesePlayers,
      boolean includeObservers);

  /**
   * Typed-dispatch payload for {@link #playSoundToPlayers(String, Collection, Collection,
   * boolean)}.
   */
  class PlaySoundToPlayersMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8371246170014820002L;

    public static final MessageType<PlaySoundToPlayersMessage> TYPE =
        MessageType.of(PlaySoundToPlayersMessage.class);

    private final String clipName;
    private final List<EntityRef> playersToSendTo;
    @Nullable private final List<EntityRef> butNotThesePlayers;
    private final boolean includeObservers;

    public PlaySoundToPlayersMessage(
        final String clipName,
        final Collection<GamePlayer> playersToSendTo,
        @Nullable final Collection<GamePlayer> butNotThesePlayers,
        final boolean includeObservers) {
      this.clipName = clipName;
      this.playersToSendTo = toRefs(playersToSendTo);
      this.butNotThesePlayers = butNotThesePlayers == null ? null : toRefs(butNotThesePlayers);
      this.includeObservers = includeObservers;
    }

    private static List<EntityRef> toRefs(final Collection<GamePlayer> players) {
      final List<EntityRef> refs = new ArrayList<>(players.size());
      for (final GamePlayer player : players) {
        refs.add(EntityRef.of(player));
      }
      return refs;
    }

    private static List<GamePlayer> resolve(
        @Nullable final List<EntityRef> refs, final GameData gameData) {
      if (refs == null) {
        return null;
      }
      final List<GamePlayer> players = new ArrayList<>(refs.size());
      for (final EntityRef ref : refs) {
        players.add(ref.resolvePlayer(gameData));
      }
      return players;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void accept(final ISound sound, final GameData gameData) {
      sound.playSoundToPlayers(
          clipName,
          resolve(playersToSendTo, gameData),
          resolve(butNotThesePlayers, gameData),
          includeObservers);
    }
  }
}
