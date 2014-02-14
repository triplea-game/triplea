package games.strategy.engine.lobby.server;

import games.strategy.engine.message.IRemote;

/**
 * 
 * @author veqryn
 * 
 */
public interface IRemoteHostUtils extends IRemote
{
	public String getConnections();
	
	public String getChatLogHeadlessHostBot(String hashedPassword, String salt);
	
	public String mutePlayerHeadlessHostBot(String playerNameToBeMuted, int minutes, String hashedPassword, String salt);
	
	public String bootPlayerHeadlessHostBot(String playerNameToBeBooted, String hashedPassword, String salt);
	
	public String banPlayerHeadlessHostBot(String playerNameToBeBanned, int hours, String hashedPassword, String salt);
	
	public String stopGameHeadlessHostBot(String hashedPassword, String salt);
	
	public String shutDownHeadlessHostBot(String hashedPassword, String salt);
	
	public String getSalt();
}
