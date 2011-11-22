package games.strategy.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

class ClientLoginHelper
{
	private final IConnectionLogin m_login;
	private final SocketStreams m_streams;
	private String m_clientName;
	
	public ClientLoginHelper(final IConnectionLogin login, final SocketStreams streams, final String clientName)
	{
		m_login = login;
		m_streams = streams;
		m_clientName = clientName;
	}
	
	@SuppressWarnings("unchecked")
	public boolean login()
	{
		try
		{
			final ObjectOutputStream out = new ObjectOutputStream(m_streams.getBufferedOut());
			out.writeObject(m_clientName);
			// write the object output streams magic number
			out.flush();
			final ObjectInputStream in = new ObjectInputStream(m_streams.getBufferedIn());
			final Map challenge = (Map) in.readObject();
			// the degenerate case
			if (challenge == null)
			{
				out.writeObject(null);
				out.flush();
				return true;
			}
			final Set<Map.Entry> entries = challenge.entrySet();
			for (final Map.Entry entry : entries)
			{
				// check what we read is a string
				if (!(entry.getKey() instanceof String) && !(entry.getValue() instanceof String))
				{
					throw new IllegalStateException("Value must be a String");
				}
			}
			if (m_login == null)
				throw new IllegalStateException("Challenged, but no login generator");
			final Map<String, String> props = m_login.getProperties(challenge);
			out.writeObject(props);
			out.flush();
			final String response = (String) in.readObject();
			if (response == null)
			{
				m_clientName = (String) in.readObject();
				return true;
			}
			m_login.notifyFailedLogin(response);
			return false;
		} catch (final Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public String getClientName()
	{
		return m_clientName;
	}
}
