package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

public class ProResourceTracker {

  private final IntegerMap<Resource> resources;
  private IntegerMap<Resource> tempPurchases = new IntegerMap<>();

  public ProResourceTracker(final PlayerID player) {
    resources = player.getResources().getResourcesCopy();
  }

  public ProResourceTracker(final int pus, final GameData data) {
    resources = new IntegerMap<>();
    resources.add(data.getResourceList().getResource(Constants.PUS), pus);
  }

  public boolean hasEnough(final ProPurchaseOption ppo) {
    return getRemaining().greaterThanOrEqualTo(ppo.getCosts());
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
    return getRemaining().allValuesEqual(0);
  }

  public int getTempPUs(final GameData data) {
    final Resource PUs = data.getResourceList().getResource(Constants.PUS);
    return tempPurchases.getInt(PUs);
  }

  @Override
  public String toString() {
    return getRemaining().toString();
  }

  private IntegerMap<Resource> getRemaining() {
    final IntegerMap<Resource> combinedResources = new IntegerMap<>(resources);
    combinedResources.subtract(tempPurchases);
    return combinedResources;
  }

}
