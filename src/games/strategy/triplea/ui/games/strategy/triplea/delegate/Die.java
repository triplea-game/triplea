package games.strategy.triplea.delegate;



/**
 * A single roll of a die.
 * 
 */
public class Die
{
    public static enum DieType {MISS, HIT, IGNORED}

    private final DieType m_type;
    //the value of the dice, 0 based
    private final int m_value;
    //this value is 1 based
    private final int m_rolledAt;

    
    
    public Die(int value)
    {
        this(value, -1, DieType.MISS);
    }
    
    public Die(int value, int rolledAt, DieType type)
    {
        m_type = type;
        m_value = value;
        m_rolledAt = rolledAt;
    }
    
    public Die.DieType getType()
    {
       return m_type;
    }

    public int getValue()
    {
        return m_value;
    }
    
    public int getRolledAt()
    {
        return m_rolledAt;
    }
   
    //compress to an int
    //we write a lot of dice over the network and to the saved
    //game, so we want to make this fairly efficient
    int getCompressedValue()
    {
        if(m_value > 255 || m_rolledAt > 255)
            throw new IllegalStateException("too big to serialize");
        
        return (m_rolledAt << 8) + (m_value << 16) + (m_type.ordinal());
    }
    
    //read from an int
    static Die getFromWriteValue(int value)
    {
        int rolledAt = (value & 0x0FF00) >> 8; 
        int roll = (value & 0x0FF0000) >> 16;
        DieType type = DieType.values()[(value & 0x0F)];
        return new Die(roll, rolledAt, type);
    }
    
    public boolean equals(Object o)
    {
        if(!(o instanceof Die))
            return false;
        Die other = (Die) o;
        return other.m_type == this.m_type &&
               other.m_value == this.m_value &&
               other.m_rolledAt == this.m_rolledAt;
    }
    
    public int hashCode()
    {
        return m_value + 37 * m_rolledAt;
    }
    
    public String toString()
    {
        return "Die roll" + m_value + " rolled at:" + m_rolledAt + " type:" + m_type;
    }
        
}
