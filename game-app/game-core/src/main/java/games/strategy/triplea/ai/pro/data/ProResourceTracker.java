package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import org.triplea.java.collections.IntegerMap;

/** Tracks available resources during an AI purchase analysis. */
public class ProResourceTracker {

  private final IntegerMap<Resource> resources;
  private IntegerMap<Resource> tempPurchases = new IntegerMap<>();

  public ProResourceTracker(final GamePlayer player) {
    resources = player.getResources().getResourcesCopy();
  }

  public ProResourceTracker(final int pus, final GameState data) {
    resources = new IntegerMap<>();
    resources.add(data.getResourceList().getResource(Constants.PUS).orElse(null), pus);
  }

  public boolean hasEnough(final ProPurchaseOption ppo) {
    return hasEnough(ppo.getCosts());
  }

  public boolean hasEnough(final IntegerMap<Resource> amount) {
    return getRemaining().greaterThanOrEqualTo(amount);
  }

  public void purchase(final ProPurchaseOption ppo) {
    resources.subtract(ppo.getCosts());
  }

  public void removePurchase(final ProPurchaseOption ppo) {
    if (ppo != null) {
      resources.add(ppo.getCosts());
    }
  }

  public void tempPurchase(final ProPurchaseOption ppo) {
    tempPurchases.add(ppo.getCosts());
  }

  public void removeTempPurchase(final ProPurchaseOption ppo) {
    if (ppo != null) {
      tempPurchases.subtract(ppo.getCosts());
    }
  }

  public void confirmTempPurchases() {
    resources.subtract(tempPurchases);
    clearTempPurchases();
  }

  public void clearTempPurchases() {
    tempPurchases = new IntegerMap<>();
  }

  public boolean isEmpty() {
    final IntegerMap<Resource> remaining = getRemaining();
    return !remaining.isEmpty() && remaining.allValuesEqual(0);
  }

  public int getTempPUs(final GameState data) {
    final Resource pus = data.getResourceList().getResource(Constants.PUS).orElse(null);
    return tempPurchases.getInt(pus);
  }

  @Override
  public String toString() {
    return getRemaining().toString().replaceAll("\n", " ");
  }

  private IntegerMap<Resource> getRemaining() {
    final IntegerMap<Resource> combinedResources = new IntegerMap<>(resources);
    combinedResources.subtract(tempPurchases);
    return combinedResources;
  }
}
