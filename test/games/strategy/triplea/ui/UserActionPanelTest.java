package games.strategy.triplea.ui;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

public final class UserActionPanelTest {
  private GameData data;

  private Resource gold;

  private Resource pus;

  private Resource silver;

  @Before
  public void setUp() throws Exception {
    data = parseGameFromResource("test-get-resources-spendable-on-user-actions-for-player.xml");

    gold = data.getResourceList().getResource("gold");
    pus = data.getResourceList().getResource(Constants.PUS);
    silver = data.getResourceList().getResource("silver");
  }

  private GameData parseGameFromResource(final String name) throws Exception {
    final URL url = getClass().getResource(name);
    if (url == null) {
      throw new Exception(String.format("game resource not found: %s", name));
    }

    try (final InputStream is = url.openStream()) {
      final GameParser gameParser = new GameParser(url.toString());
      return gameParser.parse(is, new AtomicReference<>(), false);
    }
  }

  @Test
  public void testGetResourcesSpendableOnUserActionsForPlayer_ShouldReturnOnlyPUResources() {
    final PlayerID player = new PlayerID("player", data);
    player.getResources().add(new IntegerMap<>(Arrays.asList(gold, pus, silver), 42));

    final ResourceCollection resources = UserActionPanel.getResourcesSpendableOnUserActionsForPlayer(player);

    assertThat(resources.getQuantity(gold), equalTo(0));
    assertThat(resources.getQuantity(pus), equalTo(42));
    assertThat(resources.getQuantity(silver), equalTo(0));
  }
}
