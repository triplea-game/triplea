package games.strategy.engine.framework.startup.mc;

public interface RemoteModelListener
{
    /**
     * The players available have changed.
     */
    public void playerListChanged();
    
    /**
     * The players taken have changed
     */
    public void playersTakenChanged();
    
    
    public static RemoteModelListener NULL_LISTENER = new RemoteModelListener( )
    {
        public void playerListChanged() {}
        public void playersTakenChanged() {}
    };
}
