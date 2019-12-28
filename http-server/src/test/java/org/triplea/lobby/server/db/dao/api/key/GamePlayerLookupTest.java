package org.triplea.lobby.server.db.dao.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.domain.data.SystemId;

@ExtendWith(MockitoExtension.class)
class GamePlayerLookupTest {

  private static final String PLAYER_NAME = "player-name";
  private static final String SYSTEM_ID = "player-name";
  private static final String IP = "ip-address";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    givenMockedDatabaseResults();

    final GamePlayerLookup result = GamePlayerLookup.buildResultMapper().map(resultSet, null);

    assertThat(result.getPlayerName(), is(PlayerName.of(PLAYER_NAME)));
    assertThat(result.getSystemId(), is(SystemId.of(SYSTEM_ID)));
    assertThat(result.getIp(), is(IP));
  }

  private void givenMockedDatabaseResults() throws Exception {
    when(resultSet.getString(GamePlayerLookup.PLAYER_NAME_COLUMN)).thenReturn(PLAYER_NAME);
    when(resultSet.getString(GamePlayerLookup.SYSTEM_ID_COLUMN)).thenReturn(SYSTEM_ID);
    when(resultSet.getString(GamePlayerLookup.IP_COLUMN)).thenReturn(IP);
  }
}
