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
package games.strategy.engine.lobby.server;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.HeadlessGameServer;
import games.strategy.net.INode;
import games.strategy.net.Node;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;

/**
 * 
 * NOTE - this class is not thread safe. Modifications should be done holding an
 * external lock.
 * 
 * @author sgb
 */
public class GameDescription implements Externalizable, Cloneable
{
	private static final long serialVersionUID = 508593169141567546L;
	
	
	public enum GameStatus
	{
		LAUNCHING
		{
			@Override
			public String toString()
			{
				return "Launching";
			}
		},
		IN_PROGRESS
		{
			@Override
			public String toString()
			{
				return "In Progress";
			}
		},
		WAITING_FOR_PLAYERS
		{
			@Override
			public String toString()
			{
				return "Waiting For Players";
			}
		}
	}
	
	private INode m_hostedBy;
	private int m_port;
	private Date m_startDateTime;
	private String m_gameName;
	private int m_playerCount;
	private String m_round;
	private GameStatus m_status;
	private int m_version = Integer.MIN_VALUE;
	private String m_hostName;
	private String m_comment;
	
	private boolean m_passworded;
	private String m_engineVersion;
	private String m_gameVersion;
	
	private String m_botSupportEmail = HeadlessGameServer.getInstance() != null ? System.getProperty(GameRunner2.LOBBY_GAME_SUPPORT_EMAIL, "") : "";
	
	// if you add a field, add it to write/read object as well for Externalizable
	public GameDescription()
	{
	}
	
	public GameDescription(final INode hostedBy, final int port, final Date startDateTime, final String gameName, final int playerCount, final GameStatus status, final String round,
				final String hostName, final String comment, final boolean passworded, final String engineVersion, final String gameVersion)
	{
		m_hostName = hostName;
		m_hostedBy = hostedBy;
		m_port = port;
		m_startDateTime = startDateTime;
		m_gameName = gameName;
		m_playerCount = playerCount;
		m_status = status;
		m_round = round;
		m_comment = comment;
		m_passworded = passworded;
		m_engineVersion = engineVersion;
		m_gameVersion = gameVersion;
	}
	
	@Override
	public Object clone()
	{
		try
		{
			return super.clone();
		} catch (final CloneNotSupportedException e)
		{
			throw new IllegalStateException("how did that happen");
		}
	}
	
	/**
	 * The version number is updated after every change. This handles
	 * synchronization problems where updates arrive out of order
	 * 
	 */
	public int getVersion()
	{
		return m_version;
	}
	
	public void setGameName(final String gameName)
	{
		m_version++;
		m_gameName = gameName;
	}
	
	public void setHostedBy(final INode hostedBy)
	{
		m_version++;
		m_hostedBy = hostedBy;
	}
	
	public void setPlayerCount(final int playerCount)
	{
		m_version++;
		m_playerCount = playerCount;
	}
	
	public void setPort(final int port)
	{
		m_version++;
		m_port = port;
	}
	
	public void setRound(final String round)
	{
		m_version++;
		m_round = round;
	}
	
	public void setStartDateTime(final Date startDateTime)
	{
		m_version++;
		m_startDateTime = startDateTime;
	}
	
	public void setStatus(final GameStatus status)
	{
		m_version++;
		m_status = status;
	}
	
	public void setPassworded(final boolean passworded)
	{
		m_version++;
		m_passworded = passworded;
	}
	
	public boolean getPassworded()
	{
		return m_passworded;
	}
	
	public void setEngineVersion(final String engineVersion)
	{
		m_version++;
		m_engineVersion = engineVersion;
	}
	
	public void setGameVersion(final String gameVersion)
	{
		m_version++;
		m_gameVersion = gameVersion;
	}
	
	public String getEngineVersion()
	{
		return m_engineVersion;
	}
	
	public String getGameVersion()
	{
		return m_gameVersion;
	}
	
	public String getBotSupportEmail()
	{
		return m_botSupportEmail;
	}
	
	public String getRound()
	{
		return m_round;
	}
	
	public String getGameName()
	{
		return m_gameName;
	}
	
	public INode getHostedBy()
	{
		return m_hostedBy;
	}
	
	public int getPlayerCount()
	{
		return m_playerCount;
	}
	
	public int getPort()
	{
		return m_port;
	}
	
	public Date getStartDateTime()
	{
		return m_startDateTime;
	}
	
	public GameStatus getStatus()
	{
		return m_status;
	}
	
	public String getHostName()
	{
		return m_hostName;
	}
	
	public void setHostName(final String hostName)
	{
		m_version++;
		m_hostName = hostName;
	}
	
	public String getComment()
	{
		return m_comment;
	}
	
	public void setComment(final String comment)
	{
		m_version++;
		m_comment = comment;
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		m_hostedBy = new Node();
		((Node) m_hostedBy).readExternal(in);
		m_port = in.readInt();
		m_startDateTime = new Date();
		m_startDateTime.setTime(in.readLong());
		m_playerCount = in.readByte();
		m_round = in.readUTF();
		m_status = GameStatus.values()[in.readByte()];
		m_version = in.readInt();
		m_hostName = in.readUTF();
		m_comment = in.readUTF();
		m_gameName = in.readUTF();
		m_passworded = in.readBoolean();
		m_engineVersion = in.readUTF();
		m_gameVersion = in.readUTF();
		m_botSupportEmail = in.readUTF();
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		((Node) m_hostedBy).writeExternal(out);
		out.writeInt(m_port);
		out.writeLong(m_startDateTime.getTime());
		out.writeByte(m_playerCount);
		out.writeUTF(m_round);
		out.writeByte(m_status.ordinal());
		out.writeInt(m_version);
		out.writeUTF(m_hostName);
		out.writeUTF(m_comment);
		out.writeUTF(m_gameName);
		out.writeBoolean(m_passworded);
		out.writeUTF(m_engineVersion);
		out.writeUTF(m_gameVersion);
		out.writeUTF(m_botSupportEmail);
	}
	
	@Override
	public String toString()
	{
		return "Game Hosted By:" + m_hostName + " gameName:" + m_gameName + " at:" + m_hostedBy.getAddress() + ":" + m_port + " playerCount:" + m_playerCount;
	}
}
