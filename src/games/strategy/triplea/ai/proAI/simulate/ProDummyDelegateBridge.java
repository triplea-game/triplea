package games.strategy.triplea.ai.proAI.simulate;

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
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangePerformer;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.sound.DummySoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.ui.display.DummyTripleaDisplay;

import java.util.Properties;

public class ProDummyDelegateBridge implements IDelegateBridge
{
	private final PlainRandomSource m_randomSource = new PlainRandomSource();
	private final DummyTripleaDisplay m_display = new DummyTripleaDisplay();
	private final DummySoundChannel m_soundChannel = new DummySoundChannel();
	private final PlayerID m_player;
	private final ProAI m_proAI;
	private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new ProDummyGameModifiedChannel());
	private final GameData m_data;
	private final ChangePerformer m_changePerformer;
	private final CompositeChange m_allChanges = new CompositeChange();
	private MustFightBattle m_battle = null;
	
	public ProDummyDelegateBridge(final ProAI proAI, final PlayerID player, final GameData data)
	{
		m_proAI = proAI;
		m_data = data;
		m_player = player;
		m_changePerformer = new ChangePerformer(m_data);
	}
	
	public GameData getData()
	{
		return m_data;
	}
	
	public void leaveDelegateExecution()
	{
	}
	
	public Properties getStepProperties()
	{
		throw new UnsupportedOperationException();
	}
	
	public String getStepName()
	{
		throw new UnsupportedOperationException();
	}
	
	public IRemotePlayer getRemotePlayer(final PlayerID id)
	{
		return m_proAI;
	}
	
	public IRemotePlayer getRemotePlayer()
	{
		return m_proAI;
	}
	
	public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_randomSource.getRandom(max, count, annotation);
	}
	
	public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return m_randomSource.getRandom(max, annotation);
	}
	
	public PlayerID getPlayerID()
	{
		return m_player;
	}
	
	public IDelegateHistoryWriter getHistoryWriter()
	{
		return m_writer;
	}
	
	public IDisplay getDisplayChannelBroadcaster()
	{
		return m_display;
	}
	
	public ISound getSoundChannelBroadcaster()
	{
		return m_soundChannel;
	}
	
	public void enterDelegateExecution()
	{
	}
	
	public void addChange(final Change aChange)
	{
		m_allChanges.add(aChange);
		m_changePerformer.perform(aChange);
	}
	
	public void stopGameSequence()
	{
	}
	
	public MustFightBattle getBattle()
	{
		return m_battle;
	}
	
	public void setBattle(final MustFightBattle battle)
	{
		m_battle = battle;
	}
	
}
