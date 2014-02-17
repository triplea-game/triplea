package games.strategy.triplea.ui.screen;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.MapData;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.List;

public class UnitsDrawer implements IDrawable
{
	private final int m_count;
	private final String m_unitType;
	private final String m_playerName;
	private final Point m_placementPoint;
	private final int m_damaged;
	private final int m_bombingUnitDamage;
	private final boolean m_disabled;
	private final boolean m_overflow;
	private final String m_territoryName;
	private final IUIContext m_uiContext;
	
	public UnitsDrawer(final int count, final String unitType, final String playerName, final Point placementPoint, final int damaged, final int bombingUnitDamage, final boolean disabled,
				final boolean overflow, final String territoryName, final IUIContext uiContext2)
	{
		m_count = count;
		m_unitType = unitType;
		m_playerName = playerName;
		m_placementPoint = placementPoint;
		m_damaged = damaged;
		m_bombingUnitDamage = bombingUnitDamage;
		m_disabled = disabled;
		m_overflow = overflow;
		m_territoryName = territoryName;
		m_uiContext = uiContext2;
	}
	
	public void prepare()
	{
	}
	
	public Point getPlacementPoint()
	{
		return m_placementPoint;
	}
	
	public String getPlayer()
	{
		return m_playerName;
	}
	
	public void draw(final Rectangle bounds, final GameData data, final Graphics2D graphics, final MapData mapData, final AffineTransform unscaled, final AffineTransform scaled)
	{
		if (m_overflow)
		{
			graphics.setColor(Color.BLACK);
			graphics.fillRect(m_placementPoint.x - bounds.x - 2, m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight(), m_uiContext.getUnitImageFactory()
						.getUnitImageWidth() + 2, 3);
		}
		final UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
		if (type == null)
			throw new IllegalStateException("Type not found:" + m_unitType);
		final PlayerID owner = data.getPlayerList().getPlayerID(m_playerName);
		final Image img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged > 0 || m_bombingUnitDamage > 0, m_disabled);
		/* VEQRYN: I can not figure out why we have the below code.  It appears to duplicate the call above.
		// figure the unitDamage here, for disabled or not
		if (Matches.UnitTypeCanBeDamaged.match(type) && (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data)))
		{
			// checks to see if this is being carried with a mouse over, or is in a territory.
			if (m_territoryName.length() != 0)
			{
				// kev, why are we doing a for loop here? each unit that needs to be drawn individually will be drawn if sorted properly, at least that was my understanding
				final Collection<Unit> units = Match.getMatches(data.getMap().getTerritory(m_territoryName).getUnits().getUnits(), Matches.unitIsOfType(type));
				for (final Unit current : units)
				{
					if (Matches.UnitIsDisabled().match(current))
					{
						img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged, true);
					}
				}
			}
			else
			{
				// needed, don't delete please. if it is a mouse over, we need to carry the unit on our mouse
				img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged, m_disabled);
			}
		}
		if (!m_damaged && Matches.UnitTypeCanBeDamaged.match(type) && isDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
		{
			// checks to see if this is being carried with a mouse over, or is in a territory.
			if (m_territoryName.length() != 0)
			{
				if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data))
				{
					// kev, why are we doing a for loop here? each unit that needs to be drawn individually will be drawn if sorted properly, at least that was my understanding
					final Collection<Unit> units = Match.getMatches(data.getMap().getTerritory(m_territoryName).getUnits().getUnits(), Matches.unitIsOfType(type));
					for (final Unit current : units)
					{
						if (Matches.UnitHasSomeUnitDamage().match(current))
						{
							img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, true, m_disabled);
						}
					}
				}
			}
			else
			{
				// needed, don't delete please. if it is a mouse over, we need to carry the unit on our mouse
				img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged, m_disabled);
			}
		}
		else
		{
			// needed, don't delete please. if it is a mouse over, we need to carry the unit on our mouse
			img = m_uiContext.getUnitImageFactory().getImage(type, owner, data, m_damaged, m_disabled);
		}
		*/
		graphics.drawImage(img, m_placementPoint.x - bounds.x, m_placementPoint.y - bounds.y, null);
		// more then 1 unit of this category
		if (m_count != 1)
		{
			final int stackSize = mapData.getDefaultUnitsStackSize();
			if (stackSize > 0)
			{ // Display more units as a stack
				for (int i = 1; i < m_count && i < stackSize; i++)
				{
					graphics.drawImage(img, m_placementPoint.x + 2 * i - bounds.x, m_placementPoint.y - 2 * i - bounds.y, null);
				}
				if (m_count > stackSize)
				{
					final Font font = MapImage.getPropertyMapFont();
					if (font.getSize() > 0)
					{
						graphics.setColor(MapImage.getPropertyUnitCountColor());
						graphics.setFont(font);
						graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + 2 * stackSize
									+ (m_uiContext.getUnitImageFactory().getUnitImageWidth() * 6 / 10), m_placementPoint.y - 2 * stackSize - bounds.y
									+ m_uiContext.getUnitImageFactory().getUnitImageHeight() * 1 / 3);
					}
				}
			}
			else
			{ // Display a white number at the bottom of the unit
				final Font font = MapImage.getPropertyMapFont();
				if (font.getSize() > 0)
				{
					graphics.setColor(MapImage.getPropertyUnitCountColor());
					graphics.setFont(font);
					graphics.drawString(String.valueOf(m_count), m_placementPoint.x - bounds.x + (m_uiContext.getUnitImageFactory().getUnitCounterOffsetWidth()), m_placementPoint.y - bounds.y
								+ m_uiContext.getUnitImageFactory().getUnitCounterOffsetHeight());
				}
			}
		}
		displayHitDamage(bounds, data, graphics, type, img);
		// Display Factory Damage
		if (isDamageFromBombingDoneToUnitsInsteadOfTerritories(data) && Matches.UnitTypeCanBeDamaged.match(type))
		{
			displayFactoryDamage(bounds, data, graphics, type, img);
		}
	}
	
	private void displayFactoryDamage(final Rectangle bounds, final GameData data, final Graphics2D graphics, final UnitType type, final Image img)
	{
		final Font font = MapImage.getPropertyMapFont();
		if (m_territoryName.length() != 0 && font.getSize() > 0 && m_bombingUnitDamage > 0)
		{
			graphics.setColor(MapImage.getPropertyUnitFactoryDamageColor());
			graphics.setFont(font);
			graphics.drawString("" + m_bombingUnitDamage, m_placementPoint.x - bounds.x + (m_uiContext.getUnitImageFactory().getUnitImageWidth() / 4),
						m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight() / 4);
		}
	}
	
	private void displayHitDamage(final Rectangle bounds, final GameData data, final Graphics2D graphics, final UnitType type, final Image img)
	{
		final Font font = MapImage.getPropertyMapFont();
		if (m_territoryName.length() != 0 && font.getSize() > 0 && m_damaged > 1)
		{
			graphics.setColor(MapImage.getPropertyUnitHitDamageColor());
			graphics.setFont(font);
			graphics.drawString("" + m_damaged, m_placementPoint.x - bounds.x + (m_uiContext.getUnitImageFactory().getUnitImageWidth() * 3 / 4),
						m_placementPoint.y - bounds.y + m_uiContext.getUnitImageFactory().getUnitImageHeight() / 4);
		}
	}
	
	public Tuple<Territory, List<Unit>> getUnits(final GameData data)
	{
		// note - it may be the case where the territory is being changed as a result
		// to a mouse click, and the map units haven't updated yet, so the unit count
		// from the territory wont match the units in m_count
		final Territory t = data.getMap().getTerritory(m_territoryName);
		final UnitType type = data.getUnitTypeList().getUnitType(m_unitType);
		final CompositeMatch<Unit> selectedUnits = new CompositeMatchAnd<Unit>();
		selectedUnits.add(Matches.unitIsOfType(type));
		selectedUnits.add(Matches.unitIsOwnedBy(data.getPlayerList().getPlayerID(m_playerName)));
		if (m_damaged > 0)
			selectedUnits.add(Matches.UnitHasTakenSomeDamage);
		else
			selectedUnits.add(Matches.UnitHasNotTakenAnyDamage);
		if (m_bombingUnitDamage > 0)
			selectedUnits.add(Matches.UnitHasTakenSomeBombingUnitDamage);
		else
			selectedUnits.add(Matches.UnitHasNotTakenAnyBombingUnitDamage);
		final List<Unit> rVal = t.getUnits().getMatches(selectedUnits);
		return new Tuple<Territory, List<Unit>>(t, rVal);
	}
	
	public int getLevel()
	{
		return UNITS_LEVEL;
	}
	
	@Override
	public String toString()
	{
		return "UnitsDrawer for " + m_count + " " + MyFormatter.pluralize(m_unitType) + " in  " + m_territoryName;
	}
	
	private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data)
	{
		return games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
	}
}
