package co.kaush.msusf.movies.livedatatesting;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.support.annotation.NonNull;

/**
 * Credit - jraska on github
 * Copied from [https://github.com/jraska/livedata-testing]
 * Reason - The library on maven only supports AndroidX
 */
public final class TestLifecycle implements LifecycleOwner {
  private final LifecycleRegistry registry = new LifecycleRegistry(this);

  private TestLifecycle() {
  }

  public TestLifecycle create() {
    return handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
  }

  public TestLifecycle start() {
    return handleLifecycleEvent(Lifecycle.Event.ON_START);
  }

  public TestLifecycle resume() {
    return handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
  }

  public TestLifecycle pause() {
    return handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
  }

  public TestLifecycle stop() {
    return handleLifecycleEvent(Lifecycle.Event.ON_STOP);
  }

  public TestLifecycle destroy() {
    return handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
  }

  @NonNull
  public Lifecycle.State getCurrentState() {
    return registry.getCurrentState();
  }

  private TestLifecycle handleLifecycleEvent(@NonNull Lifecycle.Event event) {
    registry.handleLifecycleEvent(event);
    return this;
  }

  @NonNull
  @Override
  public Lifecycle getLifecycle() {
    return registry;
  }

  public static TestLifecycle initialized() {
    return new TestLifecycle();
  }

  public static TestLifecycle resumed() {
    return initialized().resume();
  }
}
