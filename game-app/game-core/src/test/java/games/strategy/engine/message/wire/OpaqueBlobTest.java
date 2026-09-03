package games.strategy.engine.message.wire;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

/**
 * Confirms an opaque blob carries a {@link Change} through a JSON round-trip (as it would over the
 * wire) and reconstructs an equal mutation, without any dedicated codec for the change type.
 */
class OpaqueBlobTest {
  private static final Gson gson = new Gson();

  private final GameData gameData = TestMapGameData.REVISED.getGameData();

  @Test
  void changeRoundTripsUnchanged() {
    final Territory territory = gameData.getMap().getTerritories().get(0);
    final GamePlayer newOwner = gameData.getPlayerList().getPlayers().get(0);
    final Change change = ChangeFactory.changeOwner(territory, newOwner);

    final OpaqueBlob blob = OpaqueBlob.of(change);
    final OpaqueBlob envelope = gson.fromJson(gson.toJson(blob), OpaqueBlob.class);
    final Change restored = envelope.read(Change.class);

    // OwnerChange has no value equality, so compare the serialized form: identical bytes prove the
    // change survived the round-trip untouched.
    assertThat(OpaqueBlob.of(restored), is(blob));
    assertThat(restored.toString(), is(change.toString()));
  }
}
