/*
 * MapSelectionListener.java
 *
 * Created on November 5, 2001, 2:12 PM
 */

package games.strategy.triplea.ui;

import java.awt.event.MouseEvent;

import games.strategy.engine.data.Territory;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface MapSelectionListener 
{
	public void territorySelected(Territory territory, MouseEvent me);
	
	/**
	 * The mouse has entered the given territory, 
	 * null if the mouse is in no territory.
	 */
	public void mouseEntered(Territory territory);
}
