package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Allows client nodes to access various information from the server node during network game setup.
 */
public interface IServerStartupRemote extends IRemote {
  /** Returns a listing of the players in the game. */
  PlayerListing getPlayerListing();

  /** Typed request asking for the current player listing. */
  class GetPlayerListingRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6521329911993246421L;

    public static final MessageType<GetPlayerListingRequest> TYPE =
        MessageType.of(GetPlayerListingRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the current player listing. */
  @AllArgsConstructor
  class GetPlayerListingResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1998311143214551900L;

    public static final MessageType<GetPlayerListingResponse> TYPE =
        MessageType.of(GetPlayerListingResponse.class);

    @Getter @Nullable private final PlayerListing playerListing;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void takePlayer(INode who, String playerName);

  /** Typed request to assign a player to the requesting node. */
  @AllArgsConstructor
  class TakePlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8524325324422162901L;

    public static final MessageType<TakePlayerRequest> TYPE =
        MessageType.of(TakePlayerRequest.class);

    @Getter private final INode who;
    @Getter private final String playerName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that a take-player request was applied. */
  class TakePlayerResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3320048576544671220L;

    public static final MessageType<TakePlayerResponse> TYPE =
        MessageType.of(TakePlayerResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void releasePlayer(INode who, String playerName);

  /** Typed request to release a player held by the requesting node. */
  @AllArgsConstructor
  class ReleasePlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6042707044399855230L;

    public static final MessageType<ReleasePlayerRequest> TYPE =
        MessageType.of(ReleasePlayerRequest.class);

    @Getter private final INode who;
    @Getter private final String playerName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that a release-player request was applied. */
  class ReleasePlayerResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1176990477818062200L;

    public static final MessageType<ReleasePlayerResponse> TYPE =
        MessageType.of(ReleasePlayerResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void disablePlayer(String playerName);

  /** Typed request to disable a player. */
  @AllArgsConstructor
  class DisablePlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7231480918852701881L;

    public static final MessageType<DisablePlayerRequest> TYPE =
        MessageType.of(DisablePlayerRequest.class);

    @Getter private final String playerName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that a disable-player request was applied. */
  class DisablePlayerResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5623410992288730121L;

    public static final MessageType<DisablePlayerResponse> TYPE =
        MessageType.of(DisablePlayerResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void enablePlayer(String playerName);

  /** Typed request to enable a player. */
  @AllArgsConstructor
  class EnablePlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4409922184773554010L;

    public static final MessageType<EnablePlayerRequest> TYPE =
        MessageType.of(EnablePlayerRequest.class);

    @Getter private final String playerName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that an enable-player request was applied. */
  class EnablePlayerResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8804441823390124551L;

    public static final MessageType<EnablePlayerResponse> TYPE =
        MessageType.of(EnablePlayerResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Has the game already started? If true, the server will call our ObserverWaitingToJoin to start
   * the game. Note, the return value may come back after our ObserverWaitingToJoin has been created
   */
  boolean isGameStarted(INode newNode);

  /** Typed request asking whether the game has already started. */
  @AllArgsConstructor
  class IsGameStartedRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2210492388235510811L;

    public static final MessageType<IsGameStartedRequest> TYPE =
        MessageType.of(IsGameStartedRequest.class);

    @Getter private final INode newNode;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying whether the game has already started. */
  @AllArgsConstructor
  class IsGameStartedResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6633120998234771201L;

    public static final MessageType<IsGameStartedResponse> TYPE =
        MessageType.of(IsGameStartedResponse.class);

    @Getter private final boolean gameStarted;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  boolean getIsServerHeadless();

  /** Typed request asking whether the server node is a headless (bot) host. */
  class GetServerHeadlessRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5559981337288369814L;

    public static final MessageType<GetServerHeadlessRequest> TYPE =
        MessageType.of(GetServerHeadlessRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the server's headless flag. */
  @AllArgsConstructor
  class GetServerHeadlessResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3623145572069361182L;

    public static final MessageType<GetServerHeadlessResponse> TYPE =
        MessageType.of(GetServerHeadlessResponse.class);

    @Getter private final boolean serverHeadless;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  List<String> getAvailableGames();

  /** Typed request asking for the games available on the server. */
  class GetAvailableGamesRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 9033120441226751920L;

    public static final MessageType<GetAvailableGamesRequest> TYPE =
        MessageType.of(GetAvailableGamesRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply listing the games available on the server. */
  @AllArgsConstructor
  class GetAvailableGamesResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1240099183367720145L;

    public static final MessageType<GetAvailableGamesResponse> TYPE =
        MessageType.of(GetAvailableGamesResponse.class);

    @Getter private final List<String> availableGames;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void changeServerGameTo(String gameName);

  /** Typed request to change the server's selected game. */
  @AllArgsConstructor
  class ChangeServerGameToRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3312047744991855120L;

    public static final MessageType<ChangeServerGameToRequest> TYPE =
        MessageType.of(ChangeServerGameToRequest.class);

    @Getter private final String gameName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the change-server-game request was applied. */
  class ChangeServerGameToResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8890043321997611450L;

    public static final MessageType<ChangeServerGameToResponse> TYPE =
        MessageType.of(ChangeServerGameToResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void changeToGameSave(byte[] bytes, String fileName);

  /** Typed request to load a save game across the network. */
  @AllArgsConstructor
  class ChangeToGameSaveRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2201499338874550120L;

    public static final MessageType<ChangeToGameSaveRequest> TYPE =
        MessageType.of(ChangeToGameSaveRequest.class);

    @Getter private final byte[] bytes;
    @Getter private final String fileName;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the change-to-game-save request was applied. */
  class ChangeToGameSaveResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7761209048822013301L;

    public static final MessageType<ChangeToGameSaveResponse> TYPE =
        MessageType.of(ChangeToGameSaveResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  byte[] getGameOptions();

  /** Typed request asking for the current game options bytes. */
  class GetGameOptionsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4459920014471985511L;

    public static final MessageType<GetGameOptionsRequest> TYPE =
        MessageType.of(GetGameOptionsRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the current game options bytes. */
  @AllArgsConstructor
  class GetGameOptionsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5590048833120147822L;

    public static final MessageType<GetGameOptionsResponse> TYPE =
        MessageType.of(GetGameOptionsResponse.class);

    @Getter private final byte[] gameOptions;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  void changeToGameOptions(byte[] bytes);

  /** Typed request to apply new game options across the network. */
  @AllArgsConstructor
  class ChangeToGameOptionsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3301488220997641551L;

    public static final MessageType<ChangeToGameOptionsRequest> TYPE =
        MessageType.of(ChangeToGameOptionsRequest.class);

    @Getter private final byte[] bytes;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the change-to-game-options request was applied. */
  class ChangeToGameOptionsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8890132048771620033L;

    public static final MessageType<ChangeToGameOptionsResponse> TYPE =
        MessageType.of(ChangeToGameOptionsResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  interface ServerModelView {
    PlayerListing getPlayerListing();

    void takePlayer(final INode who, final String playerName);

    void releasePlayer(final INode who, final String playerName);

    void disablePlayer(final String playerName);

    void enablePlayer(final String playerName);

    boolean isGameStarted(final INode newNode);

    byte[] getGameOptions();
  }
}
