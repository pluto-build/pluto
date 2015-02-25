package org.sugarj.cleardep.build;

import java.util.ArrayDeque;
import java.util.Deque;

import org.sugarj.common.path.Path;

public class RequireStack {

  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();

  protected BuildStackEntry push(BuilderFactory<?, ?, ?> factory, Path dep) {
    BuildStackEntry entry = new BuildStackEntry(factory, dep);

    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep);
    }
    this.requireCallStack.push(entry);
    return entry;
  }

  protected BuildStackEntry pop() {
    BuildStackEntry poppedEntry = requireCallStack.pop();
    return poppedEntry;
  }

}
