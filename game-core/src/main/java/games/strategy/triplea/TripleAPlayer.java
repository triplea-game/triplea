package games.strategy.triplea;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import javax.swing.ButtonModel;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.net.GUID;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.AbstractHumanPlayer;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.PlaceData;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * As a rule, nothing that changes GameData should be in here.
 * It should be using a Change done in a delegate, and done through an IDelegate, which we get through
 * getPlayerBridge().getRemote()
 */
public class TripleAPlayer extends AbstractHumanPlayer<TripleAFrame> implements ITripleAPlayer {
  private boolean soundPlayedAlreadyCombatMove = false;
  private boolean soundPlayedAlreadyNonCombatMove = false;
  private boolean soundPlayedAlreadyPurchase = false;
  private boolean soundPlayedAlreadyTechnology = false;
  private boolean soundPlayedAlreadyBattle = false;
  private boolean soundPlayedAlreadyEndTurn = false;
  private boolean soundPlayedAlreadyPlacement = false;

  /** Creates new TripleAPlayer. */
  public TripleAPlayer(final String name, final String type) {
    super(name, type);
  }

  @Override
  public void reportError(final String error) {
    ui.notifyError(error);
  }

  @Override
  public void reportMessage(final String message, final String title) {
    if (ui != null) {
      ui.notifyMessage(message, title);
    }
  }

  @Override
  public void start(final String name) {
    // must call super.start
    super.start(name);
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    if (ui == null) {
      // We will get here if we are loading a save game of a map that we do not have. Caller code should be doing
      // the error handling, so just return..
      return;
    }
    // TODO: parsing which UI thing we should run based on the string name of a possibly extended delegate
    // class seems like a bad way of doing this whole method. however i can't think of anything better right now.
    // This is how we find out our game step: getGameData().getSequence().getStep()
    // The game step contains information, like the exact delegate and the delegate's class,
    // that we can further use if needed. This is how we get our communication bridge for affecting the gamedata:
    // (ISomeDelegate) getPlayerBridge().getRemote()
    // We should never touch the game data directly. All changes to game data are done through the remote,
    // which then changes the game using the DelegateBridge -> change factory
    ui.requiredTurnSeries(getPlayerId());
    enableEditModeMenu();
    boolean badStep = false;
    if (name.endsWith("Tech")) {
      tech();
    } else if (name.endsWith("TechActivation")) {
      // do nothing
    } else if (name.endsWith("Bid") || name.endsWith("Purchase")) { // the delegate handles everything
      purchase(GameStepPropertiesHelper.isBid(getGameData()));
    } else if (name.endsWith("Move")) {
      final boolean nonCombat = GameStepPropertiesHelper.isNonCombatMove(getGameData(), false);
      move(nonCombat, name);
      if (!nonCombat) {
        ui.waitForMoveForumPoster(getPlayerId(), getPlayerBridge());
        // TODO only do forum post if there is a combat
      }
    } else if (name.endsWith("Battle")) {
      battle();
    } else if (name.endsWith("Place")) {
      place();
    } else if (name.endsWith("Politics")) {
      politics(true);
    } else if (name.endsWith("UserActions")) {
      userActions(true);
    } else if (name.endsWith("EndTurn")) {
      endTurn();
      // reset our sounds
      soundPlayedAlreadyCombatMove = false;
      soundPlayedAlreadyNonCombatMove = false;
      soundPlayedAlreadyPurchase = false;
      soundPlayedAlreadyTechnology = false;
      soundPlayedAlreadyBattle = false;
      soundPlayedAlreadyEndTurn = false;
      soundPlayedAlreadyPlacement = false;
    } else {
      badStep = true;
    }
    disableEditModeMenu();
    if (badStep) {
      throw new IllegalArgumentException("Unrecognized step name:" + name);
    }
  }

  private void enableEditModeMenu() {
    try {
      ui.setEditDelegate((IEditDelegate) getPlayerBridge().getRemotePersistentDelegate("edit"));
    } catch (final Exception e) {
      ClientLogger.logQuietly("Failed to set edit delegate", e);
    }
    SwingUtilities.invokeLater(() -> {
      ui.getEditModeButtonModel().addActionListener(editModeAction);
      ui.getEditModeButtonModel().setEnabled(true);
    });
  }

  private void disableEditModeMenu() {
    ui.setEditDelegate(null);
    SwingUtilities.invokeLater(() -> {
      ui.getEditModeButtonModel().setEnabled(false);
      ui.getEditModeButtonModel().removeActionListener(editModeAction);
    });
  }

  private final ActionListener editModeAction = e -> {
    final boolean editMode = ((ButtonModel) e.getSource()).isSelected();
    try {
      // Set edit mode
      // All GameDataChangeListeners will be notified upon success
      final IEditDelegate editDelegate = (IEditDelegate) getPlayerBridge().getRemotePersistentDelegate("edit");
      editDelegate.setEditMode(editMode);
    } catch (final Exception exception) {
      exception.printStackTrace();
      // toggle back to previous state since setEditMode failed
      ui.getEditModeButtonModel().setSelected(!ui.getEditModeButtonModel().isSelected());
    }

  };

  private void politics(final boolean firstRun) {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final IPoliticsDelegate politicsDelegate;
    try {
      politicsDelegate = (IPoliticsDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }

    final PoliticalActionAttachment actionChoice =
        ui.getPoliticalActionChoice(getPlayerId(), firstRun, politicsDelegate);
    if (actionChoice != null) {
      politicsDelegate.attemptAction(actionChoice);
      politics(false);
    }
  }

  private void userActions(final boolean firstRun) {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final IUserActionDelegate userActionDelegate;
    try {
      userActionDelegate = (IUserActionDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    final UserActionAttachment actionChoice = ui.getUserActionChoice(getPlayerId(), firstRun, userActionDelegate);
    if (actionChoice != null) {
      userActionDelegate.attemptAction(actionChoice);
      userActions(false);
    }
  }

  @Override
  public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion,
      final boolean politics) {
    final GameData data = getGameData();
    if (!getPlayerId().amNotDeadYet(data) || getPlayerBridge().isGameOver()) {
      return true;
    }
    return ui.acceptAction(playerSendingProposal, "To " + getPlayerId().getName() + ": " + acceptanceQuestion,
        politics);
  }

  private void tech() {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final ITechDelegate techDelegate;
    try {
      techDelegate = (ITechDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }


    final PlayerID id = getPlayerId();
    if (!soundPlayedAlreadyTechnology) {
      ClipPlayer.play(SoundPath.CLIP_PHASE_TECHNOLOGY, id);
      soundPlayedAlreadyTechnology = true;
    }
    final TechRoll techRoll = ui.getTechRolls(id);
    if (techRoll != null) {
      final TechResults techResults = techDelegate.rollTech(techRoll.getRolls(), techRoll.getTech(),
          techRoll.getNewTokens(), techRoll.getWhoPaysHowMuch());
      if (techResults.isError()) {
        ui.notifyError(techResults.getErrorString());
        tech();
      } else {
        ui.notifyTechResults(techResults);
      }
    }
  }

  private void move(final boolean nonCombat, final String stepName) {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final IMoveDelegate moveDel;
    try {
      moveDel = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }

    final PlayerID id = getPlayerId();

    if (nonCombat && !soundPlayedAlreadyNonCombatMove) {
      ClipPlayer.play(SoundPath.CLIP_PHASE_MOVE_NONCOMBAT, id);
      soundPlayedAlreadyNonCombatMove = true;
    }

    if (!nonCombat && !soundPlayedAlreadyCombatMove) {
      ClipPlayer.play(SoundPath.CLIP_PHASE_MOVE_COMBAT, id);
      soundPlayedAlreadyCombatMove = true;
    }
    // getMove will block until all moves are done. We recursively call this same method
    // until getMove stops blocking.
    final MoveDescription moveDescription = ui.getMove(id, getPlayerBridge(), nonCombat, stepName);
    if (moveDescription == null) {
      if (GameStepPropertiesHelper.isRemoveAirThatCanNotLand(getGameData())) {
        if (!canAirLand(true, id)) {
          // continue with the move loop
          move(nonCombat, stepName);
        }
      }
      if (!nonCombat) {
        if (canUnitsFight()) {
          move(nonCombat, stepName);
        }
      }
      return;
    }
    final String error = moveDel.move(moveDescription.getUnits(), moveDescription.getRoute(),
        moveDescription.getTransportsThatCanBeLoaded(), moveDescription.getDependentUnits());
    if (error != null) {
      ui.notifyError(error);
    }
    move(nonCombat, stepName);
  }

  private boolean canAirLand(final boolean movePhase, final PlayerID player) {
    final Collection<Territory> airCantLand;
    try {
      if (movePhase) {
        airCantLand = ((IMoveDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereAirCantLand(player);
      } else {
        airCantLand = ((IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereAirCantLand();
      }
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    return airCantLand.isEmpty() || ui.getOkToLetAirDie(getPlayerId(), airCantLand, movePhase);
  }

  private boolean canUnitsFight() {
    final Collection<Territory> unitsCantFight;
    try {
      unitsCantFight = ((IMoveDelegate) getPlayerBridge().getRemoteDelegate()).getTerritoriesWhereUnitsCantFight();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    return !(unitsCantFight.isEmpty() || ui.getOkToLetUnitsDie(unitsCantFight, true));
  }

  private void purchase(final boolean bid) {
    if (getPlayerBridge().isGameOver()) {
      return;
    }

    final PlayerID id = getPlayerId();
    // play a sound for this phase
    if (!bid && !soundPlayedAlreadyPurchase) {
      ClipPlayer.play(SoundPath.CLIP_PHASE_PURCHASE, id);
      soundPlayedAlreadyPurchase = true;
    }
    // Check if any factories need to be repaired
    if ((id.getRepairFrontier() != null) && (id.getRepairFrontier().getRules() != null)
        && !id.getRepairFrontier().getRules().isEmpty()) {
      final GameData data = getGameData();
      if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) {
        final Predicate<Unit> myDamaged = Matches.unitIsOwnedBy(id).and(Matches.unitHasTakenSomeBombingUnitDamage());
        final Collection<Unit> damagedUnits = new ArrayList<>();
        for (final Territory t : data.getMap().getTerritories()) {
          damagedUnits.addAll(CollectionUtils.getMatches(t.getUnits().getUnits(), myDamaged));
        }
        if (damagedUnits.size() > 0) {
          final HashMap<Unit, IntegerMap<RepairRule>> repair =
              ui.getRepair(id, bid, GameStepPropertiesHelper.getRepairPlayers(data, id));
          if (repair != null) {
            final IPurchaseDelegate purchaseDel;
            try {
              purchaseDel = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
            } catch (final ClassCastException e) {
              final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName()
                  + ", Remote class name: " + getPlayerBridge().getRemoteDelegate().getClass();
              // for some reason the client is not seeing or getting these errors, so print to err too
              System.err.println(errorContext);
              ClientLogger.logQuietly(errorContext, e);
              throw new IllegalStateException(errorContext, e);
            }
            final String error = purchaseDel.purchaseRepair(repair);
            if (error != null) {
              ui.notifyError(error);
              // dont give up, keep going
              purchase(bid);
            }
          }
        }
      }
    }
    final IntegerMap<ProductionRule> prod = ui.getProduction(id, bid);
    if (prod == null) {
      return;
    }
    final IPurchaseDelegate purchaseDel;
    try {
      purchaseDel = (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    final String purchaseError = purchaseDel.purchase(prod);
    if (purchaseError != null) {
      ui.notifyError(purchaseError);
      // dont give up, keep going
      purchase(bid);
    }
  }

  private void battle() {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final IBattleDelegate battleDel;
    try {
      battleDel = (IBattleDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      ClientLogger.logQuietly(errorContext, e); // TODO: this code is triplicated in the code..
      throw new IllegalStateException(errorContext, e);
    }

    final PlayerID id = getPlayerId();
    while (true) {
      if (getPlayerBridge().isGameOver()) {
        return;
      }
      final BattleListing battles = battleDel.getBattles();
      if (battles.isEmpty()) {
        final IBattle battle = battleDel.getCurrentBattle();
        if (battle != null) {
          // this should never happen, but it happened once....
          System.err.println("Current battle exists but is not on pending list:  " + battle.toString());
          battleDel.fightCurrentBattle();
        }
        return;
      }
      if (!soundPlayedAlreadyBattle) {
        ClipPlayer.play(SoundPath.CLIP_PHASE_BATTLE, id);
        soundPlayedAlreadyBattle = true;
      }
      final FightBattleDetails details = ui.getBattle(id, battles.getBattles());
      if (getPlayerBridge().isGameOver()) {
        return;
      }
      if (details != null) {
        final String error =
            battleDel.fightBattle(details.getWhere(), details.isBombingRaid(), details.getBattleType());
        if (error != null) {
          ui.notifyError(error);
        }
      }
    }
  }

  private void place() {
    final boolean bid = GameStepPropertiesHelper.isBid(getGameData());
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final PlayerID id = getPlayerId();
    final IAbstractPlaceDelegate placeDel;
    try {
      placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    while (true) {
      if (!soundPlayedAlreadyPlacement) {
        ClipPlayer.play(SoundPath.CLIP_PHASE_PLACEMENT, id);
        soundPlayedAlreadyPlacement = true;
      }
      final PlaceData placeData = ui.waitForPlace(id, bid, getPlayerBridge());
      if (placeData == null) {
        // this only happens in lhtr rules
        if (!GameStepPropertiesHelper.isRemoveAirThatCanNotLand(getGameData()) || canAirLand(false, id)
            || getPlayerBridge().isGameOver()) {
          return;
        }
        continue;
      }
      final String error = placeDel.placeUnits(placeData.getUnits(), placeData.getAt(),
          bid ? IAbstractPlaceDelegate.BidMode.BID : IAbstractPlaceDelegate.BidMode.NOT_BID);
      if (error != null) {
        ui.notifyError(error);
      }
    }
  }

  private void endTurn() {
    if (getPlayerBridge().isGameOver()) {
      return;
    }
    final GameData data = getGameData();
    // play a sound for this phase
    final IAbstractForumPosterDelegate endTurnDelegate;
    try {
      endTurnDelegate = (IAbstractForumPosterDelegate) getPlayerBridge().getRemoteDelegate();
    } catch (final ClassCastException e) {
      final String errorContext = "PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: "
          + getPlayerBridge().getRemoteDelegate().getClass();
      // for some reason the client is not seeing or getting these errors, so print to err too
      System.err.println(errorContext);
      ClientLogger.logQuietly(errorContext, e);
      throw new IllegalStateException(errorContext, e);
    }
    if (!soundPlayedAlreadyEndTurn && TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(getPlayerId(), data)) {
      // do not play if we are reloading a savegame from pbem (gets annoying)
      if (!endTurnDelegate.getHasPostedTurnSummary()) {
        ClipPlayer.play(SoundPath.CLIP_PHASE_END_TURN, getPlayerId());
      }
      soundPlayedAlreadyEndTurn = true;
    }
    ui.waitForEndTurn(getPlayerId(), getPlayerBridge());
  }

  @Override
  public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
      final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
      final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties, final GUID battleId, final Territory battlesite,
      final boolean allowMultipleHitsPerUnit) {
    return ui.getBattlePanel().getCasualties(selectFrom, dependents, count, message, dice, hit, defaultCasualties,
        battleId, allowMultipleHitsPerUnit);
  }

  @Override
  public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title,
      final int diceSides) {
    return ui.selectFixedDice(numDice, hitAt, hitOnlyIfEquals, title, diceSides);
  }

  @Override
  public Territory selectBombardingTerritory(final Unit unit, final Territory unitTerritory,
      final Collection<Territory> territories, final boolean noneAvailable) {
    return ui.getBattlePanel().getBombardment(unit, unitTerritory, territories, noneAvailable);
  }

  @Override
  public boolean selectAttackSubs(final Territory unitTerritory) {
    return ui.getBattlePanel().getAttackSubs(unitTerritory);
  }

  @Override
  public boolean selectAttackTransports(final Territory unitTerritory) {
    return ui.getBattlePanel().getAttackTransports(unitTerritory);
  }

  @Override
  public boolean selectAttackUnits(final Territory unitTerritory) {
    return ui.getBattlePanel().getAttackUnits(unitTerritory);
  }

  @Override
  public boolean selectShoreBombard(final Territory unitTerritory) {
    return ui.getBattlePanel().getShoreBombard(unitTerritory);
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return ui.getStrategicBombingRaid(territory);
  }

  @Override
  public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    return ui.getStrategicBombingRaidTarget(territory, potentialTargets, bombers);
  }

  @Override
  public Territory whereShouldRocketsAttack(final Collection<Territory> candidates, final Territory from) {
    return ui.getRocketAttack(candidates, from);
  }

  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved,
      final Territory from) {
    return ui.moveFightersToCarrier(fightersThatCanBeMoved, from);
  }

  @Override
  public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory,
      final String unitMessage) {
    return ui.selectTerritoryForAirToLand(candidates, currentTerritory, unitMessage);
  }

  @Override
  public boolean confirmMoveInFaceOfAa(final Collection<Territory> aaFiringTerritories) {
    final String question = "Your units will be fired on in: "
        + MyFormatter.defaultNamedToTextList(aaFiringTerritories, " and ", false) + ".  Do you still want to move?";
    return ui.getOk(question);
  }

  @Override
  public boolean confirmMoveKamikaze() {
    final String question = "Not all air units in destination territory can land, do you still want to move?";
    return ui.getOk(question);
  }

  @Override
  public boolean confirmMoveHariKari() {
    final String question = "All units in destination territory will automatically die, do you still want to move?";
    return ui.getOk(question);
  }

  @Override
  public Territory retreatQuery(final GUID battleId, final boolean submerge, final Territory battleTerritory,
      final Collection<Territory> possibleTerritories, final String message) {
    return ui.getBattlePanel().getRetreat(battleId, message, possibleTerritories, submerge);
  }

  @Override
  public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    return ui.scrambleUnitsQuery(scrambleTo, possibleScramblers);
  }

  @Override
  public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible,
      final String message) {
    return ui.selectUnitsQuery(current, possible, message);
  }

  @Override
  public void confirmEnemyCasualties(final GUID battleId, final String message, final PlayerID hitPlayer) {
    // no need, we have already confirmed since we are firing player
    if (ui.getLocalPlayers().playing(hitPlayer)) {
      return;
    }
    // we dont want to confirm enemy casualties
    if (!ClientSetting.CONFIRM_ENEMY_CASUALTIES.booleanValue()) {
      return;
    }
    ui.getBattlePanel().confirmCasualties(battleId, message);
  }

  @Override
  public void confirmOwnCasualties(final GUID battleId, final String message) {
    ui.getBattlePanel().confirmCasualties(battleId, message);
  }

  private static boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
  }

  @Override
  public HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack) {
    final PlayerID id = getPlayerId();
    final PlayerAttachment pa = PlayerAttachment.get(id);
    if (pa == null) {
      return null;
    }
    final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
    if (resourcesAndAttackValues.size() <= 0) {
      return null;
    }
    final IntegerMap<Resource> playerResourceCollection = id.getResources().getResourcesCopy();
    final IntegerMap<Resource> attackTokens = new IntegerMap<>();
    for (final Resource possible : resourcesAndAttackValues.keySet()) {
      final int amount = playerResourceCollection.getInt(possible);
      if (amount > 0) {
        attackTokens.put(possible, amount);
      }
    }
    if (attackTokens.size() <= 0) {
      return null;
    }
    final HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> kamikazeSuicideAttacks = new HashMap<>();
    for (final Entry<Resource, Integer> entry : attackTokens.entrySet()) {
      final Resource resource = entry.getKey();
      final int max = entry.getValue();
      final Map<Territory, IntegerMap<Unit>> selection =
          ui.selectKamikazeSuicideAttacks(possibleUnitsToAttack, resource, max);
      for (final Entry<Territory, IntegerMap<Unit>> selectionEntry : selection.entrySet()) {
        final Territory territory = selectionEntry.getKey();
        final HashMap<Unit, IntegerMap<Resource>> currentTerr =
            kamikazeSuicideAttacks.getOrDefault(territory, new HashMap<>());
        for (final Entry<Unit, Integer> unitEntry : selectionEntry.getValue().entrySet()) {
          final Unit unit = unitEntry.getKey();
          final IntegerMap<Resource> currentUnit = currentTerr.getOrDefault(unit, new IntegerMap<>());
          currentUnit.add(resource, unitEntry.getValue());
          currentTerr.put(unit, currentUnit);
        }
        kamikazeSuicideAttacks.put(territory, currentTerr);
      }
    }
    return kamikazeSuicideAttacks;
  }

  @Override
  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final List<Territory> territoryChoices,
      final List<Unit> unitChoices, final int unitsPerPick) {
    if ((territoryChoices == null) || territoryChoices.isEmpty() || (unitsPerPick < 1)) {
      return Tuple.of(null, new HashSet<>());
    }
    return ui.pickTerritoryAndUnits(this.getPlayerId(), territoryChoices, unitChoices, unitsPerPick);
  }
}
