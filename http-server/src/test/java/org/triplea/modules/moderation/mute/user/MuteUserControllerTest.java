package org.triplea.modules.moderation.mute.user;

import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.MuteUserRequest;
import org.triplea.java.DateTimeUtil;
import org.triplea.modules.chat.Chatters;

@ExtendWith(MockitoExtension.class)
class MuteUserControllerTest {
  private static final Instant NOW = DateTimeUtil.utcInstantOf(2020, 1, 1, 23, 0);
  private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Mock private Chatters chatters;

  @Test
  void muteUser() {
    final var muteUserController = new MuteUserController(chatters, FIXED_CLOCK);

    muteUserController.muteUser(
        MuteUserRequest.builder().playerChatId("player-chat-id").minutes(20).build());

    verify(chatters)
        .mutePlayer(PlayerChatId.of("player-chat-id"), NOW.plus(20, ChronoUnit.MINUTES));
  }
}
