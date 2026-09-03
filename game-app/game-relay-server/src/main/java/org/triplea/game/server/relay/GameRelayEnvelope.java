package org.triplea.game.server.relay;

import javax.annotation.Nullable;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * The wire frame the relay routes on. It rides inside a {@link MessageEnvelope} (so the shared
 * {@code GenericWebSocket} is untouched): the envelope's {@code messageTypeId} is this record's FQN
 * and its {@code payload} is the JSON of this record.
 *
 * <p>The relay reads ONLY the routing header ({@code gameId}, {@code to}, {@code from}, {@code
 * type}). For data messages the {@code payload} is opaque (base64 supplied by the peer) and the
 * relay never decodes it. For control messages ({@code JOIN}/{@code BOOT}/…) the relay owns the
 * payload shape and deserializes it into the matching {@link RelayControl} record.
 *
 * <p>All fields are String so Gson serialization stays trivial and backward compatible (per {@link
 * WebSocketMessage}).
 *
 * @param gameId Game room this message belongs to.
 * @param to Destination nodeId, or null to broadcast to all other members of the game.
 * @param from Origin nodeId (the relay re-stamps this with the authoritative id on forward).
 * @param correlationId Optional request/response correlation id; carried through untouched.
 * @param type Routing/control type; see the {@code TYPE_*} constants.
 * @param payload Opaque data payload (data messages) or control-message JSON (control messages).
 */
public record GameRelayEnvelope(
    @Nullable String gameId,
    @Nullable String to,
    @Nullable String from,
    @Nullable String correlationId,
    String type,
    @Nullable String payload)
    implements WebSocketMessage {

  public static final MessageType<GameRelayEnvelope> TYPE = MessageType.of(GameRelayEnvelope.class);

  /** Client -> relay: join a game; payload is a {@link RelayControl.JoinPayload}. */
  public static final String TYPE_JOIN = "JOIN";

  /**
   * Relay -> joining client: assigned nodeId + host flag + member list ({@link
   * RelayControl.WelcomePayload}).
   */
  public static final String TYPE_WELCOME = "WELCOME";

  /** Relay -> other members: a member joined ({@link RelayControl.MemberJoinedPayload}). */
  public static final String TYPE_MEMBER_JOINED = "MEMBER_JOINED";

  /** Relay -> other members: a member left ({@link RelayControl.MemberLeftPayload}). */
  public static final String TYPE_MEMBER_LEFT = "MEMBER_LEFT";

  /** Host client -> relay: boot a target nodeId ({@link RelayControl.BootPayload}). */
  public static final String TYPE_BOOT = "BOOT";

  /** Opaque game traffic routed within a game by {@code to}/broadcast. */
  public static final String TYPE_GAME = "GAME";

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
