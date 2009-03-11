package games.strategy.util;

public class Triple<F,S,T> extends Tuple<F,S> 
{
    private final T m_third;
    
    public Triple(F first, S second, T third)
    {
        super(first, second);
        m_third = third;
    }
    
    public T getThird()
    {
        return m_third;
    }
    
    
}
