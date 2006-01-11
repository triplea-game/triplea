package games.strategy.engine.delegate;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Manages when delegates are allowed to execute.<p>
 * 
 * When saving a game, we want to ensure that no delegate is executing, otherwise
 * the delegate could modify the state of the game while the game is being saved, resulting
 * in an invalid save game.<p>
 * 
 * This class effectivly keeps a count of how many threads are executing in the delegates,
 * and provides a way of blocking further threads from starting execution in a delegate.<p> 
 * 
 * 
 * @author sgb
 */
public class DelegateExecutionManager
{

    /*
     * Delegate execution can be thought of as a read/write lock.
     * Many delegates can be executing at one time (to execute you acquire the read lock), but
     * only 1 block can be held (the block is equivalent to the read lock).
     * 
     */
    private final ReentrantReadWriteLock m_readWriteLock = new ReentrantReadWriteLock(true);
    private ThreadLocal<Boolean> m_currentThreadHasReadLock = new ThreadLocal<Boolean>();
    
    
    /**
     *
     * When this method returns true, threads will not be able to enter delegates until
     * a call to resumeDelegateExecution is made.<p>
     * 
     * When delegateExecution is blocked, it also blocks subsequent cals to blockDelegateExecution(...)<p>
     * 
     * If timeToWaitMS is > 0, we will give up trying to block delegate execution after
     * timeTiWaitMS has elapsed.<p> 
     * 
     * @param timeToWait
     * @return
     */
    public boolean blockDelegateExecution(int timeToWaitMS ) throws InterruptedException
    {
        return m_readWriteLock.writeLock().tryLock(timeToWaitMS, TimeUnit.MILLISECONDS);       
    }
    
    
    /**
     * Allow delegate execution to resume. 
     *
     */
    public void resumeDelegateExecution()
    {
        m_readWriteLock.writeLock().unlock();
    }
    
    private boolean currentThreadHasReadLock()
    {
        return m_currentThreadHasReadLock.get() == Boolean.TRUE;
    }
    
    /**
     * Used to create an object the exits delegate execution.<p>
     * 
     * Objects on this method will decrement the thread lock count when called, and will
     * increment it again when execution is finished. 
     */
    public Object createOutboundImplementation(final Object implementor, Class[] interfaces)
    {
                
        InvocationHandler ih = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                
                if(currentThreadHasReadLock())
                    m_readWriteLock.readLock().unlock();
                try
                {
                    return method.invoke(implementor, args);
                }
                finally
                {
                    if(currentThreadHasReadLock())
                        m_readWriteLock.readLock().lock();
                }
            }
        };
        
     
        return Proxy.newProxyInstance( implementor.getClass().getClassLoader(), interfaces,  ih);
        
    }
    
    /**
     * Use to create an object that begins delegate execution.<p>
     * 
     * Objects on this method will increment the thread lock count when called, and will
     * decrement it again when execution is finished. 
     */
    public Object createInboundImplementation(final Object implementor, Class[] interfaces)
    {
                
        InvocationHandler ih = new InvocationHandler()
        {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                enterDelegateExecution();
                
                
                try
                {
                    return method.invoke(implementor, args);
                }
                finally
                {
                    leaveDelegateExecution();
                }
            }
        };
        
     
        return Proxy.newProxyInstance( implementor.getClass().getClassLoader(), interfaces,  ih);
    }     
    
    public void leaveDelegateExecution()
    {
        m_readWriteLock.readLock().unlock();
        m_currentThreadHasReadLock.set(null);
    }

    public void enterDelegateExecution()
    {
        if(currentThreadHasReadLock())
            throw new IllegalStateException("Already locked?");

        m_readWriteLock.readLock().lock();
        m_currentThreadHasReadLock.set(Boolean.TRUE);        
    }

    
}
