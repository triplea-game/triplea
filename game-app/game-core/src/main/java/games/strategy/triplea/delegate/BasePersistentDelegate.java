package games.strategy.triplea.delegate;

import games.strategy.engine.delegate.IPersistentDelegate;

/**
 * Base class designed to make writing custom persistent delegates simpler. Code common to all
 * persistent delegates is implemented here. Do NOT combine this class with
 * "BaseTripleADelegate.java" It is supposed to be separate, as Persistent Delegates do not do many
 * things that normal delegates do, like Triggers, etc. Persistent Delegates are active all the
 * time.
 */
@SuppressWarnings("AbstractClassWithOnlyOneDirectInheritor")
public abstract class BasePersistentDelegate extends AbstractDelegate
    implements IPersistentDelegate {}
