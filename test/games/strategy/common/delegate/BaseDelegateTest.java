package games.strategy.common.delegate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import games.strategy.engine.message.IRemote;

public class BaseDelegateTest {

  final TestBaseDelegate delegate = new TestBaseDelegate();

  @Test
  public void start() {
    assertThat(delegate.getTriggeredOnStart(), is(equalTo(false)));
    delegate.start();
    assertThat(delegate.getTriggeredOnStart(), is(equalTo(true)));
  }

  @Test
  public void end() {
    assertThat(delegate.getTriggeredOnEnd(), is(equalTo(false)));
    delegate.end();
    assertThat(delegate.getTriggeredOnEnd(), is(equalTo(true)));
  }

  @Test
  public void saveState() {
    final BaseDelegateState saveState = (BaseDelegateState) delegate.saveState();
    assertThat(saveState.m_startBaseStepsFinished, is(equalTo(false)));
    assertThat(saveState.m_endBaseStepsFinished, is(equalTo(false)));
  }

  @Test
  public void loadState() {
    final BaseDelegateState setState = new BaseDelegateState();
    setState.m_startBaseStepsFinished = true;
    setState.m_endBaseStepsFinished = true;
    delegate.loadState(setState);
    final BaseDelegateState newState = (BaseDelegateState) delegate.saveState();
    assertThat(setState.m_startBaseStepsFinished, is(equalTo(newState.m_startBaseStepsFinished)));
    assertThat(setState.m_endBaseStepsFinished, is(equalTo(newState.m_endBaseStepsFinished)));
  }

  class TestBaseDelegate extends BaseDelegate {

    boolean triggeredOnStart = false;

    public boolean getTriggeredOnStart() {
      return triggeredOnStart;
    }

    boolean triggeredOnEnd = false;

    public boolean getTriggeredOnEnd() {
      return triggeredOnEnd;
    }

    @Override
    protected void triggerOnStart() {
      super.triggerOnStart();
      triggeredOnStart = true;
    }

    @Override
    protected void triggerOnEnd() {
      super.triggerOnEnd();
      triggeredOnEnd = true;
    }

    @Override
    public boolean delegateCurrentlyRequiresUserInput() {
      return false;
    }

    @Override
    public Class<? extends IRemote> getRemoteType() {
      return null;
    }

  }

}
