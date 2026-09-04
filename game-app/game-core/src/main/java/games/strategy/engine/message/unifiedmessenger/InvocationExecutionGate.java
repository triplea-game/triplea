package games.strategy.engine.message.unifiedmessenger;

/**
 * Brackets the local dispatch of an inbound invocation at an endpoint. Delegate endpoints supply
 * the delegate-execution read lock (enter/leave) so a save cannot run while an inbound message
 * mutates game state; every other endpoint uses {@link #NONE}. This replaces the reflective inbound
 * proxy wrapper that used to acquire the lock around each delegate method call.
 */
public interface InvocationExecutionGate {
  InvocationExecutionGate NONE =
      new InvocationExecutionGate() {
        @Override
        public void enter() {}

        @Override
        public void leave() {}
      };

  void enter();

  void leave();
}
