/*
 * TripleA.java
 *
 * Created on November 2, 2001, 8:56 PM
 */

package games.strategy.triplea;

import java.util.*;
import java.net.URL;
import java.io.*;
import javax.swing.*;
import org.xml.sax.SAXException;

import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.gamePlayer.GamePlayer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.*;
import games.strategy.net.*;


import games.strategy.triplea.ui.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TripleA 
{

	public static Set createPlayers(Set playerNames)
	{
		Set players = new HashSet();
		Iterator iter = playerNames.iterator();
		while(iter.hasNext())
		{
			String name = (String) iter.next();
			TripleAPlayer player = new TripleAPlayer(name);
			players.add(player);
		}
		return players;
	}
	
	
	public static void startUI(IGame game, Set players)
	{
		try
		{
			TripleAFrame frame = new TripleAFrame(game.getData(), players);
						
			frame.setSize(800,600);
			
			frame.setVisible(true);
			while(!frame.isVisible())
			{
				Thread.currentThread().yield();
			}

			connectPlayers(players, frame);		
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(0);
		}
	}
		
	private static void connectPlayers(Set players, TripleAFrame frame)
	{
		Iterator iter = players.iterator();
		while(iter.hasNext())
		{	
			TripleAPlayer player = (TripleAPlayer) iter.next();
			player.setFrame(frame);
		}
	}
		
	/** Creates new TripleA */
    private TripleA() 
	{
    }
}
