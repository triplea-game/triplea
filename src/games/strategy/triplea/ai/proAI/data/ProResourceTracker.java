package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

public class ProResourceTracker {

  private IntegerMap<Resource> resources;
  private IntegerMap<Resource> tempPurchases;

  public ProResourceTracker(PlayerID player) {
    resources = player.getResources().getResourcesCopy();
    tempPurchases = new IntegerMap<Resource>();
  }

  public boolean hasEnough(ProPurchaseOption ppo) {
    return getRemaining().greaterThanOrEqualTo(ppo.getCosts());
  }

  public void purchase(ProPurchaseOption ppo) {
    resources.subtract(ppo.getCosts());
  }

  public void removePurchase(ProPurchaseOption ppo) {
    if (ppo != null) {
      resources.add(ppo.getCosts());
    }
  }

  public void tempPurchase(ProPurchaseOption ppo) {
    tempPurchases.add(ppo.getCosts());
  }

  public void removeTempPurchase(ProPurchaseOption ppo) {
    if (ppo != null) {
      tempPurchases.subtract(ppo.getCosts());
    }
  }

  public void confirmTempPurchases() {
    resources.subtract(tempPurchases);
    clearTempPurchases();
  }

  public void clearTempPurchases() {
    tempPurchases = new IntegerMap<Resource>();
  }

  public boolean isEmpty() {
    return getRemaining().allValuesEqual(0);
  }

  public int getTempPUs(GameData data) {
    final Resource PUs = data.getResourceList().getResource(Constants.PUS);
    return tempPurchases.getInt(PUs);
  }

  @Override
  public String toString() {
    return getRemaining().toString();
  }

  private IntegerMap<Resource> getRemaining() {
    IntegerMap<Resource> combinedResources = new IntegerMap<Resource>(resources);
    combinedResources.subtract(tempPurchases);
    return combinedResources;
  }

}
