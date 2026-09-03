package org.triplea.game.server.relay;

import java.util.List;

/**
 * Control-message payloads owned by the relay. Each rides as JSON inside {@link
 * GameRelayEnvelope#payload()} for its matching {@code type}. These are NOT opaque — the relay
 * serializes and deserializes them. Kept as small records with primitive/String fields for Gson.
 */
public final class RelayControl {
  private RelayControl() {}

  /** {@code JOIN} body: the desired display name (the gameId lives on the envelope). */
  public record JoinPayload(String playerName) {}

  /** A member of a game, as reported to clients. */
  public record MemberInfo(String nodeId, String name) {}

  /** {@code WELCOME} body: the assigned identity plus the current member list. */
  public record WelcomePayload(String nodeId, boolean host, List<MemberInfo> members) {}

  /** {@code MEMBER_JOINED} body. */
  public record MemberJoinedPayload(String nodeId, String name) {}

  /** {@code MEMBER_LEFT} body. */
  public record MemberLeftPayload(String nodeId) {}

  /** {@code BOOT} body: the target to remove and whether to also ban its IP. */
  public record BootPayload(String targetNodeId, boolean ban) {}
}
