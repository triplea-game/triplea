package games.strategy.triplea.baseAI;

import java.util.*;
import java.util.logging.*;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.player.ITripleaPlayer;

/**
 * Base class for ais.<p>
 * 
 *  run with -Dtriplea.abstractai.pause=false to eliminate the human friendly pauses <p>
 * 
 * AI's should note that any data that is stored in the ai instance, will be lost when
 * the game is restarted.  We cannot save data with an AI, since the player
 * may choose to restart the game with a different ai, or with a human player.<p>
 * 
 * If an ai finds itself starting in the middle of a move phase, or the middle of a purchase phase,
 * (as would happen if a player saved the game during the middle of an AI's move phase)
 * it is acceptable for the ai to play badly for a turn, but the ai should recover, and play
 * correctly when the next phase of the game starts.<p>
 * 
 * 
 * @author sgb
 */
public abstract class AbstractAI implements ITripleaPlayer
{
    private final static Logger s_logger = Logger.getLogger(AbstractAI.class.getName());
    
    private static boolean m_pause =  Boolean.valueOf(System.getProperties().getProperty("triplea.abstractai.pause", "true"));
    
    /**
     * Pause the game to allow the human player to see what is going on.
     *
     */
    protected static void pause()
    {
        if(m_pause)
        {
            try
            {
                Thread.sleep(700);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
    
    
    private final String m_name;
    private IPlayerBridge m_bridge;
    private PlayerID m_id;
    
    
    /** 
     * @param name - the name of the player.
     */
    public AbstractAI(String name)
    {
        m_name = name;
    }

    
    public final void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
    }
    
    /************************
     * Allow the AI to get game data, playerID
     *************************/
    
    /**
     * Get the GameData for the game.
     */
    protected final GameData getGameData()
    {
        return m_bridge.getGameData();
    }
    
    /**
     * Get the IPlayerBridge for this game player.
     */
    protected final IPlayerBridge getPlayerBridge()
    {
        return m_bridge;
    }
    
    /**
     * What player are we playing.
     */
    protected final PlayerID getWhoAmI()
    {
        return m_id;
    }
    
    /************************
     * The following methods are called when the AI starts a phase.
     *************************/
    
    /**
     * It is the AI's turn to purchase units.
     * 
     * @param purcahseForBid - is this a bid purchase, or a normal purchase
     * @param ipcsToSpend - how many ipcs we have to spend
     * @param purchaseDelegate - the purchase delgate to buy things with
     * @param data - the GameData
     * @param player - the player to buy for
     */
    protected abstract void purchase(boolean purcahseForBid, int ipcsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player);

    /**
     * It is the AI's turn to roll for technology.
     * 
     * @param techDelegate - the tech delegate to roll for
     * @param data - the game data
     * @param player - the player to roll tech for
     */
    protected abstract void tech(ITechDelegate techDelegate, GameData data, PlayerID player);
    
    /**
     * It is the AI's turn to move.  Make all moves before returning from this method.
     *  
     * @param nonCombat - are we moving in combat, or non combat
     * @param moveDel - the move delegate to make moves with
     * @param data - the current game data
     * @param player - the player to move with
     */
    protected abstract void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player);
    
    /**
     * It is the AI's turn to place units.  get the units available to place with player.getUnits()
     * 
     * @param placeForBid - is this a placement for bid
     * @param placeDelegate - the place delegate to place with
     * @param data - the current Game Data
     * @param player - the player to place for
     */
    protected abstract void place(boolean placeForBid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player);

    
    /**
     * It is the AI's turn to fight. Subclasses may override this if they want, but
     * generally the AI does not need to worry about the order of fighting battles.
     * 
     * @param battleDelegate the battle delegate to query for battles not fought and the 
     * @param data - the current GameData
     * @param player - the player to fight for
     */
    protected void battle(IBattleDelegate battleDelegate, GameData data, PlayerID player)
    {
        //generally all AI's will follow the same logic.  
        
        //loop until all battles are fought.
        //rather than try to analyze battles to figure out which must be fought before others
        //as in the case of a naval battle preceding an amphibious attack,
        //keep trying to fight every battle
        while (true)
        {
    
            BattleListing listing = battleDelegate.getBattles();

            //all fought
            if(listing.getBattles().isEmpty() && listing.getStrategicRaids().isEmpty())
                return;
            
            Iterator<Territory> raidBattles = listing.getStrategicRaids().iterator();

            //fight strategic bombing raids
            while(raidBattles.hasNext())
            {
                Territory current = raidBattles.next();
                String error = battleDelegate.fightBattle(current, true);
                if(error != null)
                    s_logger.fine(error);
            }
            
            
            Iterator<Territory> nonRaidBattles = listing.getBattles().iterator();

            //fight normal battles
            while(nonRaidBattles.hasNext())
            {
                Territory current = nonRaidBattles.next();
                String error = battleDelegate.fightBattle(current, false);
                if(error != null)
                    s_logger.fine(error);
            }
        }
    }
    
    /******************************************
     * The following methods the AI may choose to implemenmt,
     * but in general won't
     * 
     *******************************************/
    
    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {       
        //return the first one
        return (Territory) territories.iterator().next();
    }
    
    public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from)
    {   
        //just use the first one
        return candidates.iterator().next();
    }
    
    public boolean confirmMoveKamikaze()
    {
        return false;
    }

    
    /*****************************************
     * The following methods are more for the ui, and the 
     * ai will generally not care
     * 
     *****************************************/

    public void battleInfoMessage(String shortMessage, DiceRoll dice)
    {}

    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
    {}

    public void retreatNotificationMessage(Collection<Unit> units)
    {}

    public void reportError(String error)
    {}

    public void reportMessage(String message)
    {}


    public void confirmOwnCasualties(GUID battleId, String message)
    {
        pause();
    }

    
    /*****************************************
     * Game Player Methods
     * 
     *****************************************/
    
    /**
     * The given phase has started.  We parse the phase name and call the apropiate method.
     */
    public final void start(String name)
    {
        if (name.endsWith("Bid"))
        {
            String propertyName = m_id.getName() + " bid";
            int bidAmount = Integer.parseInt(m_bridge.getGameData().getProperties().get(propertyName).toString());
            
            purchase(true,bidAmount, (IPurchaseDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
        }
        else if (name.endsWith("Tech"))
            tech((ITechDelegate) m_bridge.getRemote() , m_bridge.getGameData(), m_id);
        else if (name.endsWith("Purchase"))
        {
            
            Resource ipcs = m_bridge.getGameData().getResourceList().getResource(Constants.IPCS);
            int leftToSpend = m_id.getResources().getQuantity(ipcs );
            
            purchase(false,leftToSpend, (IPurchaseDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
        }
        else if (name.endsWith("Move"))
            move(name.endsWith("NonCombatMove"), (IMoveDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
        else if (name.endsWith("Battle"))
            battle((IBattleDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id);
        else if (name.endsWith("Place"))
            place(name.indexOf("Bid") != -1, (IAbstractPlaceDelegate) m_bridge.getRemote(), m_bridge.getGameData(), m_id );
        else if (name.endsWith("EndTurn"))
        {}
    }
    
    public final Class<ITripleaPlayer> getRemotePlayerType()
    {
        return ITripleaPlayer.class;
    }
    
    public final String getName()
    {
        return m_name;
    }

    public final PlayerID getID()
    {
        return m_id;
    }
    
}
