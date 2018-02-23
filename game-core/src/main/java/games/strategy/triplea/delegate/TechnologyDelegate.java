package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * Logic for dealing with player tech rolls. This class requires the
 * TechActivationDelegate which actually activates the tech.
 */
@MapSupport
public class TechnologyDelegate extends BaseTripleADelegate implements ITechDelegate {
  private int techCost;
  private HashMap<PlayerID, Collection<TechAdvance>> techs;
  private TechnologyFrontier techCategory;
  private boolean needToInitialize = true;

  /** Creates new TechnolgoyDelegate. */
  public TechnologyDelegate() {}

  @Override
  public void initialize(final String name, final String displayName) {
    super.initialize(name, displayName);
    techs = new HashMap<>();
    techCost = -1;
  }

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public void start() {
    super.start();
    if (!needToInitialize) {
      return;
    }
    if (Properties.getTriggers(getData())) {
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      final Predicate<TriggerAttachment> technologyDelegateTriggerMatch = AbstractTriggerAttachment.availableUses
          .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
          .and(TriggerAttachment.techAvailableMatch());
      // get all possible triggers based on this match.
      final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
          new HashSet<>(Collections.singleton(player)), technologyDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final HashMap<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final List<TriggerAttachment> toFireTestedAndSatisfied =
            CollectionUtils.getMatches(toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerAvailableTechChange(new HashSet<>(toFireTestedAndSatisfied), bridge,
            null, null, true, true, true, true);
      }
    }
    needToInitialize = false;
  }

  @Override
  public void end() {
    super.end();
    needToInitialize = true;
  }

  @Override
  public Serializable saveState() {
    final TechnologyExtendedDelegateState state = new TechnologyExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = needToInitialize;
    state.m_techs = techs;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final TechnologyExtendedDelegateState s = (TechnologyExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.m_needToInitialize;
    techs = s.m_techs;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!Properties.getTechDevelopment(getData())) {
      return false;
    }
    if (!TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(player, getData())) {
      return false;
    }
    if (Properties.getWW2V3TechModel(getData())) {
      final Resource techtokens = getData().getResourceList().getResource(Constants.TECH_TOKENS);
      if (techtokens != null) {
        final int techTokens = player.getResources().getQuantity(techtokens);
        if (techTokens > 0) {
          return true;
        }
      }
    }
    final int techCost = TechTracker.getTechCost(player);
    int money = player.getResources().getQuantity(Constants.PUS);
    if (money < techCost) {
      final PlayerAttachment pa = PlayerAttachment.get(player);
      if (pa == null) {
        return false;
      }
      final Collection<PlayerID> helpPay = pa.getHelpPayTechCost();
      if ((helpPay == null) || helpPay.isEmpty()) {
        return false;
      }
      for (final PlayerID p : helpPay) {
        money += p.getResources().getQuantity(Constants.PUS);
      }
      return money >= techCost;
    }
    return true;
  }

  public Map<PlayerID, Collection<TechAdvance>> getAdvances() {
    return techs;
  }

  private boolean isWW2V2() {
    return Properties.getWW2V2(getData());
  }

  private boolean isWW2V3TechModel() {
    return Properties.getWW2V3TechModel(getData());
  }

  private boolean isSelectableTechRoll() {
    return Properties.getSelectableTechRoll(getData());
  }

  private boolean isLowLuckTechOnly() {
    return Properties.getLowLuckTechOnly(getData());
  }

  @Override
  public TechResults rollTech(final int techRolls, final TechnologyFrontier techToRollFor, final int newTokens,
      final IntegerMap<PlayerID> whoPaysHowMuch) {
    int rollCount = techRolls;
    if (isWW2V3TechModel()) {
      rollCount = newTokens;
    }
    final boolean canPay = checkEnoughMoney(rollCount, whoPaysHowMuch);
    if (!canPay) {
      return new TechResults("Not enough money to pay for that many tech rolls.");
    }
    chargeForTechRolls(rollCount, whoPaysHowMuch);
    int currTokens = 0;
    if (isWW2V3TechModel()) {
      currTokens = player.getResources().getQuantity(Constants.TECH_TOKENS);
    }
    final GameData data = getData();
    if (getAvailableTechs(player, data).isEmpty()) {
      if (isWW2V3TechModel()) {
        final Resource techTokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
        final String transcriptText = player.getName() + " No more available tech advances.";
        bridge.getHistoryWriter().startEvent(transcriptText);
        final Change removeTokens =
            ChangeFactory.changeResourcesChange(bridge.getPlayerId(), techTokens, -currTokens);
        bridge.addChange(removeTokens);
      }
      return new TechResults("No more available tech advances.");
    }
    final String annotation = player.getName() + " rolling for tech.";
    final int[] random;
    int techHits;
    int remainder = 0;
    final int diceSides = data.getDiceSides();
    if (BaseEditDelegate.getEditMode(data)) {
      final ITripleAPlayer tripleaPlayer = getRemotePlayer();
      random = tripleaPlayer.selectFixedDice(techRolls, diceSides, true, annotation, diceSides);
      techHits = getTechHits(random);
    } else if (isLowLuckTechOnly()) {
      techHits = techRolls / diceSides;
      remainder = techRolls % diceSides;
      if (remainder > 0) {
        random = bridge.getRandom(diceSides, 1, player, DiceType.TECH, annotation);
        if ((random[0] + 1) <= remainder) {
          techHits++;
        }
      } else {
        random = bridge.getRandom(diceSides, 1, player, DiceType.TECH, annotation);
        remainder = diceSides;
      }
    } else {
      random = bridge.getRandom(diceSides, techRolls, player, DiceType.TECH, annotation);
      techHits = getTechHits(random);
    }
    final boolean isRevisedModel = isWW2V2() || (isSelectableTechRoll() && !isWW2V3TechModel());
    final String directedTechInfo = isRevisedModel ? (" for " + techToRollFor.getTechs().get(0)) : "";
    final DiceRoll renderDice = (isLowLuckTechOnly() ? new DiceRoll(random, techHits, remainder, false)
        : new DiceRoll(random, techHits, diceSides - 1, true));
    bridge.getHistoryWriter()
        .startEvent(
            player.getName() + (random.length > 1 ? " roll " : " rolls : ") + MyFormatter.asDice(random)
                + directedTechInfo + " and gets " + techHits + " " + MyFormatter.pluralize("hit", techHits),
            renderDice);
    if (isWW2V3TechModel()
        && ((techHits > 0) || Properties.getRemoveAllTechTokensAtEndOfTurn(data))) {
      techCategory = techToRollFor;
      // remove all the tokens
      final Resource techTokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
      final String transcriptText = player.getName() + " removing all Technology Tokens after "
          + (techHits > 0 ? "successful" : "unsuccessful") + " research.";
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change removeTokens =
          ChangeFactory.changeResourcesChange(bridge.getPlayerId(), techTokens, -currTokens);
      bridge.addChange(removeTokens);
    }
    final Collection<TechAdvance> advances;
    if (isRevisedModel) {
      if (techHits > 0) {
        advances = Collections.singletonList(techToRollFor.getTechs().get(0));
      } else {
        advances = Collections.emptyList();
      }
    } else {
      advances = getTechAdvances(techHits);
    }
    // Put in techs so they can be activated later.
    techs.put(player, advances);
    final List<String> advancesAsString = new ArrayList<>();
    int count = advances.size();
    final StringBuilder text = new StringBuilder();
    for (final TechAdvance advance : advances) {
      text.append(advance.getName());
      count--;
      advancesAsString.add(advance.getName());
      if (count > 1) {
        text.append(", ");
      }
      if (count == 1) {
        text.append(" and ");
      }
    }
    final String transcriptText = player.getName() + " discover " + text;
    if (advances.size() > 0) {
      bridge.getHistoryWriter().startEvent(transcriptText);
      // play a sound
      getSoundChannel().playSoundForAll(SoundPath.CLIP_TECHNOLOGY_SUCCESSFUL, player);
    } else {
      getSoundChannel().playSoundForAll(SoundPath.CLIP_TECHNOLOGY_FAILURE, player);
    }
    return new TechResults(random, remainder, techHits, advancesAsString, player);
  }

  boolean checkEnoughMoney(final int rolls, final IntegerMap<PlayerID> whoPaysHowMuch) {
    final Resource pus = getData().getResourceList().getResource(Constants.PUS);
    final int cost = rolls * getTechCost();
    if ((whoPaysHowMuch == null) || whoPaysHowMuch.isEmpty()) {
      final int has = bridge.getPlayerId().getResources().getQuantity(pus);
      return has >= cost;
    }

    int runningTotal = 0;
    for (final Entry<PlayerID, Integer> entry : whoPaysHowMuch.entrySet()) {
      final int has = entry.getKey().getResources().getQuantity(pus);
      final int paying = entry.getValue();
      if (paying > has) {
        return false;
      }
      runningTotal += paying;
    }
    return runningTotal >= cost;
  }

  private void chargeForTechRolls(final int rolls, final IntegerMap<PlayerID> whoPaysHowMuch) {
    final Resource pus = getData().getResourceList().getResource(Constants.PUS);
    int cost = rolls * getTechCost();
    if ((whoPaysHowMuch == null) || whoPaysHowMuch.isEmpty()) {
      final String transcriptText = bridge.getPlayerId().getName() + " spend " + cost + " on tech rolls";
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change charge = ChangeFactory.changeResourcesChange(bridge.getPlayerId(), pus, -cost);
      bridge.addChange(charge);
    } else {
      for (final Entry<PlayerID, Integer> entry : whoPaysHowMuch.entrySet()) {
        final PlayerID p = entry.getKey();
        final int pays = Math.min(cost, entry.getValue());
        if (pays <= 0) {
          continue;
        }
        cost -= pays;
        final String transcriptText = p.getName() + " spend " + pays + " on tech rolls";
        bridge.getHistoryWriter().startEvent(transcriptText);
        final Change charge = ChangeFactory.changeResourcesChange(p, pus, -pays);
        bridge.addChange(charge);
      }
    }
    if (isWW2V3TechModel()) {
      final Resource tokens = getData().getResourceList().getResource(Constants.TECH_TOKENS);
      final Change newTokens = ChangeFactory.changeResourcesChange(bridge.getPlayerId(), tokens, rolls);
      bridge.addChange(newTokens);
    }
  }

  private int getTechHits(final int[] random) {
    int count = 0;
    for (final int element : random) {
      if (element == (getData().getDiceSides() - 1)) {
        count++;
      }
    }
    return count;
  }

  private Collection<TechAdvance> getTechAdvances(int hits) {
    final List<TechAdvance> available;
    if ((hits > 0) && isWW2V3TechModel()) {
      available = getAvailableAdvancesForCategory(techCategory);
      hits = 1;
    } else {
      available = getAvailableAdvances();
    }
    if (available.isEmpty()) {
      return Collections.emptyList();
    }
    if (hits >= available.size()) {
      return available;
    }
    if (hits == 0) {
      return Collections.emptyList();
    }
    final Collection<TechAdvance> newAdvances = new ArrayList<>(hits);
    final String annotation = player.getName() + " rolling to see what tech advances are aquired";
    final int[] random;
    if (isSelectableTechRoll() || BaseEditDelegate.getEditMode(getData())) {
      final ITripleAPlayer tripleaPlayer = getRemotePlayer();
      random = tripleaPlayer.selectFixedDice(hits, 0, true, annotation, available.size());
    } else {
      random = new int[hits];
      final List<Integer> rolled = new ArrayList<>();
      // generating discrete rolls. messy, can't think of a more elegant way
      // hits guaranteed to be less than available at this point.
      for (int i = 0; i < hits; i++) {
        int roll = bridge.getRandom(available.size() - i, null, DiceType.ENGINE, annotation);
        for (final int r : rolled) {
          if (roll >= r) {
            roll++;
          }
        }
        random[i] = roll;
        rolled.add(roll);
      }
    }
    final List<Integer> rolled = new ArrayList<>();
    for (final int element : random) {
      final int index = element;
      // check in case of dice chooser.
      if (!rolled.contains(index) && (index < available.size())) {
        newAdvances.add(available.get(index));
        rolled.add(index);
      }
    }
    bridge.getHistoryWriter().startEvent("Rolls to resolve tech hits:" + MyFormatter.asDice(random));
    return newAdvances;
  }

  private List<TechAdvance> getAvailableAdvances() {
    return getAvailableTechs(bridge.getPlayerId(), getData());
  }

  public static List<TechAdvance> getAvailableTechs(final PlayerID player, final GameData data) {
    final Collection<TechAdvance> currentAdvances = TechTracker.getCurrentTechAdvances(player, data);
    final Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(data, player);
    return CollectionUtils.difference(allAdvances, currentAdvances);
  }

  private List<TechAdvance> getAvailableAdvancesForCategory(final TechnologyFrontier techCategory) {
    final Collection<TechAdvance> playersAdvances =
        TechTracker.getCurrentTechAdvances(bridge.getPlayerId(), getData());
    final List<TechAdvance> available = CollectionUtils.difference(techCategory.getTechs(), playersAdvances);
    return available;
  }

  public int getTechCost() {
    techCost = TechTracker.getTechCost(player);
    return techCost;
  }

  @Override
  public Class<ITechDelegate> getRemoteType() {
    return ITechDelegate.class;
  }
}
