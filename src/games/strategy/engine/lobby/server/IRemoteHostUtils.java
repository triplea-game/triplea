package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemote;

/**
 * 
 * @author veqryn
 * 
 */
public interface IRemoteHostUtils extends IRemote
{
	public String banPlayerHeadlessHostBot(String playerNameToBeBanned, String hashedPassword, String salt);
	
	public String stopGameHeadlessHostBot(String hashedPassword, String salt);
	
	public String shutDownHeadlessHostBot(String hashedPassword, String salt);
	
	public String getSalt();
}
