/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * TripleA.java
 * 
 * 
 * Created on November 2, 2001, 8:56 PM
 */
package games.strategy.triplea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.display.IDisplayBridge;
import games.strategy.engine.framework.AbstractGameLoader;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.net.GUID;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.ai.strongAI.StrongAI;
import games.strategy.triplea.ai.weakAI.DoesNothingAI;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.triplea.ui.display.TripleaDisplay;

import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleA extends AbstractGameLoader implements IGameLoader
{
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = -8374315848374732436L;
	public static final String HUMAN_PLAYER_TYPE = "Human";
	public static final String WEAK_COMPUTER_PLAYER_TYPE = "E.Z. Fodder (AI)";
	public static final String STRONG_COMPUTER_PLAYER_TYPE = "Moore N. Able (AI)";
	// public static final String ADVANCED_COMPUTER_PLAYER_TYPE = "Moore N. Able 2 (AI)";
	public static final String DYNAMIX_COMPUTER_PLAYER_TYPE = "Dynamix Land-Only (AI)";
	public static final String DOESNOTHINGAI_COMPUTER_PLAYER_TYPE = "Does Nothing (AI)";
	// public static final String NONE = "None (AI)";
	protected transient ITripleaDisplay m_display;
	
	public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames)
	{
		final Set<IGamePlayer> players = new HashSet<IGamePlayer>();
		for (final String name : playerNames.keySet())
		{
			final String type = playerNames.get(name);
			if (type.equals(WEAK_COMPUTER_PLAYER_TYPE))
			{
				players.add(new WeakAI(name, type));
			}
			else if (type.equals(STRONG_COMPUTER_PLAYER_TYPE))
			{
				players.add(new StrongAI(name, type));
			}
			/*
			else if (type.equals(ADVANCED_COMPUTER_PLAYER_TYPE))
			{
				players.add(new AdvancedAI(name, type));
			}
			*/
			else if (type.equals(DYNAMIX_COMPUTER_PLAYER_TYPE))
			{
				players.add(new Dynamix_AI(name, type));
			}
			else if (type.equals(DOESNOTHINGAI_COMPUTER_PLAYER_TYPE))
			{
				players.add(new DoesNothingAI(name, type));
			}
			else if (type.equals(HUMAN_PLAYER_TYPE) || type.equals(CLIENT_PLAYER_TYPE))
			{
				final TripleAPlayer player = new TripleAPlayer(name, type);
				players.add(player);
			}
			else
			{
				throw new IllegalStateException("Player type not recognized:" + type);
			}
		}
		return players;
	}
	
	@Override
	public void shutDown()
	{
		super.shutDown();
		if (m_display != null)
		{
			m_game.removeDisplay(m_display);
			m_display.shutDown();
			m_display = null;
		}
	}
	
	protected void initializeGame()
	{
	}
	
	public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless) throws Exception
	{
		try
		{
			/*
			   Retreive the map name from xml file
			   This is the key for triplea to find the maps
			*/
			m_game = game;
			// final String mapDir = game.getData().getProperties().get(Constants.MAP_NAME).toString();
			if (game.getData().getDelegateList().getDelegate("edit") == null)
			{
				// an evil awful hack
				// we don't want to change the game xml
				// and invalidate mods so hack it
				// and force the addition here
				final EditDelegate delegate = new EditDelegate();
				delegate.initialize("edit", "edit");
				m_game.getData().getDelegateList().addDelegate(delegate);
				if (game instanceof ServerGame)
				{
					((ServerGame) game).addDelegateMessenger(delegate);
				}
			}
			final LocalPlayers localPlayers = new LocalPlayers(players);
			if (headless)
			{
				m_display = new ITripleaDisplay()
				{
					public void initialize(final IDisplayBridge bridge)
					{
					}
					
					public void shutDown()
					{
					}
					
					public void reportMessageToAll(final String message, final String title, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers)
					{
					}
					
					public void reportMessageToPlayers(final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers, final String message, final String title)
					{
					}
					
					public void showBattle(final GUID battleID, final Territory location, final String battleTitle, final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits,
								final Collection<Unit> killedUnits,
								final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie, final Map<Unit, Collection<Unit>> dependentUnits, final PlayerID attacker,
								final PlayerID defender,
								final boolean isAmphibious, final BattleType battleType, final Collection<Unit> amphibiousLandAttackers)
					{
					}
					
					public void listBattleSteps(final GUID battleID, final List<String> steps)
					{
					}
					
					public void battleEnd(final GUID battleID, final String message)
					{
					}
					
					public void casualtyNotification(final GUID battleID, final String step, final DiceRoll dice, final PlayerID player, final Collection<Unit> killed, final Collection<Unit> damaged,
								final Map<Unit, Collection<Unit>> dependents)
					{
					}
					
					public void deadUnitNotification(final GUID battleID, final PlayerID player, final Collection<Unit> dead, final Map<Unit, Collection<Unit>> dependents)
					{
					}
					
					public void changedUnitsNotification(final GUID battleID, final PlayerID player, final Collection<Unit> removedUnits, final Collection<Unit> addedUnits,
								final Map<Unit, Collection<Unit>> dependents)
					{
					}
					
					public void bombingResults(final GUID battleID, final List<Die> dice, final int cost)
					{
					}
					
					public void notifyRetreat(final String shortMessage, final String message, final String step, final PlayerID retreatingPlayer)
					{
					}
					
					public void notifyRetreat(final GUID battleId, final Collection<Unit> retreating)
					{
					}
					
					public void notifyDice(final GUID battleId, final DiceRoll dice, final String stepName)
					{
					}
					
					public void gotoBattleStep(final GUID battleId, final String step)
					{
					}
				};
				m_soundChannel = new ISound()
				{
					public void initialize()
					{
					}
					
					public void shutDown()
					{
					}
					
					public void playSoundForAll(final String clipName, final String subFolder)
					{
					}
					
					public void playSoundForAll(final String clipName, final String subFolder, final boolean doNotIncludeHost, final boolean doNotIncludeClients, final boolean doNotIncludeObservers)
					{
					}
					
					public void playSoundToPlayers(final String clipName, final String subFolder, final Collection<PlayerID> playersToSendTo, final Collection<PlayerID> butNotThesePlayers)
					{
					}
					
					public void playSoundToPlayer(final String clipName, final String subFolder, final PlayerID playerToSendTo)
					{
					}
				};
				game.addDisplay(m_display);
				game.addSoundChannel(m_soundChannel);
				initializeGame();
				connectPlayers(players, null);// technically not needed because we won't have any "local human players" in a headless game.
			}
			else
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						final TripleAFrame frame;
						try
						{
							frame = new TripleAFrame(game, localPlayers);
						} catch (final IOException e)
						{
							e.printStackTrace();
							System.exit(-1);
							return;
						}
						m_display = new TripleaDisplay(frame);
						game.addDisplay(m_display);
						m_soundChannel = new DefaultSoundChannel(localPlayers);
						game.addSoundChannel(m_soundChannel);
						frame.setSize(700, 400);
						frame.setVisible(true);
						DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_GAME_START, null);
						connectPlayers(players, frame);
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								frame.setExtendedState(Frame.MAXIMIZED_BOTH);
								frame.toFront();
							}
						});
					}
				});
			}
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			else
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getCause().getMessage());
			}
		}
	}
	
	private void connectPlayers(final Set<IGamePlayer> players, final TripleAFrame frame)
	{
		for (final IGamePlayer player : players)
		{
			if (player instanceof TripleAPlayer)
				((TripleAPlayer) player).setFrame(frame);
		}
	}
	
	/**
	 * Return an array of player types that can play on the server.
	 */
	@Override
	public String[] getServerPlayerTypes()
	{
		return new String[] { HUMAN_PLAYER_TYPE,
					WEAK_COMPUTER_PLAYER_TYPE,
					STRONG_COMPUTER_PLAYER_TYPE,
					/* ADVANCED_COMPUTER_PLAYER_TYPE, */
					DYNAMIX_COMPUTER_PLAYER_TYPE,
					DOESNOTHINGAI_COMPUTER_PLAYER_TYPE };
	}
	
	/*
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return ITripleaDisplay.class;
	}
	
	public Class<? extends IChannelSubscribor> getSoundType()
	{
		return ISound.class;
	}
	
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return ITripleaPlayer.class;
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new IUnitFactory()
		{
			private static final long serialVersionUID = 5684926837825766505L;
			
			public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data)
			{
				return new TripleAUnit(type, owner, data);
			}
		};
	}
}
