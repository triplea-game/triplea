package games.strategy.util;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: </p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class TaskTimer
{
    private final long m_start;
    private final String m_name;

    public TaskTimer(String name)
    {
        m_name = name;
        m_start = System.currentTimeMillis();
    }

    public void done()
    {
        long done = System.currentTimeMillis();
        System.out.println(m_name + ", done in " + (done - m_start)  / 1000.0 + " s");
    }

}
