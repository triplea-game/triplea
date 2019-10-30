package org.triplea.http.client.lobby.chat.events.server;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** A message sent from the server to client. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@EqualsAndHashCode
public class ServerMessageEnvelope {
  private static final Gson gson = new Gson();

  @Getter @Nonnull private final ServerMessageType messageType;
  /** Payload itself may be a JSON string */
  @Nonnull private final String payload;

  public static <T> ServerMessageEnvelope packageMessage(
      final ServerMessageType messageType, final T data) {
    return new ServerMessageEnvelope(messageType, gson.toJson(data));
  }

  public StatusUpdate toPlayerStatusChange() {
    Preconditions.checkState(messageType == ServerMessageType.STATUS_CHANGED);
    return gson.fromJson(payload, StatusUpdate.class);
  }

  public PlayerLeft toPlayerLeft() {
    Preconditions.checkState(messageType == ServerMessageType.PLAYER_LEFT);
    return gson.fromJson(payload, PlayerLeft.class);
  }

  public PlayerJoined toPlayerJoined() {
    Preconditions.checkState(messageType == ServerMessageType.PLAYER_JOINED);
    return gson.fromJson(payload, PlayerJoined.class);
  }

  // TODO: Project#12 apply truncation at chat message level here.
  public ChatMessage toChatMessage() {
    Preconditions.checkState(messageType == ServerMessageType.CHAT_MESSAGE);
    return gson.fromJson(payload, ChatMessage.class);
  }

  public PlayerSlapped toPlayerSlapped() {
    Preconditions.checkState(messageType == ServerMessageType.PLAYER_SLAPPED);
    return gson.fromJson(payload, PlayerSlapped.class);
  }

  public PlayerListing toPlayerListing() {
    Preconditions.checkState(messageType == ServerMessageType.PLAYER_LISTING);
    return gson.fromJson(payload, PlayerListing.class);
  }

  public String toErrorMessage() {
    Preconditions.checkState(messageType == ServerMessageType.SERVER_ERROR);
    return gson.fromJson(payload, String.class);
  }

  public String toChatEvent() {
    Preconditions.checkState(messageType == ServerMessageType.CHAT_EVENT);
    return gson.fromJson(payload, String.class);
  }

  /** Message type is used by client to know what kind of JSOn message has been received. */
  public enum ServerMessageType {
    CHAT_EVENT,
    CHAT_MESSAGE,
    PLAYER_JOINED,
    PLAYER_LEFT,
    PLAYER_LISTING,
    PLAYER_SLAPPED,
    SERVER_ERROR,
    STATUS_CHANGED,
  }
}
