package games.strategy.engine.chat;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

public class RecipientList implements Externalizable
{
    private final Object m_lock = new Object();
    private List<String> m_receiving = new ArrayList<String>();

    public RecipientList()
    {
    }
    public RecipientList(List<String> list)
    {
        synchronized (m_lock)
        {
            m_receiving = list;
        }
    }
	public void writeExternal(ObjectOutput out) throws IOException
	{
        out.writeByte(m_receiving.size());
        for (int i = 0; i < m_receiving.size(); i++)
        {
            out.writeObject(m_receiving.toArray()[i]);
        }
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
        byte count = in.readByte();
        m_receiving = new ArrayList<String>();
        for (int i = 0; i < count; i++)
        {
            m_receiving.add((String) in.readObject());
        }
	}

    public void add(String name)
    {
        synchronized (m_lock)
        {
            m_receiving.add(name);
        }
    }
    public void remove(String name)
    {
        synchronized (m_lock)
        {
            m_receiving.remove(name);
        }
    }

    public void ClearReceivingList()
    {
        synchronized (m_lock)
        {
            m_receiving.clear();
        }
    }
    public boolean shouldReceive(String name)
    {
        synchronized (m_lock)
        {
            return m_receiving.contains(name);
        }
    }
    public String[] RetrieveNamesInList()
    {
        synchronized (m_lock)
        {
            String[] array = new String[m_receiving.size()];
            return m_receiving.toArray(array);
        }
    }
}
