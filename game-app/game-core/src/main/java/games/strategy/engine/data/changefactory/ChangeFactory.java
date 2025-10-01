package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeAttachmentChange;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHolder;
import games.strategy.engine.data.changefactory.units.BombingUnitDamageChange;
import games.strategy.engine.data.changefactory.units.UnitDamageReceivedChange;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.data.BattleRecords;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.triplea.java.collections.IntegerMap;

/**
 * All changes made to GameData should be made through changes produced here.
 *
 * <p>The way to change game data is to
 *
 * <ol>
 *   <li>Create a change with a ChangeFactory.change** or ChangeFactory.set** method
 *   <li>Execute that change through DelegateBridge.addChange()
 * </ol>
 *
 * <p>In this way changes to the game data can be co-ordinated across the network.
 */
public class ChangeFactory {
  public static final Change EMPTY_CHANGE =
      new Change() {
        private static final long serialVersionUID = -5514560889478876641L;

        @Override
        protected void perform(final GameState data) {}

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

  private ChangeFactory() {}

  public static Change changeOwner(final Territory territory, final GamePlayer owner) {
    return new OwnerChange(territory, owner);
  }

  public static Change changeOwner(
      final Collection<Unit> units, final GamePlayer owner, final Territory location) {
    return new PlayerOwnerChange(units, owner, location);
  }

  public static Change addUnits(final UnitHolder holder, final Collection<Unit> units) {
    return new AddUnits(holder.getUnitCollection(), units);
  }

  public static Change removeUnits(final UnitHolder holder, final Collection<Unit> units) {
    return new RemoveUnits(holder.getUnitCollection(), units);
  }

  public static Change moveUnits(
      final Territory start, final Territory end, final Collection<Unit> units) {
    return new CompositeChange(List.of(removeUnits(start, units), addUnits(end, units)));
  }

  public static Change changeProductionFrontier(
      final GamePlayer player, final ProductionFrontier frontier) {
    return new ProductionFrontierChange(frontier, player);
  }

  public static Change changePlayerWhoAmIChange(
      final GamePlayer player, final String encodedPlayerTypeAndName) {
    return new PlayerWhoAmIChange(encodedPlayerTypeAndName, player);
  }

  public static Change changeResourcesChange(
      final GamePlayer player, final Resource resource, final int quantity) {
    return new ChangeResourceChange(player, resource, quantity);
  }

  public static Change removeResourceCollection(
      final GamePlayer gamePlayer, final ResourceCollection resourceCollection) {
    final CompositeChange compositeChange = new CompositeChange();
    for (final Resource r : resourceCollection.getResourcesCopy().keySet()) {
      compositeChange.add(
          new ChangeResourceChange(gamePlayer, r, -resourceCollection.getQuantity(r)));
    }
    return compositeChange;
  }

  public static Change setProperty(
      final String property, final Object value, final GameState data) {
    return new SetPropertyChange(property, value, data.getProperties());
  }

  /** Must already include existing damage to the unit. This does not add damage, it sets damage. */
  public static Change unitsHit(
      final IntegerMap<Unit> newHits, final Collection<Territory> territoriesToNotify) {
    return new UnitDamageReceivedChange(newHits, territoriesToNotify);
  }

  /** Must already include existing damage to the unit. This does not add damage, it sets damage. */
  public static Change bombingUnitDamage(
      final IntegerMap<Unit> newDamage, final Collection<Territory> territoriesToNotify) {
    return new BombingUnitDamageChange(newDamage, territoriesToNotify);
  }

  public static Change addProductionRule(
      final ProductionRule rule, final ProductionFrontier frontier) {
    return new AddProductionRule(rule, frontier);
  }

  public static Change removeProductionRule(
      final ProductionRule rule, final ProductionFrontier frontier) {
    return new RemoveProductionRule(rule, frontier);
  }

  public static Change addAvailableTech(
      final TechnologyFrontier tf, final TechAdvance ta, final GamePlayer player) {
    return new AddAvailableTech(tf, ta, player);
  }

  public static Change removeAvailableTech(
      final TechnologyFrontier tf, final TechAdvance ta, final GamePlayer player) {
    return new RemoveAvailableTech(tf, ta, player);
  }

  public static Change attachmentPropertyChange(
      final IAttachment attachment, final Object newValue, final String property) {
    return new ChangeAttachmentChange(attachment, newValue, property);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the
   * setting method is actually adding things to a list rather than overwriting.
   */
  public static Change attachmentPropertyChange(
      final IAttachment attachment,
      final Object newValue,
      final String property,
      final boolean resetFirst) {
    return new ChangeAttachmentChange(attachment, newValue, property, resetFirst);
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the
   * setting method is actually adding things to a list rather than overwriting.
   */
  public static Change attachmentPropertyReset(
      final IAttachment attachment, final String property) {
    return new AttachmentPropertyReset(attachment, property);
  }

  public static Change genericTechChange(
      final TechAttachment attachment, final boolean value, final String property) {
    return new GenericTechChange(attachment, value, property);
  }

  public static Change unitPropertyChange(
      final Unit unit, final Object newValue, final String propertyName) {
    return new ObjectPropertyChange(unit, propertyName, newValue);
  }

  public static Change addBattleRecords(final BattleRecords records, final GameState data) {
    return new AddBattleRecordsChange(records, data);
  }

  /**
   * Creates a change of relationshipType between 2 players, for example: change Germany-France
   * relationship from neutral to war.
   *
   * @return the Change of relationship between 2 players
   */
  public static Change relationshipChange(
      final GamePlayer player,
      final GamePlayer player2,
      final RelationshipType currentRelation,
      final RelationshipType newRelation) {
    return new RelationshipChange(player, player2, currentRelation, newRelation);
  }

  /**
   * Mark units as having no movement (less than 0).
   *
   * @param units referring units
   * @return change that contains marking of units as having no movement
   */
  public static Change markNoMovementChange(final Collection<Unit> units) {
    final CompositeChange change = new CompositeChange();
    for (final Unit unit : units) {
      if (unit.getMovementLeft().compareTo(BigDecimal.ZERO) >= 0) {
        change.add(markNoMovementChange(unit));
      }
    }
    if (change.isEmpty()) {
      return EMPTY_CHANGE;
    }
    return change;
  }

  public static Change markNoMovementChange(final Unit unit) {
    return unitPropertyChange(
        unit, new BigDecimal(unit.getMaxMovementAllowed() + 1), Unit.PropertyName.ALREADY_MOVED);
  }
}
