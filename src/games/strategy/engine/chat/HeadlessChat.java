package games.strategy.engine.chat;

import games.strategy.engine.chat.Chat.CHAT_SOUND_PROFILE;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.INode;
import games.strategy.net.ServerMessenger;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.SoundPath;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;

/**
 * Headless version of ChatPanel.
 * 
 * @author veqryn
 * 
 */
public class HeadlessChat implements IChatListener, IChatPanel
{
	private static final int MAX_LENGTH = 1000 * 200; // roughly 1000 chat messages
	private Chat m_chat;
	private boolean m_showTime = true;
	private StringBuffer m_allText = new StringBuffer();
	private String m_lastText = "";
	private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("'('HH:mm:ss')'");
	private final ChatFloodControl floodControl = new ChatFloodControl();
	public Set<String> m_hiddenPlayers = new HashSet<String>();
	private final Set<INode> m_players = new HashSet<INode>();
	private PrintStream m_out = null;
	private final IStatusListener m_statusListener = new IStatusListener()
	{
		public void statusChanged(final INode node, final String newStatus)
		{ // nothing for now
		}
	};
	
	public HeadlessChat(final IMessenger messenger, final IChannelMessenger channelMessenger, final IRemoteMessenger remoteMessenger, final String chatName, final CHAT_SOUND_PROFILE chatSoundProfile)
	{
		final Chat chat = new Chat(messenger, chatName, channelMessenger, remoteMessenger, chatSoundProfile);
		setChat(chat);
	}
	
	public HeadlessChat(final Chat chat)
	{
		setChat(chat);
	}
	
	public Set<INode> getAllChatters()
	{
		return new HashSet<INode>(m_players);
	}
	
	public void setPrintStream(final PrintStream out)
	{
		m_out = out;
	}
	
	@Override
	public String toString()
	{
		return m_allText.toString();
	}
	
	public String getAllText()
	{
		return m_allText.toString();
	}
	
	public String getLastText()
	{
		return m_lastText;
	}
	
	public Chat getChat()
	{
		return m_chat;
	}
	
	public void setShowChatTime(final boolean showTime)
	{
		m_showTime = showTime;
	}
	
	public void setPlayerRenderer(final DefaultListCellRenderer renderer)
	{ // nothing
	}
	
	public synchronized void updatePlayerList(final Collection<INode> players)
	{
		m_players.clear();
		for (final INode name : players)
		{
			if (!m_hiddenPlayers.contains(name.getName()))
				m_players.add(name);
		}
	}
	
	public void addHiddenPlayerName(final String name)
	{
		m_hiddenPlayers.add(name);
	}
	
	public void shutDown()
	{
		if (m_chat != null)
		{
			m_chat.removeChatListener(this);
			m_chat.getStatusManager().removeStatusListener(m_statusListener);
			m_chat.shutdown();
		}
		m_chat = null;
	}
	
	public void setChat(final Chat chat)
	{
		if (m_chat != null)
		{
			m_chat.removeChatListener(this);
			m_chat.getStatusManager().removeStatusListener(m_statusListener);
		}
		m_chat = chat;
		if (m_chat != null)
		{
			m_chat.addChatListener(this);
			m_chat.getStatusManager().addStatusListener(m_statusListener);
			synchronized (m_chat.getMutex())
			{
				m_allText = new StringBuffer();
				m_lastText = "";
				try
				{
					if (m_out != null)
						m_out.println();
				} catch (final Exception e)
				{
				}
				for (final ChatMessage message : m_chat.getChatHistory())
				{
					if (message.getFrom().equals(m_chat.getServerNode().getName()))
					{
						if (message.getMessage().equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_LOBBY))
						{
							addChatMessage("YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER", "ADMIN_CHAT_CONTROL", false);
							continue;
						}
						else if (message.getMessage().equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_GAME))
						{
							addChatMessage("YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST", "HOST_CHAT_CONTROL", false);
							continue;
						}
					}
					addChatMessage(message.getMessage(), message.getFrom(), message.isMeMessage());
				}
			}
		}
		else
		{
			updatePlayerList(Collections.<INode> emptyList());
		}
	}
	
	/** thread safe */
	public void addMessage(final String message, final String from, final boolean thirdperson)
	{
		addMessageWithSound(message, from, thirdperson, SoundPath.CLIP_CHAT_MESSAGE);
	}
	
	/** thread safe */
	public void addMessageWithSound(final String message, final String from, final boolean thirdperson, final String sound)
	{
		// TODO: I don't really think we need a new thread for this...
		final Thread t = new Thread(new Runnable()
		{
			public void run()
			{
				if (from.equals(m_chat.getServerNode().getName()))
				{
					if (message.equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_LOBBY))
					{
						addChatMessage("YOUR LOBBY CHATTING HAS BEEN TEMPORARILY 'MUTED' BY THE ADMINS, TRY AGAIN LATER", "ADMIN_CHAT_CONTROL", false);
						return;
					}
					else if (message.equals(ServerMessenger.YOU_HAVE_BEEN_MUTED_GAME))
					{
						addChatMessage("YOUR CHATTING IN THIS GAME HAS BEEN 'MUTED' BY THE HOST", "HOST_CHAT_CONTROL", false);
						return;
					}
				}
				if (!floodControl.allow(from, System.currentTimeMillis()))
				{
					if (from.equals(m_chat.getLocalNode().getName()))
					{
						addChatMessage("MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", "ADMIN_FLOOD_CONTROL", false);
					}
					return;
				}
				addChatMessage(message, from, thirdperson);
				DefaultSoundChannel.playSoundOnLocalMachine(sound, null);
			}
		});
		t.start();
	}
	
	private void addChatMessage(final String originalMessage, final String from, final boolean thirdperson)
	{
		final String message = trimMessage(originalMessage);
		final String time = simpleDateFormat.format(new Date());
		final String prefix = thirdperson ? (m_showTime ? "* " + time + " " + from : "* " + from) : (m_showTime ? time + " " + from + ": " : from + ": ");
		final String fullMessage = prefix + " " + message + "\n";
		m_lastText = fullMessage;
		final String currentAllText = m_allText.toString();
		if (currentAllText.length() > MAX_LENGTH)
			m_allText = new StringBuffer(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
		m_allText.append(fullMessage);
		try
		{
			if (m_out != null)
				m_out.print("CHAT: " + fullMessage);
		} catch (final Exception e)
		{
		}
	}
	
	public void addServerMessage(final String message)
	{
		final String fullMessage = "Server Message: \n" + message + "\n";
		m_lastText = fullMessage;
		final String currentAllText = m_allText.toString();
		if (currentAllText.length() > MAX_LENGTH)
			m_allText = new StringBuffer(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
		m_allText.append(fullMessage);
		try
		{
			if (m_out != null)
				m_out.print("CHAT: " + fullMessage);
		} catch (final Exception e)
		{
		}
	}
	
	public void addStatusMessage(final String message)
	{
		final String fullMessage = "--- " + message + " ---\n";
		m_lastText = fullMessage;
		final String currentAllText = m_allText.toString();
		if (currentAllText.length() > MAX_LENGTH)
			m_allText = new StringBuffer(currentAllText.substring(MAX_LENGTH / 2, currentAllText.length()));
		m_allText.append(fullMessage);
		try
		{
			if (m_out != null)
				m_out.print("CHAT: " + fullMessage);
		} catch (final Exception e)
		{
		}
	}
	
	private String trimMessage(final String originalMessage)
	{
		// dont allow messages that are too long
		if (originalMessage.length() > 200)
		{
			return originalMessage.substring(0, 199) + "...";
		}
		else
		{
			return originalMessage;
		}
	}
	
	public String getPlayerDisplayString(final INode node)
	{
		if (m_chat == null)
			return "";
		String extra = "";
		final String notes = m_chat.getNotesForNode(node);
		if (notes != null && notes.length() > 0)
		{
			extra = extra + notes;
		}
		String status = m_chat.getStatusManager().getStatus(node);
		final StringBuilder statusSB = new StringBuilder("");
		if (status != null && status.length() > 0)
		{
			if (status.length() > 25)
			{
				status = status.substring(0, 25);
			}
			for (int i = 0; i < status.length(); i++)
			{
				final char c = status.charAt(i);
				// skip combining characters
				if (c >= '\u0300' && c <= '\u036F')
				{
					continue;
				}
				statusSB.append(c);
			}
			extra = extra + " (" + statusSB.toString() + ")";
		}
		if (extra.length() == 0)
		{
			return node.getName();
		}
		return node.getName() + extra;
	}
	
	public void ignorePlayer(final INode player)
	{
		final boolean isIgnored = m_chat.isIgnored(player);
		if (!isIgnored)
			m_chat.setIgnored(player, true);
	}
	
	public void stopIgnoringPlayer(final INode player)
	{
		final boolean isIgnored = m_chat.isIgnored(player);
		if (isIgnored)
			m_chat.setIgnored(player, false);
	}
	
	public void slapPlayer(final INode player)
	{
		m_chat.sendSlap(player.getName());
	}
	
	public void slapPlayer(final String playerName)
	{
		m_chat.sendSlap(playerName);
	}
}
