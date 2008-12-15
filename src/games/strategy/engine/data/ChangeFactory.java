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
import games.strategy.util.IntegerMap;
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
 * All changes made to GameData should be made through changes produced here.
 * <br>
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
    
        @Override
        protected void perform(GameData data)
        {}
    
        @Override
        public Change invert()
        {
            return this;
        }
        
        //when de-serializing, always return the singleton
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
    
    public static Change changeOwner(Territory territory, PlayerID owner)
    {

        return new OwnerChange(territory, owner);
    }

    public static Change changeOwner(Collection<Unit> units, PlayerID owner, Territory location)
    {

        return new PlayerOwnerChange(units, owner, location);
    }

    public static Change changeOwner(Unit unit, PlayerID owner, Territory location)
    {

        ArrayList<Unit> list = new ArrayList<Unit>(1);
        list.add(unit);
        return new PlayerOwnerChange(list, owner, location);
    }

    public static Change addUnits(Territory territory, Collection<Unit> units)
    {

        return new AddUnits(territory.getUnits(), units);
    }

    public static Change removeUnits(Territory territory, Collection<Unit> units)
    {

        return new RemoveUnits(territory.getUnits(), units);
    }

    public static Change addUnits(PlayerID player, Collection<Unit> units)
    {

        return new AddUnits(player.getUnits(), units);
    }

    public static Change removeUnits(PlayerID player, Collection<Unit> units)
    {

        return new RemoveUnits(player.getUnits(), units);
    }

    public static Change moveUnits(Territory start, Territory end, Collection<Unit> units)
    {

        units = new ArrayList<Unit>(units);
        List<Change> changes = new ArrayList<Change>(2);
        changes.add(removeUnits(start, units));
        changes.add(addUnits(end, units));
        return new CompositeChange(changes);
    }

    public static Change changeProductionFrontier(PlayerID player, ProductionFrontier frontier)
    {

        return new ProductionFrontierChange(frontier, player);
    }

    public static Change changeResourcesChange(PlayerID player, Resource resource, int quantity)
    {

        return new ChangeResourceChange(player, resource, quantity);
    }

    public static Change changeProductionFrontierChange(PlayerID player, ProductionFrontier newFrontier)
    {

        return new ProductionFrontierChange(newFrontier, player);
    }

    public static Change setProperty(String property, Object value, GameData data)
    {

        return new SetPropertyChange(property, value, data.getProperties());
    }

    public static Change unitsHit(IntegerMap<Unit> newHits)
    {

        return new UnitHitsChange(newHits);
    }

    public static Change addProductionRule(ProductionRule rule, ProductionFrontier frontier)
    {
        return new AddProductionRule(rule, frontier);
    }

    public static Change removeProductionRule(ProductionRule rule, ProductionFrontier frontier)
    {
        return new RemoveProductionRule(rule, frontier);
    }    
    
    public static Change attachmentPropertyChange(IAttachment attatchment, Object newValue, String property)
    {
        return new ChangeAttachmentChange(attatchment, newValue, property);
    }

    public static Change attachmentPropertyChange(Attachable attatchment, String attatchmentName, Object newValue, Object oldValue, String property)
    {
        return new ChangeAttachmentChange(attatchment, attatchmentName, newValue, oldValue, property);
    }
    
    public static Change changeGameSteps(GameSequence oldSequence, GameStep[] newSteps)
    {
        return new GameSequenceChange(oldSequence, newSteps);
    }

    public static Change unitPropertyChange(Unit unit, Object newValue, String propertyName) 
    {
        return new ObjectPropertyChange(unit,propertyName, newValue );
    }
    
    public static Change addAttachmentChange(IAttachment attachment, Attachable attachable, String name)
    {
        return new AddAttachmentChange(attachment, attachable, name);
    }
    
    /** Creates new ChangeFactory. No need */
    private ChangeFactory()
    {

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

    AddUnits(UnitCollection collection, Collection<Unit> units)
    {
        m_units = new ArrayList<Unit>(units);
        m_name = collection.getHolder().getName();
        m_type = collection.getHolder().getType();
    }

    AddUnits(String name, String type, Collection<Unit> units)
    {
        m_units = new ArrayList<Unit>(units);
        m_type = type;
        m_name = name;
    }

    public Change invert()
    {

        return new RemoveUnits(m_name, m_type, m_units);
    }

    protected void perform(GameData data)
    {

        UnitHolder holder = data.getUnitHolder(m_name, m_type);
        holder.getUnits().addAllUnits(m_units);
    }

    public String toString()
    {

        return "Add unit change.  Add to:" + m_name + " units:" + m_units;
    }
}


class RemoveUnits extends Change
{

    static final long serialVersionUID = -6410444472951010568L;

    private final String m_name;
    private final Collection<Unit> m_units;
    private final String m_type;

    RemoveUnits(UnitCollection collection, Collection<Unit> units)
    {

        this(collection.getHolder().getName(), collection.getHolder().getType(), units);
    }
 
    RemoveUnits(String name, String type, Collection<Unit> units)
    {
        
        m_units = new ArrayList<Unit>(units);
        m_name = name;
        m_type = type;
    }

    RemoveUnits(String name, String type, Collection<Unit> units, boolean isCasualty)
    {
        m_type = type;
        m_units = new ArrayList<Unit>(units);
        m_name = name;
    }

    public Change invert()
    {

        return new AddUnits(m_name, m_type, m_units);
    }

    protected void perform(GameData data)
    {

        UnitHolder holder = data.getUnitHolder(m_name, m_type);
        if(!holder.getUnits().containsAll(m_units))
        {
            throw new IllegalStateException("Not all units present in:" + m_name + ".  Trying to remove:" + m_units + " present:" + holder.getUnits().getUnits());
        }
        
        holder.getUnits().removeAllUnits(m_units);
    }

    public String toString()
    {

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
    OwnerChange(Territory territory, PlayerID newOwner)
    {

        m_territory = territory.getName();
        m_new = getName(newOwner);
        m_old = getName(territory.getOwner());
    }

    private OwnerChange(String name, String newOwner, String oldOwner)
    {

        m_territory = name;
        m_new = newOwner;
        m_old = oldOwner;
    }

    private String getName(PlayerID player)
    {

        if (player == null)
            return null;
        return player.getName();
    }

    private PlayerID getPlayerID(String name, GameData data)
    {

        if (name == null)
            return null;
        return data.getPlayerList().getPlayerID(name);
    }

    public Change invert()
    {

        return new OwnerChange(m_territory, m_old, m_new);
    }

    protected void perform(GameData data)
    {

        //both names could be null
        data.getMap().getTerritory(m_territory).setOwner(getPlayerID(m_new, data));
    }

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

    PlayerOwnerChange(Collection<Unit> units, PlayerID newOwner, Territory location)
    {

        m_old = new HashMap<GUID, String>();
        m_new = new HashMap<GUID, String>();
        m_location = location.getName();
        
        Iterator<Unit> iter = units.iterator();
        while (iter.hasNext())
        {
            Unit unit = iter.next();
            m_old.put(unit.getID(), unit.getOwner().getName());
            m_new.put(unit.getID(), newOwner.getName());
        }
    }

    PlayerOwnerChange(Map<GUID, String> newOwner, Map<GUID, String> oldOwner, String location)
    {
        m_old = oldOwner;
        m_new = newOwner;
        m_location = location;
    }

    public Change invert()
    {

        return new PlayerOwnerChange(m_old, m_new, m_location);
    }

    protected void perform(GameData data)
    {

        Iterator<GUID> iter = m_new.keySet().iterator();
        while (iter.hasNext())
        {
            GUID id = iter.next();
            Unit unit = data.getUnits().get(id);
            
            if(!m_old.get(id).equals(unit.getOwner().getName()))
            {
                throw new IllegalStateException("Wrong owner, expecting" + m_old.get(id) +" but got " + unit.getOwner());
            }
            
            String owner = m_new.get(id);
            PlayerID player = data.getPlayerList().getPlayerID(owner);
            unit.setOwner(player);
        }
        data.getMap().getTerritory(m_location).notifyChanged();
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

    ChangeResourceChange(PlayerID player, Resource resource, int quantity)
    {

        m_player = player.getName();
        m_resource = resource.getName();
        m_quantity = quantity;
    }

    private ChangeResourceChange(String player, String resource, int quantity)
    {

        m_player = player;
        m_resource = resource;
        m_quantity = quantity;
    }

    public Change invert()
    {

        return new ChangeResourceChange(m_player, m_resource, -m_quantity);
    }

    protected void perform(GameData data)
    {

        Resource resource = data.getResourceList().getResource(m_resource);
        ResourceCollection resources = data.getPlayerList().getPlayerID(m_player).getResources();
        if (m_quantity > 0)
            resources.addResource(resource, m_quantity);
        else if (m_quantity < 0)
            resources.removeResource(resource, -m_quantity);
    }

    public String toString()
    {

        return "Change resource.  Resource:" + m_resource + " quantity:" + m_quantity + " Player:" + m_player;
    }
}


class SetPropertyChange extends Change
{

    private final String m_property;
    private final Object m_value;
    private final Object m_oldValue;

    SetPropertyChange(String property, Object value, GameProperties properties)
    {

        m_property = property;
        m_value = value;
        m_oldValue = properties.get(property);
    }

    private SetPropertyChange(String property, Object value, Object oldValue)
    {

        m_property = property;
        m_value = value;
        m_oldValue = oldValue;
    }

    public Change invert()
    {

        return new SetPropertyChange(m_property, m_oldValue, m_value);
    }

    protected void perform(GameData data)
    {

        data.getProperties().set(m_property, m_value);
    }

}


class AddProductionRule extends Change
{

    private ProductionRule m_rule;
    private ProductionFrontier m_frontier;

    public AddProductionRule(ProductionRule rule, ProductionFrontier frontier)
    {
        if(rule == null)
            throw new IllegalArgumentException("Null rule");
        if(frontier == null)
            throw new IllegalArgumentException("Null frontier");            

        m_rule = rule;
        m_frontier = frontier;
    }

    public void perform(GameData data)
    {

        m_frontier.addRule(m_rule);
    }

    public Change invert()
    {
        return new RemoveProductionRule(m_rule, m_frontier);

    }
}


class RemoveProductionRule extends Change
{

    private ProductionRule m_rule;
    private ProductionFrontier m_frontier;

    public RemoveProductionRule(ProductionRule rule, ProductionFrontier frontier)
    {
        if(rule == null)
            throw new IllegalArgumentException("Null rule");
        if(frontier == null)
            throw new IllegalArgumentException("Null frontier");            

        m_rule = rule;
        m_frontier = frontier;
    }

    public void perform(GameData data)
    {

        m_frontier.removeRule(m_rule);
    }

    public Change invert()
    {

        return new AddProductionRule(m_rule, m_frontier);
    }

}

/**
 * Change a players production frontier.
 */


class AddAttachmentChange extends Change
{
    private final IAttachment m_attachment;
    private final String m_originalAttachmentName;
    private final Attachable m_originalAttachable;
    
    private final Attachable m_attachable;
    private final String m_name;
   
    public AddAttachmentChange(IAttachment attachment, Attachable attachable, String name)
    {
        m_attachment = attachment;
        m_originalAttachmentName = attachment.getName();
        m_originalAttachable = attachment.getAttatchedTo();
        
        m_attachable = attachable;
        m_name = name;
    }
   
    @Override
    protected void perform(GameData data)
    {
        m_attachable.addAttachment(m_name, m_attachment);
        m_attachment.setData(data);
        m_attachment.setName(m_name);
        m_attachment.setAttatchedTo(m_attachable);
    }

    @Override
    public Change invert()
    {
        return new RemoveAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
    }
    
}

class RemoveAttachmentChange extends Change
{
    private final IAttachment m_attachment;
    private final String m_originalAttachmentName;
    private final Attachable m_originalAttachable;
    
    private final Attachable m_attachable;
    private final String m_name;
    
    public RemoveAttachmentChange(IAttachment attachment, Attachable attachable, String name)
    {
        m_attachment = attachment;
        m_originalAttachmentName = attachment.getName();
        m_originalAttachable = attachment.getAttatchedTo();
        
        m_attachable = attachable;
        m_name = name;
    }

    @Override
    protected void perform(GameData data)
    {
        Map<String, IAttachment> attachments = m_attachable.getAttachments();
        attachments.remove(m_attachment);
        
        m_attachment.setAttatchedTo(m_attachable);
        m_attachment.setName(m_name);
        if (m_attachable!=null)
            m_attachable.addAttachment(m_name, m_attachment);
    }

    @Override
    public Change invert()
    {
        return new AddAttachmentChange(m_attachment, m_originalAttachable, m_originalAttachmentName);
    }

    
    
    
}

class ProductionFrontierChange extends Change
{

    private final String m_startFrontier;
    private final String m_endFrontier;
    private final String m_player;

    private static final long serialVersionUID = 3336145814067456701L;

    ProductionFrontierChange(ProductionFrontier newFrontier, PlayerID player)
    {

        m_startFrontier = player.getProductionFrontier().getName();
        m_endFrontier = newFrontier.getName();
        m_player = player.getName();
    }

    ProductionFrontierChange(String startFrontier, String endFrontier, String player)
    {

        m_startFrontier = startFrontier;
        m_endFrontier = endFrontier;
        m_player = player;
    }

    protected void perform(GameData data)
    {

        PlayerID player = data.getPlayerList().getPlayerID(m_player);
        ProductionFrontier frontier = data.getProductionFrontierList().getProductionFrontier(m_endFrontier);
        player.setProductionFrontier(frontier);
    }

    public Change invert()
    {

        return new ProductionFrontierChange(m_endFrontier, m_startFrontier, m_player);
    }

}



class GameSequenceChange extends Change
{
    private final GameStep[] m_oldSteps;
    private final GameStep[] m_newSteps;
    
    GameSequenceChange(GameSequence oldSequence, GameStep[] newSteps)
    {
        ArrayList<GameStep> oldSteps = new ArrayList<GameStep>();
        
        for (GameStep step : oldSequence)
        {
            oldSteps.add(step);
        }
        
        m_oldSteps = (GameStep[]) oldSteps.toArray();
        m_newSteps = newSteps;
    }
    
    private GameSequenceChange(GameStep[] oldSteps, GameStep[] newSteps)
    {
        m_oldSteps = oldSteps;
        m_newSteps = newSteps;
    } 
    
    protected void perform(GameData data)
    {
        GameSequence steps = data.getSequence();
        steps.removeAllSteps();
        
        for (GameStep newStep : m_newSteps)
        {
            steps.addStep(newStep);
        }
    }
    
    public Change invert()
    {
        return new GameSequenceChange(m_newSteps, m_oldSteps);
    }
    
}

class ObjectPropertyChange extends Change 
{
    private final Object m_object;
    private String m_property;
    private Object m_newValue;
    private Object m_oldValue;
    
    public ObjectPropertyChange(final Object object, final String property, final Object newValue)
    {
        m_object = object;
        m_property = property.intern();
        m_newValue = newValue;
        m_oldValue = PropertyUtil.get(property, object);        
    }
    
    /**
     * Use canonical objects to reduce memory use after serialization. 
     */
    private Object resolve(Object value) 
    {
        if(value instanceof Boolean) { 
            return Boolean.valueOf(((Boolean) value).booleanValue());
        } else if(value instanceof Integer) {
            return Integer.valueOf(( (Integer) value).intValue());
        }
        return value;
    }
    
    public ObjectPropertyChange(final Object object, final String property, final Object newValue, final Object oldValue)
    {
        m_object = object;
        //prevent multiple copies of the property names being held in the game
        m_property = property.intern();
        m_newValue = newValue;
        m_oldValue = oldValue;
    }

    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException
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
    protected void perform(GameData data)
    {
        PropertyUtil.set(m_property, m_newValue, m_object);
        
    }
    
    public String toString() 
    {
        return "Property change, unit:" + m_object + " property:" + m_property + " newValue:" + m_newValue + " oldValue:" + m_oldValue;
    }
    
    
}

