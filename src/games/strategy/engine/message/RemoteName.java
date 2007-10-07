package games.strategy.engine.message;

/**
 * 
 * Description for a Channel or a Remote end point.
 * 
 * @author Sean Bridges
 */
public class RemoteName
{
    private final String m_name;
    private final Class<?> m_class;
    
    public RemoteName(final Class<?> class1, final String name)
    {
        this(name,class1);
    }
    
    public RemoteName(final String name, final Class<?> class1)
    {
        if(!class1.isInterface())
            throw new IllegalArgumentException("Not an interface");
        
        m_name = name;
        m_class = class1;
    }
    
    public Class<?> getClazz()
    {
        return m_class;
    }
    public String getName()
    {
        return m_name;
    }
    
    public String toString()
    {
        return m_name + ":" + m_class.getSimpleName();
    }
    
}
