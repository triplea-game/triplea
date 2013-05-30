package games.strategy.engine.chat;

import javax.swing.DefaultListCellRenderer;

/**
 * Not sure if this is the right way to go about it, but we need a headless version, so I'm making an interface so we can use the headless or non-headless versions as we like.
 * 
 * @author veqryn
 * 
 */
public interface IChatPanel
{
	public void shutDown();
	
	public void setChat(final Chat chat);
	
	public Chat getChat();
	
	public void setPlayerRenderer(final DefaultListCellRenderer renderer);
	
	public void setShowChatTime(final boolean showTime);
	
	public String getAllText();
}
