package games.strategy.triplea.delegate;

import com.google.common.collect.ImmutableList;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.FireTriggerParams;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/**
 * Logic for dealing with player tech rolls. This class requires the TechActivationDelegate which
 * actually activates the tech.
 */
public class TechnologyDelegate extends BaseTripleADelegate implements ITechDelegate {
  private int techCost;
  private @Nullable Map<GamePlayer, Collection<TechAdvance>> techs;
  private TechnologyFrontier techCategory;
  private boolean needToInitialize = true;

  public TechnologyDelegate() {}

  @Override
  public void initialize(final String name, final String displayName) {
    super.initialize(name, displayName);
    techs = new HashMap<>();
    techCost = -1;
  }

  @Override
  public void start() {
    super.start();
    if (!needToInitialize) {
      return;
    }
    if (Properties.getTriggers(getData().getProperties())) {
      // First set up a match for what we want to have fire as a default in this delegate. List out
      // as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT
      // have 'when' set.
      final Predicate<TriggerAttachment> technologyDelegateTriggerMatch =
          AbstractTriggerAttachment.availableUses
              .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
              .and(TriggerAttachment.techAvailableMatch());
      // get all possible triggers based on this match.
      final Set<TriggerAttachment> toFirePossible =
          TriggerAttachment.collectForAllTriggersMatching(
              Set.of(player), technologyDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final Map<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final List<TriggerAttachment> toFireTestedAndSatisfied =
            CollectionUtils.getMatches(
                toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerAvailableTechChange(
            new HashSet<>(toFireTestedAndSatisfied),
            bridge,
            new FireTriggerParams(null, null, true, true, true, true));
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
    state.needToInitialize = needToInitialize;
    state.techs = techs;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final TechnologyExtendedDelegateState s = (TechnologyExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.needToInitialize;
    techs = s.techs;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!Properties.getTechDevelopment(getData().getProperties())) {
      return false;
    }
    if (!TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(player, getData().getMap())) {
      return false;
    }
    if (Properties.getWW2V3TechModel(getData().getProperties())) {
      final Resource techTokens =
          getData().getResourceList().getResource(Constants.TECH_TOKENS).orElse(null);
      if (techTokens != null && player.getResources().getQuantity(techTokens) > 0) {
        return true;
      }
    }
    final int techCost = TechTracker.getTechCost(player);
    int money = player.getResources().getQuantity(Constants.PUS);
    if (money < techCost) {
      final PlayerAttachment pa = PlayerAttachment.get(player);
      if (pa == null) {
        return false;
      }
      final Collection<GamePlayer> helpPay = pa.getHelpPayTechCost();
      if (helpPay.isEmpty()) {
        return false;
      }
      for (final GamePlayer p : helpPay) {
        money += p.getResources().getQuantity(Constants.PUS);
      }
      return money >= techCost;
    }
    return true;
  }

  public @Nullable Collection<TechAdvance> getAdvances(GamePlayer player) {
    return techs == null ? null : techs.get(player);
  }

  public void clearAdvances(GamePlayer player) {
    if (techs != null) {
      techs.remove(player);
    }
  }

  @Override
  public TechResults rollTech(
      final int techRolls,
      final TechnologyFrontier techToRollFor,
      final int newTokens,
      final IntegerMap<GamePlayer> whoPaysHowMuch) {
    int rollCount = techRolls;
    if (Properties.getWW2V3TechModel(getData().getProperties())) {
      rollCount = newTokens;
    }
    final boolean canPay = checkEnoughMoney(rollCount, whoPaysHowMuch);
    if (!canPay) {
      return new TechResults("Not enough money to pay for that many tech rolls.");
    }
    chargeForTechRolls(rollCount, whoPaysHowMuch);
    int currTokens = 0;
    if (Properties.getWW2V3TechModel(getData().getProperties())) {
      currTokens = player.getResources().getQuantity(Constants.TECH_TOKENS);
    }
    final GameData data = getData();
    if (getAvailableTechs(player, data.getTechnologyFrontier()).isEmpty()) {
      if (Properties.getWW2V3TechModel(getData().getProperties())) {
        final Resource techTokens =
            data.getResourceList().getResource(Constants.TECH_TOKENS).orElse(null);
        final String transcriptText = player.getName() + " No more available tech advances.";
        bridge.getHistoryWriter().startEvent(transcriptText);
        final Change removeTokens =
            ChangeFactory.changeResourcesChange(bridge.getGamePlayer(), techTokens, -currTokens);
        bridge.addChange(removeTokens);
      }
      return new TechResults("No more available tech advances.");
    }
    final String annotation = player.getName() + " rolling for tech.";
    final int[] random;
    int techHits;
    int remainder = 0;
    final int diceSides = data.getDiceSides();
    if (EditDelegate.getEditMode(data.getProperties())) {
      final Player tripleaPlayer = bridge.getRemotePlayer();
      random = tripleaPlayer.selectFixedDice(techRolls, diceSides, annotation, diceSides);
      techHits = getTechHits(random);
    } else if (Properties.getLowLuckTechOnly(getData().getProperties())) {
      techHits = techRolls / diceSides;
      remainder = techRolls % diceSides;
      random = bridge.getRandom(diceSides, 1, player, DiceType.TECH, annotation);
      if (remainder > 0) {
        if (random[0] + 1 <= remainder) {
          techHits++;
        }
      } else {
        remainder = diceSides;
      }
    } else {
      random = bridge.getRandom(diceSides, techRolls, player, DiceType.TECH, annotation);
      techHits = getTechHits(random);
    }
    final boolean isRevisedModel =
        Properties.getWW2V2(getData().getProperties())
            || (Properties.getSelectableTechRoll(getData().getProperties())
                && !Properties.getWW2V3TechModel(getData().getProperties()));
    final String directedTechInfo = isRevisedModel ? " for " + techToRollFor.getTechs().get(0) : "";
    final DiceRoll renderDice =
        (Properties.getLowLuckTechOnly(getData().getProperties())
            ? new DiceRoll(random, techHits, remainder, false, player.getName())
            : new DiceRoll(random, techHits, diceSides - 1, true, player.getName()));
    bridge
        .getHistoryWriter()
        .startEvent(
            player.getName()
                + (random.length > 1 ? " roll " : " rolls : ")
                + MyFormatter.asDice(random)
                + directedTechInfo
                + " and gets "
                + techHits
                + " "
                + MyFormatter.pluralize("hit", techHits),
            renderDice);
    if (Properties.getWW2V3TechModel(getData().getProperties())
        && (techHits > 0 || Properties.getRemoveAllTechTokensAtEndOfTurn(data.getProperties()))) {
      techCategory = techToRollFor;
      // remove all the tokens
      final Resource techTokens =
          data.getResourceList().getResource(Constants.TECH_TOKENS).orElse(null);
      final String transcriptText =
          player.getName()
              + " removing all Technology Tokens after "
              + (techHits > 0 ? "successful" : "unsuccessful")
              + " research.";
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change removeTokens =
          ChangeFactory.changeResourcesChange(bridge.getGamePlayer(), techTokens, -currTokens);
      bridge.addChange(removeTokens);
    }
    final Collection<TechAdvance> advances;
    if (isRevisedModel) {
      if (techHits > 0) {
        advances = List.of(techToRollFor.getTechs().get(0));
      } else {
        advances = List.of();
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
    if (!advances.isEmpty()) {
      bridge.getHistoryWriter().startEvent(transcriptText);
      // play a sound
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_TECHNOLOGY_SUCCESSFUL, player);
    } else {
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_TECHNOLOGY_FAILURE, player);
    }
    return new TechResults(random, remainder, techHits, advancesAsString);
  }

  boolean checkEnoughMoney(final int rolls, final IntegerMap<GamePlayer> whoPaysHowMuch) {
    final Resource pus = getData().getResourceList().getResource(Constants.PUS).orElse(null);
    final int cost = rolls * getTechCost();
    if (whoPaysHowMuch == null || whoPaysHowMuch.isEmpty()) {
      final int has = bridge.getGamePlayer().getResources().getQuantity(pus);
      return has >= cost;
    }

    int runningTotal = 0;
    for (final Entry<GamePlayer, Integer> entry : whoPaysHowMuch.entrySet()) {
      final int has = entry.getKey().getResources().getQuantity(pus);
      final int paying = entry.getValue();
      if (paying > has) {
        return false;
      }
      runningTotal += paying;
    }
    return runningTotal >= cost;
  }

  private void chargeForTechRolls(final int rolls, final IntegerMap<GamePlayer> whoPaysHowMuch) {
    final Resource pus = getData().getResourceList().getResource(Constants.PUS).orElse(null);
    int cost = rolls * getTechCost();
    if (whoPaysHowMuch == null || whoPaysHowMuch.isEmpty()) {
      final String transcriptText =
          bridge.getGamePlayer().getName() + " spend " + cost + " on tech rolls";
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change charge = ChangeFactory.changeResourcesChange(bridge.getGamePlayer(), pus, -cost);
      bridge.addChange(charge);
    } else {
      for (final Entry<GamePlayer, Integer> entry : whoPaysHowMuch.entrySet()) {
        final GamePlayer p = entry.getKey();
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
    if (Properties.getWW2V3TechModel(getData().getProperties())) {
      final Resource tokens =
          getData().getResourceList().getResource(Constants.TECH_TOKENS).orElse(null);
      final Change newTokens =
          ChangeFactory.changeResourcesChange(bridge.getGamePlayer(), tokens, rolls);
      bridge.addChange(newTokens);
    }
  }

  private int getTechHits(final int[] random) {
    int count = 0;
    for (final int element : random) {
      if (element == getData().getDiceSides() - 1) {
        count++;
      }
    }
    return count;
  }

  private Collection<TechAdvance> getTechAdvances(final int initialHits) {
    final List<TechAdvance> available;
    int hits = initialHits;
    if (hits > 0 && Properties.getWW2V3TechModel(getData().getProperties())) {
      available = getAvailableAdvancesForCategory(techCategory);
      hits = 1;
    } else {
      available = getAvailableAdvances();
    }
    if (available.isEmpty()) {
      return List.of();
    }
    if (hits >= available.size()) {
      return available;
    }
    if (hits == 0) {
      return List.of();
    }
    final Collection<TechAdvance> newAdvances = new ArrayList<>(hits);
    final String annotation = player.getName() + " rolling to see what tech advances are acquired";
    final int[] random;
    if (Properties.getSelectableTechRoll(getData().getProperties())
        || EditDelegate.getEditMode(getData().getProperties())) {
      final Player tripleaPlayer = bridge.getRemotePlayer();
      random = tripleaPlayer.selectFixedDice(hits, 0, annotation, available.size());
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
      // check in case of dice chooser.
      if (!rolled.contains(element) && element < available.size()) {
        newAdvances.add(available.get(element));
        rolled.add(element);
      }
    }
    bridge
        .getHistoryWriter()
        .startEvent("Rolls to resolve tech hits: " + MyFormatter.asDice(random));
    return ImmutableList.copyOf(newAdvances);
  }

  private List<TechAdvance> getAvailableAdvances() {
    return getAvailableTechs(bridge.getGamePlayer(), getData().getTechnologyFrontier());
  }

  public static List<TechAdvance> getAvailableTechs(
      final GamePlayer player, final TechnologyFrontier technologyFrontier) {
    final Collection<TechAdvance> currentAdvances =
        TechTracker.getCurrentTechAdvances(player, technologyFrontier);
    final Collection<TechAdvance> allAdvances =
        TechAdvance.getTechAdvances(technologyFrontier, player);
    return CollectionUtils.difference(allAdvances, currentAdvances);
  }

  private List<TechAdvance> getAvailableAdvancesForCategory(final TechnologyFrontier techCategory) {
    final Collection<TechAdvance> playersAdvances =
        TechTracker.getCurrentTechAdvances(
            bridge.getGamePlayer(), getData().getTechnologyFrontier());
    return CollectionUtils.difference(techCategory.getTechs(), playersAdvances);
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
