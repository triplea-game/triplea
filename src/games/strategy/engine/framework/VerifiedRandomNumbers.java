/*
 * Created on Feb 18, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package games.strategy.engine.framework;

import games.strategy.triplea.formatter.Formatter;

/**
 * @author sgb
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class VerifiedRandomNumbers
{
    private final int[] m_values;
    private final String m_annotation;
    
    public VerifiedRandomNumbers(String annotation, int[] values)
    {
        m_values = values;
        m_annotation = annotation;
    }
    
    public String toString()
    {
        return "Rolled :" +  Formatter.asDice(m_values) + " for " + m_annotation;
    }
    /**
     * @return Returns the m_annotation.
     */
    public String getAnnotation()
    {
        return m_annotation;
    }
    /**
     * @return Returns the m_values.
     */
    public int[] getValues()
    {
        return m_values;
    }
}
