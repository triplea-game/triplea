/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.delegate;

import java.util.Properties;

import games.strategy.engine.data.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.message.*;
import games.strategy.engine.random.*;

/**
 * 
 * Default implementation of DelegateBridge
 * 
 * @author Sean Bridges
 */
public class DefaultDelegateBridge implements IDelegateBridge
{

    private final GameStep m_step;
    private PlayerID m_player;
    private final IGame m_game;
    private final DelegateHistoryWriter m_historyWriter;
    private final RandomStats m_randomStats;

    private IRandomSource m_randomSource = new PlainRandomSource();

    /** Creates new DefaultDelegateBridge */
    public DefaultDelegateBridge(GameData data, GameStep step, IGame game,
            DelegateHistoryWriter historyWriter, RandomStats randomStats)
    {
        m_step = step;
        m_game = game;
        m_player = m_step.getPlayerID();
        m_historyWriter = historyWriter;
        m_randomStats = randomStats;
    }

    public PlayerID getPlayerID()
    {
        return m_player;
    }

    public void setRandomSource(IRandomSource randomSource)
    {
        m_randomSource = randomSource;
    }

    /**
     * All delegates should use random data that comes from both players so that
     * neither player cheats.
     */
    public int getRandom(int max, String annotation)
    {
        int random = m_randomSource.getRandom(max, annotation);
        m_randomStats.addRandom(random);
        return random;
    }

    /**
     * Delegates should not use random data that comes from any other source.
     */
    public int[] getRandom(int max, int count, String annotation)
    {
        int[] rVal = m_randomSource.getRandom(max, count, annotation);
        m_randomStats.addRandom(rVal);
        return rVal;
    }

    public void addChange(Change aChange)
    {
        m_game.addChange(aChange);
    }


    public void setPlayerID(PlayerID aPlayer)
    {
        m_player = aPlayer;
    }



    /**
     * Returns the current step name
     */
    public String getStepName()
    {
        return m_step.getName();
    }



    public DelegateHistoryWriter getHistoryWriter()
    {
        return m_historyWriter;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote()
     */
    public IRemote getRemote()
    {
        return getRemote(m_player);
    }

    /*
     * @see games.strategy.engine.delegate.IDelegateBridge#getRemote(games.strategy.engine.data.PlayerID)
     */
    public IRemote getRemote(PlayerID id)
    {
        return m_game.getRemoteMessenger().getRemote(
                ServerGame.getRemoteName(id));
    }

    /* (non-Javadoc)
     * @see games.strategy.engine.delegate.IDelegateBridge#getDisplayChannelBroadcaster()
     */
    public IChannelSubscribor getDisplayChannelBroadcaster()
    {
        return m_game.getChannelMessenger().getChannelBroadcastor(ServerGame.DISPLAY_CHANNEL);
    }
    
    public Properties getStepProperties()
    {
        return m_step.getProperties();
    }
}