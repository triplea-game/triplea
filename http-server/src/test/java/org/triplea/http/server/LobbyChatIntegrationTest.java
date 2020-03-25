package org.triplea.http.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatMessageListeners;
import org.triplea.http.client.lobby.chat.LobbyChatClient;
import org.triplea.http.client.lobby.chat.messages.server.ChatMessage;
import org.triplea.http.client.lobby.chat.messages.server.ChatterList;
import org.triplea.http.client.lobby.chat.messages.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.messages.server.StatusUpdate;
import org.triplea.modules.http.DropwizardTest;

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
@SuppressWarnings("SameParameterValue")
class LobbyChatIntegrationTest extends DropwizardTest {
  private static final int MESSAGE_TIMEOUT = 3000;

  private static final String STATUS = "status";
  private static final String MESSAGE = "sample";

  // caution: api-key values must match database (integration.yml)
  private static final ApiKey MODERATOR_API_KEY = ApiKey.of("MODERATOR");
  private static final ApiKey CHATTER_API_KEY = ApiKey.of("PLAYER");

  private static final UserName MODERATOR_NAME = UserName.of("mod");
  private static final ChatParticipant MODERATOR =
      ChatParticipant.builder().userName(MODERATOR_NAME).isModerator(true).status("").build();

  private static final UserName CHATTER_NAME = UserName.of("chatter");
  private static final ChatParticipant CHATTER =
      ChatParticipant.builder().userName(CHATTER_NAME).isModerator(false).status("").build();

  private List<StatusUpdate> modPlayerStatusEvents = new ArrayList<>();
  private List<UserName> modPlayerLeftEvents = new ArrayList<>();
  private List<ChatParticipant> modPlayerJoinedEvents = new ArrayList<>();
  private List<PlayerSlapped> modPlayerSlappedEvents = new ArrayList<>();
  private List<ChatMessage> modMessageEvents = new ArrayList<>();
  private List<ChatterList> modConnectedEvents = new ArrayList<>();
  private LobbyChatClient moderator;

  private List<StatusUpdate> chatterPlayerStatusEvents = new ArrayList<>();
  private List<UserName> chatterPlayerLeftEvents = new ArrayList<>();
  private List<ChatParticipant> chatterPlayerJoinedEvents = new ArrayList<>();
  private List<PlayerSlapped> chatterPlayerSlappedEvents = new ArrayList<>();
  private List<ChatMessage> chatterMessageEvents = new ArrayList<>();
  private List<ChatterList> chatterConnectedEvents = new ArrayList<>();
  private LobbyChatClient chatter;

  private LobbyChatClient createModerator() {
    final LobbyChatClient moderator =
        // caution: api-key must match values in database (integration.yml)
        new LobbyChatClient(localhost, MODERATOR_API_KEY, err -> {});

    moderator.setChatMessageListeners(
        ChatMessageListeners.builder()
            .playerStatusListener(modPlayerStatusEvents::add)
            .playerLeftListener(modPlayerLeftEvents::add)
            .playerJoinedListener(modPlayerJoinedEvents::add)
            .playerSlappedListener(modPlayerSlappedEvents::add)
            .chatMessageListener(modMessageEvents::add)
            .connectedListener(modConnectedEvents::add)
            .chatEventListener(msg -> {})
            .serverErrorListener(
                error -> {
                  throw new RuntimeException(error);
                })
            .build());
    return moderator;
  }

  private LobbyChatClient createChatter() {
    final LobbyChatClient chatter = new LobbyChatClient(localhost, CHATTER_API_KEY, err -> {});

    chatter.setChatMessageListeners(
        ChatMessageListeners.builder()
            .playerStatusListener(chatterPlayerStatusEvents::add)
            .playerLeftListener(chatterPlayerLeftEvents::add)
            .playerJoinedListener(chatterPlayerJoinedEvents::add)
            .playerSlappedListener(chatterPlayerSlappedEvents::add)
            .chatMessageListener(chatterMessageEvents::add)
            .connectedListener(chatterConnectedEvents::add)
            .chatEventListener(msg -> {})
            .serverErrorListener(
                error -> {
                  throw new RuntimeException(error);
                })
            .build());
    return chatter;
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
    moderator.connect();
    // mod is notified that only 'mod' is in chat
    verifyConnectedPlayers(modConnectedEvents, MODERATOR);
    // mod should be notified of their own entry into chat
    verifyPlayerJoinedEvent(modPlayerJoinedEvents, MODERATOR);
  }

  private void chatterConnects() {
    chatter.connect();
    // chatter should be notified that both 'mod' and 'chatter' are in chat
    verifyConnectedPlayers(chatterConnectedEvents, MODERATOR, CHATTER);
    // chatter should be notified of their own entry into chat
    verifyPlayerJoinedEvent(chatterPlayerJoinedEvents, CHATTER);
    // wait for moderator to receive message that chatter joined
    waitForMessage(modPlayerJoinedEvents, 2);
    // moderator is notified that chatter has joined
    assertThat(
        modPlayerJoinedEvents.get(1),
        is(ChatParticipant.builder().userName(CHATTER_NAME).isModerator(true).status("").build()));
    // moderator should *not* receive a connected event when chatter joins
    assertThat(modConnectedEvents, hasSize(1));
  }

  private static void verifyConnectedPlayers(
      final List<ChatterList> connectedEvents, final ChatParticipant... participants) {
    waitForMessage(connectedEvents);
    assertThat(connectedEvents.get(0).getChatters(), hasItems(participants));
  }

  private static void verifyPlayerJoinedEvent(
      final List<ChatParticipant> playerJoinedEvents, final ChatParticipant expectedPlayer) {
    waitForMessage(playerJoinedEvents);
    assertThat(playerJoinedEvents.get(0), is(expectedPlayer));
  }

  private void chatterChats() {
    // chatter chats
    chatter.sendChatMessage(MESSAGE);
    // moderator should receive chat message from chatter
    verifyChatMessageEvent(modMessageEvents, new ChatMessage(CHATTER_NAME, MESSAGE));
    // chatter should receive their own chat message
    verifyChatMessageEvent(chatterMessageEvents, new ChatMessage(CHATTER_NAME, MESSAGE));
  }

  private static void verifyChatMessageEvent(
      final List<ChatMessage> chatMessageEvents, final ChatMessage expectedChatMessage) {
    waitForMessage(chatMessageEvents);
    assertThat(chatMessageEvents.get(0), is(expectedChatMessage));
  }

  private void chatterSlapsMod() {
    // chatter slaps mod
    chatter.slapPlayer(MODERATOR_NAME);
    // moderator is notified of the slap
    waitForMessage(modPlayerSlappedEvents);

    final PlayerSlapped moderatorSlapped =
        PlayerSlapped.builder().slapper(CHATTER_NAME).slapped(MODERATOR_NAME).build();

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
    assertThat(modPlayerStatusEvents.get(0), is(new StatusUpdate(CHATTER_NAME, STATUS)));
    waitForMessage(chatterPlayerStatusEvents);
    assertThat(chatterPlayerStatusEvents.get(0), is(new StatusUpdate(CHATTER_NAME, STATUS)));

    // chatter is notified of their own status update

  }

  private void chatterLeaves() {
    // chatter disconnects
    chatter.close();
    // chatter is disconnected before they can be notified of their own disconnect
    assertThat(chatterPlayerLeftEvents, empty());

    // moderator is notified chatter has left
    waitForMessage(modPlayerLeftEvents);
    assertThat(modPlayerLeftEvents.get(0), is(CHATTER_NAME));
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
