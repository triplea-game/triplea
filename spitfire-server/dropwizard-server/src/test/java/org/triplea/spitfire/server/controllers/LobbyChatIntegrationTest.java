package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerLeftMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerSlapReceivedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerStatusUpdateReceivedMessage;
import org.triplea.spitfire.server.ControllerIntegrationTest;

/**
 * End-to-end test where we go through a chat sequence exercising all chat features. Runs through
 * the following chat sequence:
 *
 * <ol>
 *   <li>Moderator joins
 *   <li>Chatter joins
 *   <li>Chatter speaks
 *   <li>Chatter slaps moderator
 *   <li>Chatter updates their status
 *   <li>Chatter leaves
 * </ol>
 *
 * Chat is very listener driven, for each of the above we expect various events to be received. To
 * execute this test we will create a series of lists for each event respective to each player.
 * After executing each event we'll wait for the expected list to gain an event (or timeout and
 * fail). Then, after receiving an event, we'll verify the event data and then continue in the
 * sequence.
 *
 * <p>Of note, when a player joins they will receive two events, a 'join' event and a 'connected'
 * event. The 'join' event should notify that they themselves have joined (all players receive
 * this), and second, only they should receive a 'connected' event that informs them of all players
 * that have joined. So we expect 'moderator' to be the only player in the connected list, when
 * chatter joins we expect both moderator and chatter to be in the connected event list.
 */
@RequiredArgsConstructor
class LobbyChatIntegrationTest extends ControllerIntegrationTest {
  private static final int MESSAGE_TIMEOUT = 3000;

  private static final String STATUS = "status";
  private static final String MESSAGE = "sample";

  private static final UserName MODERATOR_NAME = UserName.of("mod");
  private static final ChatParticipant MODERATOR =
      ChatParticipant.builder()
          .userName(MODERATOR_NAME.getValue())
          .isModerator(true)
          .status("")
          .playerChatId("moderator-chat-id")
          .build();

  private static final UserName CHATTER_NAME = UserName.of("chatter");
  private static final ChatParticipant CHATTER =
      ChatParticipant.builder()
          .userName(CHATTER_NAME.getValue())
          .isModerator(false)
          .status("")
          .playerChatId("player-chat-id")
          .build();

  private final URI localhost;

  private final List<PlayerStatusUpdateReceivedMessage> modPlayerStatusEvents = new ArrayList<>();
  private final List<PlayerLeftMessage> modPlayerLeftEvents = new ArrayList<>();
  private final List<PlayerJoinedMessage> modPlayerJoinedEvents = new ArrayList<>();
  private final List<PlayerSlapReceivedMessage> modPlayerSlappedEvents = new ArrayList<>();
  private final List<ChatReceivedMessage> modMessageEvents = new ArrayList<>();
  private final List<ChatterListingMessage> modConnectedEvents = new ArrayList<>();
  private PlayerToLobbyConnection moderator;

  private final List<PlayerStatusUpdateReceivedMessage> chatterPlayerStatusEvents =
      new ArrayList<>();
  private final List<PlayerLeftMessage> chatterPlayerLeftEvents = new ArrayList<>();
  private final List<PlayerJoinedMessage> chatterPlayerJoinedEvents = new ArrayList<>();
  private final List<PlayerSlapReceivedMessage> chatterPlayerSlappedEvents = new ArrayList<>();
  private final List<ChatReceivedMessage> chatterMessageEvents = new ArrayList<>();
  private final List<ChatterListingMessage> chatterConnectedEvents = new ArrayList<>();
  private PlayerToLobbyConnection chatter;

  private PlayerToLobbyConnection createModerator() {
    final PlayerToLobbyConnection newModerator =
        new PlayerToLobbyConnection(
            localhost,
            ControllerIntegrationTest.MODERATOR,
            err -> {
              throw new AssertionError("Error on moderator: " + err);
            });
    newModerator.addMessageListener(
        PlayerStatusUpdateReceivedMessage.TYPE, modPlayerStatusEvents::add);
    newModerator.addMessageListener(PlayerLeftMessage.TYPE, modPlayerLeftEvents::add);
    newModerator.addMessageListener(PlayerJoinedMessage.TYPE, modPlayerJoinedEvents::add);
    newModerator.addMessageListener(PlayerSlapReceivedMessage.TYPE, modPlayerSlappedEvents::add);
    newModerator.addMessageListener(ChatReceivedMessage.TYPE, modMessageEvents::add);
    newModerator.addMessageListener(ChatterListingMessage.TYPE, modConnectedEvents::add);
    return newModerator;
  }

  private PlayerToLobbyConnection createChatter() {
    final PlayerToLobbyConnection newChatter =
        new PlayerToLobbyConnection(
            localhost,
            ControllerIntegrationTest.PLAYER,
            err -> {
              throw new AssertionError("Error on chatter: " + err);
            });
    newChatter.addMessageListener(
        PlayerStatusUpdateReceivedMessage.TYPE, chatterPlayerStatusEvents::add);
    newChatter.addMessageListener(PlayerLeftMessage.TYPE, chatterPlayerLeftEvents::add);
    newChatter.addMessageListener(PlayerJoinedMessage.TYPE, chatterPlayerJoinedEvents::add);
    newChatter.addMessageListener(PlayerSlapReceivedMessage.TYPE, chatterPlayerSlappedEvents::add);
    newChatter.addMessageListener(ChatReceivedMessage.TYPE, chatterMessageEvents::add);
    newChatter.addMessageListener(ChatterListingMessage.TYPE, chatterConnectedEvents::add);
    return newChatter;
  }

  @Test
  @DisplayName("Run through a chat sequence with two players exercising all chat functionality.")
  void chatTest() {
    moderator = createModerator();
    moderatorConnects();
    chatter = createChatter();
    chatterConnects();
    chatterChats();
    chatterSlapsMod();
    chatterUpdatesStatus();
    chatterLeaves();
  }

  private void moderatorConnects() {
    moderator.sendConnectToChatMessage();
    // mod is notified that only 'mod' is in chat
    verifyConnectedPlayers(modConnectedEvents, MODERATOR);
    // mod should be notified of their own entry into chat
    verifyPlayerJoinedEvent(modPlayerJoinedEvents, MODERATOR);
  }

  private void chatterConnects() {
    chatter.sendConnectToChatMessage();
    // chatter should be notified that both 'mod' and 'chatter' are in chat
    verifyConnectedPlayers(chatterConnectedEvents, MODERATOR, CHATTER);
    // chatter should be notified of their own entry into chat
    verifyPlayerJoinedEvent(chatterPlayerJoinedEvents, CHATTER);
    // wait for moderator to receive message that chatter joined
    waitForMessage(modPlayerJoinedEvents, 2);
    // moderator is notified that chatter has joined
    assertThat(
        modPlayerJoinedEvents.get(1).getChatParticipant().getUserName().getValue(),
        is(CHATTER_NAME.getValue()));
    assertThat(modPlayerJoinedEvents.get(1).getChatParticipant().isModerator(), is(false));
    // moderator should *not* receive a connected event when chatter joins
    assertThat(modConnectedEvents, hasSize(1));
  }

  private static void verifyConnectedPlayers(
      final List<ChatterListingMessage> connectedEvents, final ChatParticipant... participants) {
    waitForMessage(connectedEvents);
    assertThat(connectedEvents.get(0).getChatters(), hasItems(participants));
  }

  private static void verifyPlayerJoinedEvent(
      final List<PlayerJoinedMessage> playerJoinedEvents, final ChatParticipant expectedPlayer) {
    waitForMessage(playerJoinedEvents);
    assertThat(playerJoinedEvents.get(0).getChatParticipant(), is(expectedPlayer));
  }

  private void chatterChats() {
    // chatter chats
    chatter.sendChatMessage(MESSAGE);
    // moderator should receive chat message from chatter
    verifyChatMessageEvent(modMessageEvents, new ChatReceivedMessage(CHATTER_NAME, MESSAGE));
    // chatter should receive their own chat message
    verifyChatMessageEvent(chatterMessageEvents, new ChatReceivedMessage(CHATTER_NAME, MESSAGE));
  }

  private static void verifyChatMessageEvent(
      final List<ChatReceivedMessage> chatMessageEvents,
      final ChatReceivedMessage expectedChatMessage) {
    waitForMessage(chatMessageEvents);
    assertThat(chatMessageEvents.get(0), is(expectedChatMessage));
  }

  private void chatterSlapsMod() {
    // chatter slaps mod
    chatter.slapPlayer(MODERATOR_NAME);
    // moderator is notified of the slap
    waitForMessage(modPlayerSlappedEvents);

    final PlayerSlapReceivedMessage moderatorSlapped =
        PlayerSlapReceivedMessage.builder()
            .slappingPlayer(CHATTER_NAME.getValue())
            .slappedPlayer(MODERATOR_NAME.getValue())
            .build();

    assertThat(modPlayerSlappedEvents.get(0), is(moderatorSlapped));
    // chatter is notified of the slap
    waitForMessage(chatterPlayerSlappedEvents);
    assertThat(chatterPlayerSlappedEvents.get(0), is(moderatorSlapped));
  }

  private void chatterUpdatesStatus() {

    // chatter updates their status
    chatter.updateStatus(STATUS);
    // moderator is notified of the status update
    waitForMessage(modPlayerStatusEvents);
    assertThat(
        modPlayerStatusEvents.get(0),
        is(new PlayerStatusUpdateReceivedMessage(CHATTER_NAME, STATUS)));
    waitForMessage(chatterPlayerStatusEvents);
    assertThat(
        chatterPlayerStatusEvents.get(0),
        is(new PlayerStatusUpdateReceivedMessage(CHATTER_NAME, STATUS)));

    // chatter is notified of their own status update

  }

  private void chatterLeaves() {
    // chatter disconnects
    chatter.close();
    // chatter is disconnected before they can be notified of their own disconnect
    assertThat(chatterPlayerLeftEvents, empty());

    // moderator is notified chatter has left
    waitForMessage(modPlayerLeftEvents);
    assertThat(modPlayerLeftEvents.get(0).getUserName(), is(CHATTER_NAME));
  }

  private static <T> void waitForMessage(final Collection<T> messageBuffer) {
    waitForMessage(messageBuffer, 1);
  }

  /** Does a busy wait loop until the given collection is at least a given size. */
  private static <T> void waitForMessage(final Collection<T> messageBuffer, final int minCount) {
    Awaitility.await()
        .atMost(Duration.ofMillis(MESSAGE_TIMEOUT))
        .until(() -> messageBuffer.size() >= minCount);
  }
}
