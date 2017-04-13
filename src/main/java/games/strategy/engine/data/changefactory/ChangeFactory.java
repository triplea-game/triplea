package games.strategy.engine.data.changefactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.BombingUnitDamageChange;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.util.IntegerMap;

/**
 * All changes made to GameData should be made through changes produced here.
 *
 * <p>
 * The way to change game data is to
 * </p>
 *
 * <ol>
 * <li>Create a change with a ChangeFactory.change** or ChangeFactory.set** method</li>
 * <li>Execute that change through DelegateBridge.addChange()</li>
 * </ol>
 *
 * <p>
 * In this way changes to the game data can be co-ordinated across the network.
 * </p>
 */
public class ChangeFactory {
  public static final Change EMPTY_CHANGE = new Change() {
    private static final long serialVersionUID = -5514560889478876641L;

    @Override
    protected void perform(final GameData data) {}

    @Override
    public Change invert() {
      return this;
    }

    // when de-serializing, always return the singleton
    private Object readResolve() {
      return ChangeFactory.EMPTY_CHANGE;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }
  };

  public static Change changeOwner(final Territory territory, final PlayerID owner) {
    return new OwnerChange(territory, owner);
  }

  public static Change changeOwner(final Collection<Unit> units, final PlayerID owner, final Territory location) {
    return new PlayerOwnerChange(units, owner, location);
  }

  public static Change changeOwner(final Unit unit, final PlayerID owner, final Territory location) {
    final ArrayList<Unit> list = new ArrayList<>(1);
    list.add(unit);
    return new PlayerOwnerChange(list, owner, location);
  }

  public static Change addUnits(final Territory territory, final Collection<Unit> units) {
    return new AddUnits(territory.getUnits(), units);
  }

  public static Change removeUnits(final Territory territory, final Collection<Unit> units) {
    return new RemoveUnits(territory.getUnits(), units);
  }

  public static Change addUnits(final PlayerID player, final Collection<Unit> units) {
    return new AddUnits(player.getUnits(), units);
  }

  public static Change removeUnits(final PlayerID player, final Collection<Unit> units) {
    return new RemoveUnits(player.getUnits(), units);
  }

  public static Change moveUnits(final Territory start, final Territory end, Collection<Unit> units) {
    units = new ArrayList<>(units);
    final List<Change> changes = new ArrayList<>(2);
    changes.add(removeUnits(start, units));
    changes.add(addUnits(end, units));
    return new CompositeChange(changes);
  }

  public static Change changeProductionFrontier(final PlayerID player, final ProductionFrontier frontier) {
    return new ProductionFrontierChange(frontier, player);
  }

  public static Change changePlayerWhoAmIChange(final PlayerID player, final String humanOrAI_colon_playerName) {
    return new PlayerWhoAmIChange(humanOrAI_colon_playerName, player);
  }

  public static Change changeResourcesChange(final PlayerID player, final Resource resource, final int quantity) {
    return new ChangeResourceChange(player, resource, quantity);
  }

  public static Change removeResourceCollection(final PlayerID id, final ResourceCollection rCollection) {
    final CompositeChange cChange = new CompositeChange();
    for (final Resource r : rCollection.getResourcesCopy().keySet()) {
      cChange.add(new ChangeResourceChange(id, r, -rCollection.getQuantity(r)));
    }
    return cChange;
  }

  public static Change setProperty(final String property, final Object value, final GameData data) {
    return new SetPropertyChange(property, value, data.getProperties());
  }

  /**
   * Must already include existing damage to the unit. This does not add damage, it sets damage.
   */
  public static Change unitsHit(final IntegerMap<Unit> newHits) {
    return new UnitHitsChange(newHits);
  }

  /**
   * Must already include existing damage to the unit. This does not add damage, it sets damage.
   */
  public static Change bombingUnitDamage(final IntegerMap<Unit> newDamage) {
    return new BombingUnitDamageChange(newDamage);
  }

  public static Change addProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    return new AddProductionRule(rule, frontier);
  }

  public static Change removeProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    return new RemoveProductionRule(rule, frontier);
  }

  public static Change addAvailableTech(final TechnologyFrontier tf, final TechAdvance ta, final PlayerID player) {
    return new AddAvailableTech(tf, ta, player);
  }

  public static Change removeAvailableTech(final TechnologyFrontier tf, final TechAdvance ta, final PlayerID player) {
    return new RemoveAvailableTech(tf, ta, player);
  }

  public static Change attachmentPropertyChange(final IAttachment attachment, final Object newValue,
      final String property) {
    return new ChangeAttachmentChange(attachment, newValue, property);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public static Change attachmentPropertyChange(final IAttachment attachment, final Object newValue,
      final String property, final boolean resetFirst) {
    return new ChangeAttachmentChange(attachment, newValue, property, resetFirst);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public static Change attachmentPropertyChange(final Attachable attachment, final String attachmentName,
      final Object newValue, final Object oldValue, final String property, final boolean clearFirst) {
    return new ChangeAttachmentChange(attachment, attachmentName, newValue, oldValue, property, clearFirst);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public static Change attachmentPropertyReset(final IAttachment attachment, final String property) {
    return new AttachmentPropertyReset(attachment, property);
  }

  public static Change genericTechChange(final TechAttachment attachment, final boolean value, final String property) {
    return new GenericTechChange(attachment, value, property);
  }

  public static Change unitPropertyChange(final Unit unit, final Object newValue, final String propertyName) {
    return new ObjectPropertyChange(unit, propertyName, newValue);
  }

  public static Change addBattleRecords(final BattleRecords records, final GameData data) {
    return new AddBattleRecordsChange(records, data);
  }

  /** Creates new ChangeFactory. No need */
  private ChangeFactory() {}

  /**
   * Creates a change of relationshipType between 2 players, for example: change Germany-France relationship from
   * neutral to war.
   *
   * @return the Change of relationship between 2 players
   */
  public static Change relationshipChange(final PlayerID player, final PlayerID player2,
      final RelationshipType currentRelation, final RelationshipType newRelation) {
    return new RelationshipChange(player, player2, currentRelation, newRelation);
  }

  /**
   * Mark units as having no movement.
   *
   * @param units
   *        referring units
   * @return change that contains marking of units as having no movement
   */
  public static Change markNoMovementChange(final Collection<Unit> units) {
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      if (TripleAUnit.get(unit).getMovementLeft() > 0) {
        change.add(markNoMovementChange(unit));
      }
    }
    if (change.isEmpty()) {
      return EMPTY_CHANGE;
    }
    return change;
  }

  public static Change markNoMovementChange(final Unit unit) {
    return unitPropertyChange(unit, TripleAUnit.get(unit).getMaxMovementAllowed(), TripleAUnit.ALREADY_MOVED);
  }
}


