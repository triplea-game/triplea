package org.triplea.game.startup;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.posted.game.pbem.IEmailSender;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.random.IRemoteDiceServer;
import org.junit.jupiter.api.Test;

class SetupModelTest {
  @Test
  void testClearPbfPbemInformation() {
    final GameProperties properties = mock(GameProperties.class);
    SetupModel.clearPbfPbemInformation(properties);

    verify(properties).set(IRemoteDiceServer.NAME, null);
    verify(properties).set(IRemoteDiceServer.GAME_NAME, null);
    verify(properties).set(IRemoteDiceServer.EMAIL_1, null);
    verify(properties).set(IRemoteDiceServer.EMAIL_2, null);
    verify(properties).set(IForumPoster.NAME, null);
    verify(properties).set(IForumPoster.TOPIC_ID, null);
    verify(properties).set(IForumPoster.INCLUDE_SAVEGAME, null);
    verify(properties).set(IForumPoster.POST_AFTER_COMBAT, null);
    verify(properties).set(IEmailSender.SUBJECT, null);
    verify(properties).set(IEmailSender.RECIPIENTS, null);
    verify(properties).set(IEmailSender.POST_AFTER_COMBAT, null);
  }
}
