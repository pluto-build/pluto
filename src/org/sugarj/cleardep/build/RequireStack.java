package org.sugarj.cleardep.build;

import java.util.ArrayDeque;
import java.util.Deque;

import org.sugarj.common.path.Path;

public class RequireStack {

  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();

  protected BuildStackEntry push(BuildRequest<?, ?, ?, ?> req, Path dep) {
    BuildStackEntry entry = new BuildStackEntry(req, dep);

    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep, entry);
    }
    this.requireCallStack.push(entry);
    return entry;
  }

  protected BuildStackEntry pop() {
    BuildStackEntry poppedEntry = requireCallStack.pop();
    return poppedEntry;
  }

}
