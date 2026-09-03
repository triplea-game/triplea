package org.triplea.game.server.relay;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.CloseReason;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.server.relay.RelayControl.BootPayload;
import org.triplea.game.server.relay.RelayControl.JoinPayload;
import org.triplea.game.server.relay.RelayControl.MemberInfo;
import org.triplea.game.server.relay.RelayControl.MemberJoinedPayload;
import org.triplea.game.server.relay.RelayControl.MemberLeftPayload;
import org.triplea.game.server.relay.RelayControl.WelcomePayload;
import org.triplea.web.socket.WebSocketMessageContext;
import org.triplea.web.socket.WebSocketMessagingBus;
import org.triplea.web.socket.WebSocketSession;

/**
 * Multiplexes many games onto one relay and routes {@link GameRelayEnvelope}s within a single game.
 *
 * <p>Routing rules:
 *
 * <ul>
 *   <li>A data message with {@code to == null} is delivered to all OTHER members of its game.
 *   <li>A data message with a {@code to} nodeId is delivered only to that member (same game).
 *   <li>Messages never cross game ids.
 * </ul>
 *
 * <p>The first client to JOIN a game is designated its host and gains boot/ban authority. The relay
 * only reads routing headers; data payloads are never decoded.
 */
@Slf4j
public class GameRelayRouter {
  private static final Gson GSON = new Gson();

  private final WebSocketMessagingBus bus;
  private final Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
  private final Map<String, Member> membersBySessionId = new ConcurrentHashMap<>();
  private final Set<InetAddress> bannedIps = ConcurrentHashMap.newKeySet();

  public GameRelayRouter(final WebSocketMessagingBus bus) {
    this.bus = bus;
  }

  /** A connected, joined participant of a game. */
  private static final class Member {
    private final String nodeId;
    private final String name;
    private final String gameId;
    private final WebSocketSession session;
    private final boolean host;

    private Member(
        final String nodeId,
        final String name,
        final String gameId,
        final WebSocketSession session,
        final boolean host) {
      this.nodeId = nodeId;
      this.name = name;
      this.gameId = gameId;
      this.session = session;
      this.host = host;
    }
  }

  /** Members of a single game. Membership mutations are guarded by synchronizing on the room. */
  private static final class GameRoom {
    private final String gameId;
    private final Map<String, Member> members = new LinkedHashMap<>();

    private GameRoom(final String gameId) {
      this.gameId = gameId;
    }
  }

  public boolean isIpBanned(final InetAddress inetAddress) {
    return bannedIps.contains(inetAddress);
  }

  public void handleMessage(final WebSocketMessageContext<GameRelayEnvelope> context) {
    final GameRelayEnvelope envelope = context.getMessage();
    final WebSocketSession session = context.getSenderSession();
    switch (envelope.type()) {
      case GameRelayEnvelope.TYPE_JOIN -> handleJoin(session, envelope);
      case GameRelayEnvelope.TYPE_BOOT -> handleBoot(session, envelope);
      default -> handleData(session, envelope);
    }
  }

  private void handleJoin(final WebSocketSession session, final GameRelayEnvelope envelope) {
    final String gameId = envelope.gameId();
    if (gameId == null) {
      log.warn("Ignoring JOIN with no gameId");
      return;
    }
    final JoinPayload joinPayload =
        envelope.payload() == null
            ? new JoinPayload("player")
            : GSON.fromJson(envelope.payload(), JoinPayload.class);
    final String name = joinPayload == null ? "player" : joinPayload.playerName();

    final GameRoom room = rooms.computeIfAbsent(gameId, GameRoom::new);
    final String nodeId = UUID.randomUUID().toString();
    final boolean isHost;
    final Member member;
    final List<MemberInfo> memberSnapshot;
    synchronized (room) {
      isHost = room.members.isEmpty();
      member = new Member(nodeId, name, gameId, session, isHost);
      room.members.put(nodeId, member);
      memberSnapshot = snapshot(room);
    }
    membersBySessionId.put(session.getId(), member);

    send(
        session,
        new GameRelayEnvelope(
            gameId,
            nodeId,
            null,
            null,
            GameRelayEnvelope.TYPE_WELCOME,
            GSON.toJson(new WelcomePayload(nodeId, isHost, memberSnapshot))));

    broadcastToOthers(
        room,
        nodeId,
        new GameRelayEnvelope(
            gameId,
            null,
            nodeId,
            null,
            GameRelayEnvelope.TYPE_MEMBER_JOINED,
            GSON.toJson(new MemberJoinedPayload(nodeId, name))));
  }

  private void handleData(final WebSocketSession session, final GameRelayEnvelope envelope) {
    final Member sender = membersBySessionId.get(session.getId());
    if (sender == null) {
      log.warn("Ignoring data message from a session that has not joined a game");
      return;
    }
    final GameRoom room = rooms.get(sender.gameId);
    if (room == null) {
      return;
    }
    // Re-stamp gameId + from with the authoritative values so a peer cannot spoof them or route
    // across games. The payload is left untouched (opaque).
    final GameRelayEnvelope forward =
        new GameRelayEnvelope(
            sender.gameId,
            envelope.to(),
            sender.nodeId,
            envelope.correlationId(),
            envelope.type(),
            envelope.payload());

    if (envelope.to() == null) {
      broadcastToOthers(room, sender.nodeId, forward);
    } else {
      final Member target;
      synchronized (room) {
        target = room.members.get(envelope.to());
      }
      if (target != null) {
        send(target.session, forward);
      }
    }
  }

  private void handleBoot(final WebSocketSession session, final GameRelayEnvelope envelope) {
    final Member requester = membersBySessionId.get(session.getId());
    if (requester == null || !requester.host) {
      log.warn("Rejecting BOOT from a non-host session");
      return;
    }
    final BootPayload bootPayload =
        envelope.payload() == null ? null : GSON.fromJson(envelope.payload(), BootPayload.class);
    if (bootPayload == null || bootPayload.targetNodeId() == null) {
      return;
    }
    final GameRoom room = rooms.get(requester.gameId);
    if (room == null) {
      return;
    }
    final Member target;
    synchronized (room) {
      target = room.members.get(bootPayload.targetNodeId());
    }
    if (target == null) {
      return;
    }
    if (bootPayload.ban()) {
      bannedIps.add(target.session.getRemoteAddress());
    }
    removeMember(room, target);
    target.session.close(
        new CloseReason(
            CloseReason.CloseCodes.NORMAL_CLOSURE,
            bootPayload.ban() ? "You have been banned" : "Removed by host"));
  }

  /** Invoked by the messaging bus when a session disconnects; cleans up any membership. */
  public void onDisconnect(final WebSocketSession session) {
    final Member member = membersBySessionId.get(session.getId());
    if (member == null) {
      return;
    }
    final GameRoom room = rooms.get(member.gameId);
    if (room != null) {
      removeMember(room, member);
    }
  }

  private void removeMember(final GameRoom room, final Member member) {
    final boolean removed;
    final boolean empty;
    synchronized (room) {
      removed = room.members.remove(member.nodeId) != null;
      empty = room.members.isEmpty();
    }
    membersBySessionId.remove(member.session.getId());
    if (!removed) {
      return;
    }
    if (empty) {
      rooms.remove(room.gameId, room);
    }
    broadcastToOthers(
        room,
        member.nodeId,
        new GameRelayEnvelope(
            room.gameId,
            null,
            member.nodeId,
            null,
            GameRelayEnvelope.TYPE_MEMBER_LEFT,
            GSON.toJson(new MemberLeftPayload(member.nodeId))));
  }

  private void broadcastToOthers(
      final GameRoom room, final String excludeNodeId, final GameRelayEnvelope envelope) {
    final List<Member> targets;
    synchronized (room) {
      targets =
          room.members.values().stream().filter(m -> !m.nodeId.equals(excludeNodeId)).toList();
    }
    targets.forEach(target -> send(target.session, envelope));
  }

  private static List<MemberInfo> snapshot(final GameRoom room) {
    return room.members.values().stream().map(m -> new MemberInfo(m.nodeId, m.name)).toList();
  }

  /** Non-blocking fan-out send (the messaging bus dispatches each send on its own thread). */
  private void send(final WebSocketSession session, final GameRelayEnvelope envelope) {
    bus.sendResponse(session, envelope);
  }

  @VisibleForTesting
  public int memberCount(final String gameId) {
    final GameRoom room = rooms.get(gameId);
    if (room == null) {
      return 0;
    }
    synchronized (room) {
      return room.members.size();
    }
  }
}
