/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/**
 * ChangeFactory.java
 * 
 * Created on October 25, 2001, 1:26 PM
 */
package games.strategy.engine.data;

import games.strategy.engine.data.properties.GameProperties;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.PropertyUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * All changes made to GameData should be made through changes produced here. <br>
 * The way to change game data is to <br>
 * 1) Create a change with a ChangeFactory.change** or ChangeFactory.set**
 * method <br>
 * 2) Execute that change through DelegateBridge.addChange()).
 * <p>
 * In this way changes to the game data can be co-ordinated across the network.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class ChangeFactory
{
	public static final Change EMPTY_CHANGE = new Change()
	{
		private static final long serialVersionUID = -5514560889478876641L;
		
		@Override
		protected void perform(final GameData data)
		{
		}
		
		@Override
		public Change invert()
		{
			return this;
		}
		
		// when de-serializing, always return the singleton
		private Object readResolve()
		{
			return ChangeFactory.EMPTY_CHANGE;
		}
		
		@Override
		public boolean isEmpty()
		{
			return true;
		}
	};
	
	public static Change changeOwner(final Territory territory, final PlayerID owner)
	{
		return new OwnerChange(territory, owner);
	}
	
	public static Change changeOwner(final Collection<Unit> units, final PlayerID owner, final Territory location)
	{
		return new PlayerOwnerChange(units, owner, location);
	}
	
	public static Change changeOwner(final Unit unit, final PlayerID owner, final Territory location)
	{
		final ArrayList<Unit> list = new ArrayList<Unit>(1);
		list.add(unit);
		return new PlayerOwnerChange(list, owner, location);
	}
	
	public static Change changeUnitProduction(final Territory terr, final int value)
	{
		return new ChangeUnitProduction(terr, value);
	}
	
	public static Change addUnits(final Territory territory, final Collection<Unit> units)
	{
		return new AddUnits(territory.getUnits(), units);
	}
	
	public static Change removeUnits(final Territory territory, final Collection<Unit> units)
	{
		return new RemoveUnits(territory.getUnits(), units);
	}
	
	public static Change addUnits(final PlayerID player, final Collection<Unit> units)
	{
		return new AddUnits(player.getUnits(), units);
	}
	
	public static Change removeUnits(final PlayerID player, final Collection<Unit> units)
	{
		return new RemoveUnits(player.getUnits(), units);
	}
	
	public static Change moveUnits(final Territory start, final Territory end, Collection<Unit> units)
	{
		units = new ArrayList<Unit>(units);
		final List<Change> changes = new ArrayList<Change>(2);
		changes.add(removeUnits(start, units));
		changes.add(addUnits(end, units));
		return new CompositeChange(changes);
	}
	
	public static Change changeProductionFrontier(final PlayerID player, final ProductionFrontier frontier)
	{
		return new ProductionFrontierChange(frontier, player);
	}
	
	public static Change changeProductionFrontierChange(final PlayerID player, final ProductionFrontier newFrontier)
	{
		return new ProductionFrontierChange(newFrontier, player);
	}
	
	public static Change changePlayerWhoAmIChange(final PlayerID player, final String humanOrAI_colon_playerName)
	{
		return new PlayerWhoAmIChange(humanOrAI_colon_playerName, player);
	}
	
	public static Change changeResourcesChange(final PlayerID player, final Resource resource, final int quantity)
	{
		return new ChangeResourceChange(player, resource, quantity);
	}
	
	public static Change addResourceCollection(final PlayerID id, final ResourceCollection rCollection)
	{
		final CompositeChange cChange = new CompositeChange();
		for (final Resource r : rCollection.getResourcesCopy().keySet())
		{
			cChange.add(new ChangeResourceChange(id, r, rCollection.getQuantity(r)));
		}
		return cChange;
	}
	
	public static Change removeResourceCollection(final PlayerID id, final ResourceCollection rCollection)
	{
		final CompositeChange cChange = new CompositeChange();
		for (final Resource r : rCollection.getResourcesCopy().keySet())
		{
			cChange.add(new ChangeResourceChange(id, r, -rCollection.getQuantity(r)));
		}
		return cChange;
	}
	
	public static Change setProperty(final String property, final Object value, final GameData data)
	{
		return new SetPropertyChange(property, value, data.getProperties());
	}
	
	public static Change unitsHit(final IntegerMap<Unit> newHits)
	{
		return new UnitHitsChange(newHits);
	}
	
	public static Change addProductionRule(final ProductionRule rule, final ProductionFrontier frontier)
	{
		return new AddProductionRule(rule, frontier);
	}
	
	public static Change removeProductionRule(final ProductionRule rule, final ProductionFrontier frontier)
	{
		return new RemoveProductionRule(rule, frontier);
	}
	
	public static Change addAvailableTech(final TechnologyFrontier tf, final TechAdvance ta, final PlayerID player)
	{
		return new AddAvailableTech(tf, ta, player);
	}
	
	public static Change removeAvailableTech(final TechnologyFrontier tf, final TechAdvance ta, final PlayerID player)
	{
		return new RemoveAvailableTech(tf, ta, player);
	}
	
	public static Change attachmentPropertyChange(final IAttachment attachment, final Object newValue, final String property)
	{
		return new ChangeAttachmentChange(attachment, newValue, property);
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public static Change attachmentPropertyChange(final IAttachment attachment, final Object newValue, final String property, final boolean getRaw, final boolean resetFirst)
	{
		return new ChangeAttachmentChange(attachment, newValue, property, getRaw, resetFirst);
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public static Change attachmentPropertyChange(final Attachable attachment, final String attachmentName, final Object newValue, final Object oldValue, final String property,
				final boolean clearFirst)
	{
		return new ChangeAttachmentChange(attachment, attachmentName, newValue, oldValue, property, clearFirst);
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public static Change attachmentPropertyReset(final IAttachment attachment, final String property, final boolean getRaw)
	{
		return new AttachmentPropertyReset(attachment, property, getRaw);
	}
	
	public static Change genericTechChange(final TechAttachment attachment, final Boolean value, final String property)
	{
		return new GenericTechChange(attachment, value, property);
	}
	
	public static Change changeGameSteps(final GameSequence oldSequence, final GameStep[] newSteps)
	{
		return new GameSequenceChange(oldSequence, newSteps);
	}
	
	public static Change unitPropertyChange(final Unit unit, final Object newValue, final String propertyName)
	{
		return new ObjectPropertyChange(unit, propertyName, newValue);
	}
	
	public static Change addAttachmentChange(final IAttachment attachment, final Attachable attachable, final String name)
	{
		return new AddAttachmentChange(attachment, attachable, name);
	}
	
	public static Change addBattleRecords(final BattleRecords records, final GameData data)
	{
		return new AddBattleRecordsChange(records, data);
	}
	
	/** Creates new ChangeFactory. No need */
	private ChangeFactory()
	{
	}
	
	/**
	 * Creates a change of relationshipType between 2 players, for example: change Germany-France relationship from neutral to war.
	 * 
	 * @return the Change of relationship between 2 players
	 * */
	public static Change relationshipChange(final PlayerID player, final PlayerID player2, final RelationshipType currentRelation, final RelationshipType newRelation)
	{
		return new RelationshipChange(player, player2, currentRelation, newRelation);
	}
	
	/**
	 * Mark units as having no movement.
	 * 
	 * @param units
	 *            referring units
	 * @return change that contains marking of units as having no movement
	 */
	public static Change markNoMovementChange(final Collection<Unit> units)
	{
		if (units.isEmpty())
			return EMPTY_CHANGE;
		final CompositeChange change = new CompositeChange();
		final Iterator<Unit> iter = units.iterator();
		while (iter.hasNext())
		{
			change.add(markNoMovementChange(iter.next()));
		}
		return change;
	}
	
	public static Change markNoMovementChange(final Unit unit)
	{
		return unitPropertyChange(unit, TripleAUnit.get(unit).getMaxMovementAllowed(), TripleAUnit.ALREADY_MOVED);
	}
	
}


/**
 * Resets the value to the default value.
 */
class AttachmentPropertyReset extends Change
{
	private static final long serialVersionUID = 9208154387325299072L;
	private final Attachable m_attachedTo;
	private final String m_attachmentName;
	private final Object m_oldValue;
	private final String m_property;
	
	AttachmentPropertyReset(final IAttachment attachment, final String property, final boolean getRaw)
	{
		if (attachment == null)
			throw new IllegalArgumentException("No attachment, property:" + property);
		m_attachedTo = attachment.getAttachedTo();
		m_attachmentName = attachment.getName();
		m_oldValue = PropertyUtil.getPropertyFieldObject(property, attachment);
		m_property = property;
		/*if (getRaw)
		{
			m_oldValue = PropertyUtil.getRaw(property, attachment);
			m_property = property;
		}
		else
		{
			m_oldValue = PropertyUtil.get(property, attachment);
			m_property = property;
		}*/
	}
	
	AttachmentPropertyReset(final Attachable attachTo, final String attachmentName, final Object oldValue, final String property)
	{
		m_attachmentName = attachmentName;
		m_attachedTo = attachTo;
		m_oldValue = oldValue;
		m_property = property;
	}
	
	public Attachable getAttachedTo()
	{
		return m_attachedTo;
	}
	
	public String getAttachmentName()
	{
		return m_attachmentName;
	}
	
	@Override
	public void perform(final GameData data)
	{
		final IAttachment attachment = m_attachedTo.getAttachment(m_attachmentName);
		PropertyUtil.reset(m_property, attachment);
	}
	
	@Override
	public Change invert()
	{
		return new AttachmentPropertyResetUndo(m_attachedTo, m_attachmentName, m_oldValue, m_property);
	}
	
	@Override
	public String toString()
	{
		return "AttachmentPropertyClear attached to:" + m_attachedTo + " name:" + m_attachmentName + ", reset old value:" + m_oldValue;
	}
}


class AttachmentPropertyResetUndo extends Change
{
	private static final long serialVersionUID = 5943939650116851332L;
	private final Attachable m_attachedTo;
	private final String m_attachmentName;
	private final Object m_newValue;
	private final String m_property;
	
	AttachmentPropertyResetUndo(final Attachable attachTo, final String attachmentName, final Object newValue, final String property)
	{
		m_attachmentName = attachmentName;
		m_attachedTo = attachTo;
		m_newValue = newValue;
		m_property = property;
	}
	
	public Attachable getAttachedTo()
	{
		return m_attachedTo;
	}
	
	public String getAttachmentName()
	{
		return m_attachmentName;
	}
	
	@Override
	public void perform(final GameData data)
	{
		final IAttachment attachment = m_attachedTo.getAttachment(m_attachmentName);
		PropertyUtil.set(m_property, m_newValue, attachment, false);
	}
	
	@Override
	public Change invert()
	{
		return new AttachmentPropertyReset(m_attachedTo, m_attachmentName, m_newValue, m_property);
	}
	
	@Override
	public String toString()
	{
		return "AttachmentPropertyClearUndo attached to:" + m_attachedTo + " name:" + m_attachmentName + " new value:" + m_newValue;
	}
}


/**
 * RelationshipChange this creates a change in relationshipType between two players, for example from Neutral to War.
 * 
 */
class RelationshipChange extends Change
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2694339584633196289L;
	private final String m_player1;
	private final String m_player2;
	private final String m_OldRelation;
	private final String m_NewRelation;
	
	RelationshipChange(final PlayerID player1, final PlayerID player2, final RelationshipType oldRelation, final RelationshipType newRelation)
	{
		m_player1 = player1.getName();
		m_player2 = player2.getName();
		m_OldRelation = oldRelation.getName();
		m_NewRelation = newRelation.getName();
	}
	
	private RelationshipChange(final String player1, final String player2, final String oldRelation, final String newRelation)
	{
		m_player1 = player1;
		m_player2 = player2;
		m_OldRelation = oldRelation;
		m_NewRelation = newRelation;
	}
	
	@Override
	public Change invert()
	{
		return new RelationshipChange(m_player1, m_player2, m_NewRelation, m_OldRelation);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_player1 == null || m_player2 == null || m_OldRelation == null || m_NewRelation == null)
			throw new IllegalStateException("RelationshipChange may not have null arguments");*/
		data.getRelationshipTracker().setRelationship(data.getPlayerList().getPlayerID(m_player1), data.getPlayerList().getPlayerID(m_player2),
					data.getRelationshipTypeList().getRelationshipType(m_NewRelation));
		// now redraw territories in case of new hostility
		if (Matches.RelationshipTypeIsAtWar.match(data.getRelationshipTypeList().getRelationshipType(m_NewRelation)))
		{
			for (final Territory t : Match.getMatches(data.getMap().getTerritories(), new CompositeMatchAnd<Territory>(Matches.territoryHasUnitsOwnedBy(data.getPlayerList().getPlayerID(m_player1)),
						Matches.territoryHasUnitsOwnedBy(data.getPlayerList().getPlayerID(m_player2)))))
			{
				t.notifyChanged();
			}
		}
	}
	
	@Override
	public String toString()
	{
		/*if (m_player1 == null || m_player2 == null || m_OldRelation == null || m_NewRelation == null)
			throw new IllegalStateException("RelationshipChange may not have null arguments");*/
		return "Add relation change. " + m_player1 + " and " + m_player2 + " change from " + m_OldRelation + " to " + m_NewRelation;
	}
}


/**
 * Add units
 */
class AddUnits extends Change
{
	static final long serialVersionUID = 2694342784633196289L;
	private final String m_name;
	private final Collection<Unit> m_units;
	private final String m_type;
	
	AddUnits(final UnitCollection collection, final Collection<Unit> units)
	{
		m_units = new ArrayList<Unit>(units);
		m_name = collection.getHolder().getName();
		m_type = collection.getHolder().getType();
	}
	
	AddUnits(final String name, final String type, final Collection<Unit> units)
	{
		m_units = new ArrayList<Unit>(units);
		m_type = type;
		m_name = name;
	}
	
	@Override
	public Change invert()
	{
		return new RemoveUnits(m_name, m_type, m_units);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_name == null || m_type == null || m_units == null)
			throw new IllegalStateException("AddUnits change may not have null arguments: m_name: " + m_name + ", m_type: " + m_type + ", m_units: " + m_units);*/
		final UnitHolder holder = data.getUnitHolder(m_name, m_type);
		holder.getUnits().addAllUnits(m_units);
	}
	
	@Override
	public String toString()
	{
		/*if (m_name == null || m_type == null || m_units == null)
			throw new IllegalStateException("AddUnits change may not have null arguments: m_name: " + m_name + ", m_type: " + m_type + ", m_units: " + m_units);*/
		return "Add unit change.  Add to:" + m_name + " units:" + m_units;
	}
}


class RemoveUnits extends Change
{
	static final long serialVersionUID = -6410444472951010568L;
	private final String m_name;
	private final Collection<Unit> m_units;
	private final String m_type;
	
	RemoveUnits(final UnitCollection collection, final Collection<Unit> units)
	{
		this(collection.getHolder().getName(), collection.getHolder().getType(), units);
	}
	
	RemoveUnits(final String name, final String type, final Collection<Unit> units)
	{
		m_units = new ArrayList<Unit>(units);
		m_name = name;
		m_type = type;
	}
	
	RemoveUnits(final String name, final String type, final Collection<Unit> units, final boolean isCasualty)
	{
		m_type = type;
		m_units = new ArrayList<Unit>(units);
		m_name = name;
	}
	
	@Override
	public Change invert()
	{
		return new AddUnits(m_name, m_type, m_units);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_name == null || m_type == null || m_units == null)
			throw new IllegalStateException("RemoveUnits change may not have null arguments: m_name: " + m_name + ", m_type: " + m_type + ", m_units: " + m_units);*/
		final UnitHolder holder = data.getUnitHolder(m_name, m_type);
		if (!holder.getUnits().containsAll(m_units))
		{
			throw new IllegalStateException("Not all units present in:" + m_name + ".  Trying to remove:" + m_units + " present:" + holder.getUnits().getUnits());
		}
		holder.getUnits().removeAllUnits(m_units);
	}
	
	@Override
	public String toString()
	{
		/*if (m_name == null || m_type == null || m_units == null)
			throw new IllegalStateException("RemoveUnits change may not have null arguments: m_name: " + m_name + ", m_type: " + m_type + ", m_units: " + m_units);*/
		return "Remove unit change. Remove from:" + m_name + " units:" + m_units;
	}
}


/**
 * Changes ownership of a territory.
 */
class OwnerChange extends Change
{
	static final long serialVersionUID = -5938125380623744929L;
	/**
	 * Either new or old owner can be null.
	 */
	private final String m_old;
	private final String m_new;
	private final String m_territory;
	
	/**
	 * newOwner can be null
	 */
	OwnerChange(final Territory territory, final PlayerID newOwner)
	{
		m_territory = territory.getName();
		m_new = getName(newOwner);
		m_old = getName(territory.getOwner());
	}
	
	private OwnerChange(final String name, final String newOwner, final String oldOwner)
	{
		m_territory = name;
		m_new = newOwner;
		m_old = oldOwner;
	}
	
	private String getName(final PlayerID player)
	{
		if (player == null)
			return null;
		return player.getName();
	}
	
	private PlayerID getPlayerID(final String name, final GameData data)
	{
		if (name == null)
			return null;
		return data.getPlayerList().getPlayerID(name);
	}
	
	@Override
	public Change invert()
	{
		return new OwnerChange(m_territory, m_old, m_new);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		// both names could be null
		data.getMap().getTerritory(m_territory).setOwner(getPlayerID(m_new, data));
	}
	
	@Override
	public String toString()
	{
		return m_new + " takes " + m_territory + " from " + m_old;
	}
}


/**
 * Changes ownership of a unit.
 */
class PlayerOwnerChange extends Change
{
	/**
	 * Maps unit id -> owner as String
	 */
	private final Map<GUID, String> m_old;
	private final Map<GUID, String> m_new;
	private final String m_location;
	private static final long serialVersionUID = -9154938431233632882L;
	
	PlayerOwnerChange(final Collection<Unit> units, final PlayerID newOwner, final Territory location)
	{
		m_old = new HashMap<GUID, String>();
		m_new = new HashMap<GUID, String>();
		m_location = location.getName();
		for (final Unit unit : units)
		{
			m_old.put(unit.getID(), unit.getOwner().getName());
			m_new.put(unit.getID(), newOwner.getName());
		}
	}
	
	PlayerOwnerChange(final Map<GUID, String> newOwner, final Map<GUID, String> oldOwner, final String location)
	{
		m_old = oldOwner;
		m_new = newOwner;
		m_location = location;
	}
	
	@Override
	public Change invert()
	{
		return new PlayerOwnerChange(m_old, m_new, m_location);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_location == null || m_old == null || m_new == null)
			throw new IllegalStateException("PlayerOwnerChange may not have null arguments");*/
		for (final GUID id : m_new.keySet())
		{
			final Unit unit = data.getUnits().get(id);
			if (!m_old.get(id).equals(unit.getOwner().getName()))
			{
				throw new IllegalStateException("Wrong owner, expecting" + m_old.get(id) + " but got " + unit.getOwner());
			}
			final String owner = m_new.get(id);
			final PlayerID player = data.getPlayerList().getPlayerID(owner);
			unit.setOwner(player);
		}
		data.getMap().getTerritory(m_location).notifyChanged();
	}
	
	@Override
	public String toString()
	{
		/*if (m_location == null || m_old == null || m_new == null)
			throw new IllegalStateException("PlayerOwnerChange may not have null arguments");*/
		return "Some units change owners in territory " + m_location;
	}
}


/**
 * Changes unit production of a territory.
 */
class ChangeUnitProduction extends Change
{
	private static final long serialVersionUID = -1485932997086849018L;
	private final int m_unitProduction;
	private final int m_old;
	private final Territory m_location;
	
	ChangeUnitProduction(final Territory terr, final int quantity, final int oldQuantity)
	{
		m_location = terr;
		m_unitProduction = quantity;
		m_old = oldQuantity;
	}
	
	ChangeUnitProduction(final Territory terr, final int quantity)
	{
		m_location = terr;
		m_unitProduction = quantity;
		m_old = TerritoryAttachment.get(terr).getUnitProduction();
	}
	
	@Override
	public Change invert()
	{
		return new ChangeUnitProduction(m_location, m_old, m_unitProduction);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_location == null)
			throw new IllegalStateException("ChangeUnitProduction may not have null arguments");*/
		final TerritoryAttachment ta = TerritoryAttachment.get(m_location);
		ta.setUnitProduction(m_unitProduction);
		m_location.notifyChanged();
	}
	
	@Override
	public String toString()
	{
		/*if (m_location == null)
			throw new IllegalStateException("ChangeUnitProduction may not have null arguments");*/
		return "Change unit production.  Quantity:" + m_unitProduction + " Territory:" + m_location;
	}
	
}


/**
 * Adds/removes resource from a player.
 */
class ChangeResourceChange extends Change
{
	static final long serialVersionUID = -2304294240555842126L;
	private final String m_player;
	private final String m_resource;
	private final int m_quantity;
	
	ChangeResourceChange(final PlayerID player, final Resource resource, final int quantity)
	{
		m_player = player.getName();
		m_resource = resource.getName();
		m_quantity = quantity;
	}
	
	private ChangeResourceChange(final String player, final String resource, final int quantity)
	{
		m_player = player;
		m_resource = resource;
		m_quantity = quantity;
	}
	
	@Override
	public Change invert()
	{
		return new ChangeResourceChange(m_player, m_resource, -m_quantity);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_player == null || m_resource == null)
			throw new IllegalStateException("ChangeResourceChange may not have null arguments");*/
		final Resource resource = data.getResourceList().getResource(m_resource);
		final ResourceCollection resources = data.getPlayerList().getPlayerID(m_player).getResources();
		if (m_quantity > 0)
			resources.addResource(resource, m_quantity);
		else if (m_quantity < 0)
			resources.removeResource(resource, -m_quantity);
	}
	
	@Override
	public String toString()
	{
		/*if (m_player == null || m_resource == null)
			throw new IllegalStateException("ChangeResourceChange may not have null arguments");*/
		return "Change resource.  Resource:" + m_resource + " quantity:" + m_quantity + " Player:" + m_player;
	}
}


class SetPropertyChange extends Change
{
	private static final long serialVersionUID = -1377597975513821508L;
	private final String m_property;
	private final Object m_value;
	private final Object m_oldValue;
	
	SetPropertyChange(final String property, final Object value, final GameProperties properties)
	{
		m_property = property;
		m_value = value;
		m_oldValue = properties.get(property);
	}
	
	private SetPropertyChange(final String property, final Object value, final Object oldValue)
	{
		m_property = property;
		m_value = value;
		m_oldValue = oldValue;
	}
	
	@Override
	public Change invert()
	{
		return new SetPropertyChange(m_property, m_oldValue, m_value);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_property == null || m_value == null || m_oldValue == null)
			throw new IllegalStateException("SetPropertyChange may not have null arguments");*/
		data.getProperties().set(m_property, m_value);
	}
	/*public String toString()
	{
		//if (m_property == null || m_value == null || m_oldValue == null)
			//throw new IllegalStateException("SetPropertyChange may not have null arguments");
		
		return m_property + " changed from " + m_oldValue.toString() + " to " + m_value.toString();
	}*/
}


class AddProductionRule extends Change
{
	private static final long serialVersionUID = 2583955907289570063L;
	private final ProductionRule m_rule;
	private final ProductionFrontier m_frontier;
	
	public AddProductionRule(final ProductionRule rule, final ProductionFrontier frontier)
	{
		if (rule == null)
			throw new IllegalArgumentException("Null rule");
		if (frontier == null)
			throw new IllegalArgumentException("Null frontier");
		m_rule = rule;
		m_frontier = frontier;
	}
	
	@Override
	public void perform(final GameData data)
	{
		/*if (m_rule == null || m_frontier == null)
			throw new IllegalStateException("AddProductionRule may not have null arguments");*/
		m_frontier.addRule(m_rule);
	}
	
	@Override
	public Change invert()
	{
		return new RemoveProductionRule(m_rule, m_frontier);
	}
	/*public String toString()
	{
		//if (m_rule == null || m_frontier == null)
			//throw new IllegalStateException("AddProductionRule may not have null arguments");
		
		return m_rule.getName() + " added to " + m_frontier.getName();
	}*/
}


class RemoveProductionRule extends Change
{
	private static final long serialVersionUID = 2312599802275503095L;
	private final ProductionRule m_rule;
	private final ProductionFrontier m_frontier;
	
	public RemoveProductionRule(final ProductionRule rule, final ProductionFrontier frontier)
	{
		if (rule == null)
			throw new IllegalArgumentException("Null rule");
		if (frontier == null)
			throw new IllegalArgumentException("Null frontier");
		m_rule = rule;
		m_frontier = frontier;
	}
	
	@Override
	public void perform(final GameData data)
	{
		/*if (m_rule == null || m_frontier == null)
			throw new IllegalStateException("RemoveProductionRule may not have null arguments");*/
		m_frontier.removeRule(m_rule);
	}
	
	@Override
	public Change invert()
	{
		return new AddProductionRule(m_rule, m_frontier);
	}
	/*public String toString()
	{
		//if (m_rule == null || m_frontier == null)
			//throw new IllegalStateException("RemoveProductionRule may not have null arguments");
		
		return m_rule.getName() + " removed from " + m_frontier.getName();
	}*/
}


class AddAvailableTech extends Change
{
	private static final long serialVersionUID = 5664428883866434959L;
	private final TechAdvance m_tech;
	private final TechnologyFrontier m_frontier;
	private final PlayerID m_player;
	
	public AddAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerID player)
	{
		if (front == null)
			throw new IllegalArgumentException("Null tech category");
		if (tech == null)
			throw new IllegalArgumentException("Null tech");
		m_tech = tech;
		m_frontier = front;
		m_player = player;
	}
	
	@Override
	public void perform(final GameData data)
	{
		/*if (m_tech == null || m_frontier == null || m_player == null)
			throw new IllegalStateException("AddAvailableTech may not have null arguments");*/
		final TechnologyFrontier front = m_player.getTechnologyFrontierList().getTechnologyFrontier(m_frontier.getName());
		front.addAdvance(m_tech);
	}
	
	@Override
	public Change invert()
	{
		return new RemoveAvailableTech(m_frontier, m_tech, m_player);
	}
	/*public String toString()
	{
		//if (m_tech == null || m_frontier == null || m_player == null)
			//throw new IllegalStateException("AddAvailableTech may not have null arguments");
		
		return m_tech.getName() + " added to " + m_player.getName() + " technology frontier, " + m_frontier.getName();
	}*/
}


class RemoveAvailableTech extends Change
{
	private static final long serialVersionUID = 6131447662760022521L;
	private final TechAdvance m_tech;
	private final TechnologyFrontier m_frontier;
	private final PlayerID m_player;
	
	public RemoveAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerID player)
	{
		if (front == null)
			throw new IllegalArgumentException("Null tech category");
		if (tech == null)
			throw new IllegalArgumentException("Null tech");
		m_tech = tech;
		m_frontier = front;
		m_player = player;
	}
	
	@Override
	public void perform(final GameData data)
	{
		/*if (m_tech == null || m_frontier == null || m_player == null)
			throw new IllegalStateException("RemoveAvailableTech may not have null arguments");*/
		final TechnologyFrontier front = m_player.getTechnologyFrontierList().getTechnologyFrontier(m_frontier.getName());
		front.removeAdvance(m_tech);
	}
	
	@Override
	public Change invert()
	{
		return new AddAvailableTech(m_frontier, m_tech, m_player);
	}
	/*public String toString()
	{
		//if (m_tech == null || m_frontier == null || m_player == null)
			//throw new IllegalStateException("RemoveAvailableTech may not have null arguments");
		
		return m_tech.getName() + " removed from " + m_player.getName() + " technology frontier, " + m_frontier.getName();
	}*/
}


class AddAttachmentChange extends Change
{
	private static final long serialVersionUID = -21015135248288454L;
	private final IAttachment m_attachment;
	private final String m_originalAttachmentName;
	private final Attachable m_originalAttachable;
	private final Attachable m_attachable;
	private final String m_name;
	
	public AddAttachmentChange(final IAttachment attachment, final Attachable attachable, final String name)
	{
		m_attachment = attachment;
		m_originalAttachmentName = attachment.getName();
		m_originalAttachable = attachment.getAttachedTo();
		m_attachable = attachable;
		m_name = name;
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_attachment == null || m_originalAttachmentName == null || m_originalAttachable == null || m_attachable == null || m_name == null)
			throw new IllegalStateException("AddAttachmentChange may not have null arguments");*/
		m_attachable.addAttachment(m_name, m_attachment);
		// m_attachment.setData(data); // why set the data again?
		m_attachment.setName(m_name);
		m_attachment.setAttachedTo(m_attachable);
	}
	
	@Override
	public Change invert()
	{
		return new RemoveAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
	}
	/*public String toString()
	{
		//if (m_attachment == null || m_originalAttachmentName == null || m_originalAttachable == null || m_attachable == null || m_name == null)
			//throw new IllegalStateException("AddAttachmentChange may not have null arguments");
		
		return m_name + " attachment attached to " + m_attachable.toString();
	}*/
}


class RemoveAttachmentChange extends Change
{
	private static final long serialVersionUID = 6365648682759047674L;
	private final IAttachment m_attachment;
	private final String m_originalAttachmentName;
	private final Attachable m_originalAttachable;
	private final Attachable m_attachable;
	private final String m_name;
	
	public RemoveAttachmentChange(final IAttachment attachment, final Attachable attachable, final String name)
	{
		m_attachment = attachment;
		m_originalAttachmentName = attachment.getName();
		m_originalAttachable = attachment.getAttachedTo();
		m_attachable = attachable;
		m_name = name;
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_attachment == null || m_originalAttachmentName == null || m_originalAttachable == null || m_attachable == null || m_name == null)
			throw new IllegalStateException("RemoveAttachmentChange may not have null arguments");*/
		m_attachable.getAttachments().remove(m_name);
		m_attachment.setAttachedTo(null);
		/*final Map<String, IAttachment> attachments = m_attachable.getAttachments();
		attachments.remove(m_attachment);
		m_attachment.setAttachedTo(m_attachable);
		m_attachment.setName(m_name);
		if (m_attachable != null)
			m_attachable.addAttachment(m_name, m_attachment);*/
	}
	
	@Override
	public Change invert()
	{
		return new AddAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
	}
	/*public String toString()
	{
		//if (m_attachment == null || m_originalAttachmentName == null || m_originalAttachable == null || m_attachable == null || m_name == null)
			//throw new IllegalStateException("RemoveAttachmentChange may not have null arguments");
		
		return m_name + " attachment un-attached from " + m_attachable.toString();
	}*/
}


/**
 * Change a players production frontier.
 */
class ProductionFrontierChange extends Change
{
	private final String m_startFrontier;
	private final String m_endFrontier;
	private final String m_player;
	private static final long serialVersionUID = 3336145814067456701L;
	
	ProductionFrontierChange(final ProductionFrontier newFrontier, final PlayerID player)
	{
		m_startFrontier = player.getProductionFrontier().getName();
		m_endFrontier = newFrontier.getName();
		m_player = player.getName();
	}
	
	ProductionFrontierChange(final String startFrontier, final String endFrontier, final String player)
	{
		m_startFrontier = startFrontier;
		m_endFrontier = endFrontier;
		m_player = player;
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_startFrontier == null || m_endFrontier == null || m_player == null)
			throw new IllegalStateException("ProductionFrontierChange may not have null arguments");*/
		final PlayerID player = data.getPlayerList().getPlayerID(m_player);
		final ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(m_endFrontier);
		player.setProductionFrontier(frontier);
	}
	
	@Override
	public Change invert()
	{
		return new ProductionFrontierChange(m_endFrontier, m_startFrontier, m_player);
	}
	/*public String toString()
	{
		//if (m_startFrontier == null || m_endFrontier == null || m_player == null)
			//throw new IllegalStateException("ProductionFrontierChange may not have null arguments");
		
		return m_player + " production frontier changed from  " + m_startFrontier + " to " + m_endFrontier;
	}*/
}


class GameSequenceChange extends Change
{
	private static final long serialVersionUID = -8925565771506676074L;
	private final GameStep[] m_oldSteps;
	private final GameStep[] m_newSteps;
	
	GameSequenceChange(final GameSequence oldSequence, final GameStep[] newSteps)
	{
		final ArrayList<GameStep> oldSteps = new ArrayList<GameStep>();
		for (final GameStep step : oldSequence)
		{
			oldSteps.add(step);
		}
		// m_oldSteps = (GameStep[]) oldSteps.toArray();
		m_oldSteps = oldSteps.toArray(new GameStep[oldSteps.size()]);
		m_newSteps = newSteps;
	}
	
	private GameSequenceChange(final GameStep[] oldSteps, final GameStep[] newSteps)
	{
		m_oldSteps = oldSteps;
		m_newSteps = newSteps;
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_oldSteps == null || m_newSteps == null)
			throw new IllegalStateException("GameSequenceChange may not have null arguments");*/
		final GameSequence steps = data.getSequence();
		steps.removeAllSteps();
		for (final GameStep newStep : m_newSteps)
		{
			steps.addStep(newStep);
		}
	}
	
	@Override
	public Change invert()
	{
		return new GameSequenceChange(m_newSteps, m_oldSteps);
	}
	/*public String toString()
	{
		//if (m_oldSteps == null || m_newSteps == null)
			//throw new IllegalStateException("GameSequenceChange may not have null arguments");
		
		return m_oldSteps.toString() + " changed to  " + m_newSteps.toString();
	}*/
}


class ObjectPropertyChange extends Change
{
	private static final long serialVersionUID = 4218093376094170940L;
	private final Object m_object;
	private String m_property;
	private Object m_newValue;
	private Object m_oldValue;
	
	public ObjectPropertyChange(final Object object, final String property, final Object newValue)
	{
		m_object = object;
		m_property = property.intern();
		m_newValue = newValue;
		// m_oldValue = PropertyUtil.get(property, object);
		m_oldValue = PropertyUtil.getPropertyFieldObject(property, object);
	}
	
	/**
	 * Use canonical objects to reduce memory use after serialization.
	 */
	private Object resolve(final Object value)
	{
		if (value instanceof Boolean)
		{
			return Boolean.valueOf(((Boolean) value).booleanValue());
		}
		else if (value instanceof Integer)
		{
			return Integer.valueOf(((Integer) value).intValue());
		}
		return value;
	}
	
	public ObjectPropertyChange(final Object object, final String property, final Object newValue, final Object oldValue)
	{
		m_object = object;
		// prevent multiple copies of the property names being held in the game
		m_property = property.intern();
		m_newValue = newValue;
		m_oldValue = oldValue;
	}
	
	private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		stream.defaultReadObject();
		m_property = m_property.intern();
		m_newValue = resolve(m_newValue);
		m_oldValue = resolve(m_oldValue);
	}
	
	@Override
	public Change invert()
	{
		return new ObjectPropertyChange(m_object, m_property, m_oldValue, m_newValue);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		/*if (m_object == null || m_property == null)
			throw new IllegalStateException("ObjectPropertyChange may not have null arguments");*/
		PropertyUtil.set(m_property, m_newValue, m_object);
	}
	
	@Override
	public String toString()
	{
		/*if (m_object == null || m_property == null)
			throw new IllegalStateException("ObjectPropertyChange may not have null arguments");*/
		return "Property change, unit:" + m_object + " property:" + m_property + " newValue:" + m_newValue + " oldValue:" + m_oldValue;
	}
}


class GenericTechChange extends Change
{
	private static final long serialVersionUID = -2439447526511535571L;
	private final Attachable m_attachedTo;
	private final String m_attachmentName;
	private final Boolean m_newValue;
	private final Boolean m_oldValue;
	private final String m_property;
	
	public Attachable getAttachedTo()
	{
		return m_attachedTo;
	}
	
	public String getAttachmentName()
	{
		return m_attachmentName;
	}
	
	GenericTechChange(final TechAttachment attachment, final Boolean newValue, final String property)
	{
		if (attachment == null)
			throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
		m_attachedTo = attachment.getAttachedTo();
		m_attachmentName = attachment.getName();
		m_oldValue = Boolean.valueOf(attachment.hasGenericTech(property));
		m_newValue = Boolean.valueOf(newValue);
		m_property = property;
	}
	
	public GenericTechChange(final Attachable attachTo, final String attachmentName, final Boolean newValue, final Boolean oldValue, final String property)
	{
		m_attachmentName = attachmentName;
		m_attachedTo = attachTo;
		m_newValue = newValue;
		m_oldValue = oldValue;
		m_property = property;
	}
	
	@Override
	public void perform(final GameData data)
	{
		/*if (m_attachedTo == null || m_attachmentName == null || m_newValue == null || m_oldValue == null || m_property == null)
			throw new IllegalStateException("GenericTechChange may not have null arguments");*/
		final TechAttachment attachment = (TechAttachment) m_attachedTo.getAttachment(m_attachmentName);
		attachment.setGenericTech(m_property, m_newValue);
	}
	
	@Override
	public Change invert()
	{
		return new GenericTechChange(m_attachedTo, m_attachmentName, m_oldValue, m_newValue, m_property);
	}
	
	@Override
	public String toString()
	{
		/*if (m_attachedTo == null || m_attachmentName == null || m_newValue == null || m_oldValue == null || m_property == null)
			throw new IllegalStateException("GenericTechChange may not have null arguments");*/
		return "GenericTechChange attached to:" + m_attachedTo + " name:" + m_attachmentName + " new value:" + m_newValue + " old value:" + m_oldValue;
	}
}


/**
 * 
 * @author veqryn
 * 
 */
class AddBattleRecordsChange extends Change
{
	private static final long serialVersionUID = -6927678548172402611L;
	private final BattleRecords m_recordsToAdd;
	private final int m_round;
	
	AddBattleRecordsChange(final BattleRecords battleRecords, final GameData data)
	{
		m_round = data.getSequence().getRound();
		m_recordsToAdd = new BattleRecords(battleRecords); // make a copy because this is only done once, and only externally from battle tracker, and the source will be cleared (battle tracker clears out the records each turn)
	}
	
	AddBattleRecordsChange(final BattleRecords battleRecords, final int round)
	{
		m_round = round;
		m_recordsToAdd = battleRecords; // do not make a copy, this is only called from RemoveBattleRecordsChange, and we make a copy when we perform, so no need for another copy.
	}
	
	@Override
	protected void perform(final GameData data)
	{
		final Map<Integer, BattleRecords> currentRecords = data.getBattleRecordsList().getBattleRecordsMap();
		BattleRecordsList.addRecords(currentRecords, m_round, new BattleRecords(m_recordsToAdd)); // make a copy because otherwise ours will be cleared when we RemoveBattleRecordsChange
	}
	
	@Override
	public Change invert()
	{
		return new RemoveBattleRecordsChange(m_recordsToAdd, m_round);
	}
	
	@Override
	public String toString()
	{
		// This only occurs when serialization went badly, or something can not be serialized.
		if (m_recordsToAdd == null)
			throw new IllegalStateException("Records can not be null (most likely caused by improper or impossible serialization): " + m_recordsToAdd);
		return "Adding Battle Records: " + m_recordsToAdd;
	}
}


/**
 * 
 * @author veqryn
 * 
 */
class RemoveBattleRecordsChange extends Change
{
	private static final long serialVersionUID = 3286634991233029854L;
	private final BattleRecords m_recordsToRemove;
	private final int m_round;
	
	RemoveBattleRecordsChange(final BattleRecords battleRecords, final int round)
	{
		m_round = round;
		m_recordsToRemove = battleRecords; // do not make a copy, this is only called from AddBattleRecordsChange, and we make a copy when we perform, so no need for another copy.
	}
	
	@Override
	protected void perform(final GameData data)
	{
		final Map<Integer, BattleRecords> currentRecords = data.getBattleRecordsList().getBattleRecordsMap();
		BattleRecordsList.removeRecords(currentRecords, m_round, new BattleRecords(m_recordsToRemove)); // make a copy else we will get a concurrent modification error
	}
	
	@Override
	public Change invert()
	{
		return new AddBattleRecordsChange(m_recordsToRemove, m_round);
	}
	
	@Override
	public String toString()
	{
		// This only occurs when serialization went badly, or something can not be serialized.
		if (m_recordsToRemove == null)
			throw new IllegalStateException("Records can not be null (most likely caused by improper or impossible serialization): " + m_recordsToRemove);
		return "Adding Battle Records: " + m_recordsToRemove;
	}
}


class PlayerWhoAmIChange extends Change
{
	private static final long serialVersionUID = -1486914230174337300L;
	private final String m_startWhoAmI;
	private final String m_endWhoAmI;
	private final String m_player;
	
	PlayerWhoAmIChange(final String newWhoAmI, final PlayerID player)
	{
		m_startWhoAmI = player.getWhoAmI();
		m_endWhoAmI = newWhoAmI;
		m_player = player.getName();
	}
	
	PlayerWhoAmIChange(final String startWhoAmI, final String endWhoAmI, final String player)
	{
		m_startWhoAmI = startWhoAmI;
		m_endWhoAmI = endWhoAmI;
		m_player = player;
	}
	
	@Override
	protected void perform(final GameData data)
	{
		final PlayerID player = data.getPlayerList().getPlayerID(m_player);
		player.setWhoAmI(m_endWhoAmI);
	}
	
	@Override
	public Change invert()
	{
		return new PlayerWhoAmIChange(m_endWhoAmI, m_startWhoAmI, m_player);
	}
	
	@Override
	public String toString()
	{
		return m_player + " changed from " + m_startWhoAmI + " to " + m_endWhoAmI;
	}
}
