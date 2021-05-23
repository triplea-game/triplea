package org.triplea.spitfire.server.controllers;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.MuteUserRequest;
import org.triplea.modules.chat.Chatters;
import org.triplea.spitfire.server.controllers.lobby.moderation.MuteUserController;

@SuppressWarnings("UnmatchedTest")
@ExtendWith(MockitoExtension.class)
class MuteUserControllerTest {
  @Mock private Chatters chatters;

  @Test
  void muteUser() {
    final var muteUserController = new MuteUserController(chatters);

    muteUserController.muteUser(
        MuteUserRequest.builder().playerChatId("player-chat-id").minutes(20).build());

    verify(chatters).mutePlayer(PlayerChatId.of("player-chat-id"), 20);
  }
}
