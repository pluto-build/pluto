package org.sugarj.cleardep;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildSchedule.ScheduleMode;
import org.sugarj.cleardep.BuildSchedule.Task;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.path.RelativePath;

public class BuildScheduleBuilder {

  private Set<BuildUnit> unitsToCompile;
  private ScheduleMode scheduleMode;

  public BuildScheduleBuilder(Set<BuildUnit> unitsToCompile, ScheduleMode mode) {
    this.scheduleMode = mode;
    this.unitsToCompile = unitsToCompile;
  }

  private boolean needToCheckDependencies(BuildUnit dep) {
    return this.scheduleMode == ScheduleMode.REBUILD_ALL || !dep.isPersisted() || !dep.isConsistentShallow(null);
  }

  public void updateDependencies(DependencyExtractor extractor) {
    // Find all dependencies which have changed
    // We need only units with changed source files, then dependencies may have
    // changed
    // Actually the units do not need to be consistent to e.g. generated files
    Set<BuildUnit> changedUnits = new HashSet<>();
    for (BuildUnit unit : this.unitsToCompile) {
      changedUnits.addAll(BuildUnitUtils.findUnitsWithChangedSourceFiles(unit));
    }

    // Set for cycle detection and fast contains check
    Set<BuildUnit> visitedUnits = new HashSet<>();
    // Queue of units which have to be processed
    List<BuildUnit> units = new LinkedList<>(changedUnits);

    // Filter out root units which are consistent -> no need to check them
    for (BuildUnit unit : this.unitsToCompile) {
      if (needToCheckDependencies(unit)) {
        units.add(unit);
      }
    }

    // But mark all root units as seen to avoid multiple consistency checks
    visitedUnits.addAll(changedUnits);
    visitedUnits.addAll(this.unitsToCompile);


    while (!units.isEmpty()) {
      BuildUnit changedUnit = units.remove(0);
      Set<BuildUnit> dependencies = extractor.extractDependencies(changedUnit);
      // Find new Compilation units and add them
      for (BuildUnit dep : dependencies) {
        if (!changedUnit.getModuleDependencies().contains(dep)) {
          changedUnit.requires(dep);
          // Need to check dep iff rebuild all or if the unit is not persistent
          // or inconsistent
          if (!visitedUnits.contains(dep)) {
            if (this.needToCheckDependencies(dep)) {
              units.add(dep);
            }
            // Add it always to visited units to avoid multiple consistency
            // checks
            visitedUnits.add(dep);
          }
        }
      }
      // Remove compilation units which are not needed anymore
      // Need to copy existing units because they will be modified
      ArrayList<BuildUnit> allUnits = new ArrayList<BuildUnit>(changedUnit.getModuleDependencies());
      for (BuildUnit unit : allUnits) {
        if (!dependencies.contains(unit)) {
          changedUnit.removeModuleDependency(unit);
        }
      }

    }
  }

  /**
   * Creates a BuildSchedule for the units in unitsToCompile. That means that
   * the BuildSchedule is sufficient to build all dependencies of the given
   * units and the units itself.
   * 
   * The scheduleMode specifies which modules are included in the BuildSchedule.
   * For REBUILD_ALL, all dependencies are included in the schedule, whether
   * they are inconsistent or net. For REBUILD_INCONSISTENT, only dependencies
   * are included in the build schedule, if they are inconsistent. For
   * REBUILD_INCONSISTENT_INTERFACE, the same tasks are included in the schedule
   * but information for the interfaces of the modules before building is stored
   * and may be used to determine modules which does not have to be build later.
   * 
   * @param unitsToCompile
   *          a set of units which has to be compiled
   * @param editedSourceFiles
   * @param mode
   * @param scheduleMode
   *          the mode of the schedule as described
   * @return the created BuildSchedule
   */
  public BuildSchedule createBuildSchedule(Map<RelativePath, Stamp> editedSourceFiles) {
    BuildSchedule schedule = new BuildSchedule();

    // Calculate strongly connected components: O(E+V)
    List<Set<BuildUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(this.unitsToCompile);

    // Create tasks on fill map to find tasks for units: O(V)
    Map<BuildUnit, Task> tasksForUnit = new HashMap<>();
    List<Task> buildTasks = new ArrayList<>(sccs.size());
    for (Set<BuildUnit> scc : sccs) {
      Task t = new Task(scc);
      buildTasks.add(t);
      for (BuildUnit u : t.getUnitsToCompile()) {
        tasksForUnit.put(u, t);
      }
    }

    // Calculate dependencies between tasks (sccs): O(E+V)
    for (Task t : buildTasks) {
      for (BuildUnit u : t.getUnitsToCompile()) {
        for (BuildUnit dep : u.getModuleDependencies()) {
          Task depTask = tasksForUnit.get(dep);
          if (depTask != t) {
            t.addRequiredTask(depTask);
          }
        }
      }
    }

    // Prefilter tasks from which we know that they are consistent: O (V+E)
    // Why not filter consistent units before calculating the build
    // schedule:
    // This required calculating strongly connected components and a
    // topological order of them
    // which we also need for calculating the build schedule
    if (this.scheduleMode == ScheduleMode.REBUILD_INCONSISTENT) {
      // Here we need all inconsistent (shallowly) units, because we need to
      // recompile them
      // Deep inconsistence will be calculated more efficiently
      Set<BuildUnit> changedUnits = new HashSet<>();
      for (BuildUnit unit : this.unitsToCompile) {
        changedUnits.addAll(BuildUnitUtils.findInconsistentUnits(unit));
      }
      // All tasks which changed units are inconsistent
      Set<Task> inconsistentTasks = new HashSet<>();
      for (BuildUnit u : changedUnits) {
        inconsistentTasks.add(tasksForUnit.get(u));
      }
      // All transitivly reachable too
      // Make use of the reverse topological order we have already
      Iterator<Task> buildTaskIter = buildTasks.iterator();
      while (buildTaskIter.hasNext()) {
        Task task = buildTaskIter.next();
        boolean taskConsistent = true;
        if (inconsistentTasks.contains(task)) {
          taskConsistent = false;
        } else {
          // Reverse topological order of sccs guarantees that all required
          // tasks has been processed
          for (Task reqTask : task.requiredTasks) {
            if (inconsistentTasks.contains(reqTask)) {
              inconsistentTasks.add(task);
              taskConsistent = false;
              break;
            }
          }
        }
        if (taskConsistent) {
          // We may remove this task
          buildTaskIter.remove();
          task.remove();
        }
      }
    }

    // Find all leaf tasks in all tasks (tasks which does not require other
    // tasks): O(V)
    for (Task possibleRoot : buildTasks) {
      if (possibleRoot.hasNoRequiredTasks()) {
        schedule.addRootTask(possibleRoot);
      }
    }
    schedule.setOrderedTasks(buildTasks);

    // At the end, we validate the graph we build
    assert validateBuildSchedule(buildTasks);
    assert validateFlattenSchedule(buildTasks);

    return schedule;

  }

  private Set<BuildUnit> calculateReachableUnits(Task task) {
    Set<BuildUnit> reachableUnits = new HashSet<>();
    Deque<Task> taskStack = new LinkedList<>();
    Set<Task> seenTasks = new HashSet<>();
    taskStack.addAll(task.requiredTasks);
    reachableUnits.addAll(task.unitsToCompile);
    Map<Task, Task> preds = new HashMap<>();
    for (Task r : task.requiredTasks) {
      preds.put(r, task);
    }
    while (!taskStack.isEmpty()) {
      Task t = taskStack.pop();
      if (t == task) {
        Task tmp = preds.get(t);
        List<Task> path = new LinkedList<>();
        path.add(t);
        while (tmp != task) {
          path.add(tmp);
          tmp = preds.get(tmp);
        }
        path.add(task);
        throw new AssertionError("Graph contains a cycle with " + path);

      }
      seenTasks.add(t);
      reachableUnits.addAll(t.unitsToCompile);
      for (Task r : t.requiredTasks)
        if (!seenTasks.contains(r)) {
          taskStack.push(r);
          preds.put(r, t);
        }
    }
    return reachableUnits;
  }

  private boolean validateDependenciesOfTask(Task task, Set<BuildUnit> singleUnits) {
    Set<BuildUnit> reachableUnits = this.calculateReachableUnits(task);
    for (BuildUnit unit : singleUnits != null ? singleUnits : task.unitsToCompile) {
      if (!validateDeps("BuildSchedule", unit, reachableUnits)) {
        return false;
      }
    }

    return true;
  }

  private boolean validateBuildSchedule(Iterable<Task> allTasks) {
    for (Task task : allTasks) {
      if (!validateDependenciesOfTask(task, null)) {
        return false;
      }
    }
    return true;
  }

  boolean validateDeps(String prefix, BuildUnit unit, Set<BuildUnit> allDeps) {
    for (BuildUnit dep : unit.getModuleDependencies()) {
      if (needsToBeBuild(dep) && !allDeps.contains(dep)) {
        if (prefix != null)
          System.err.println(prefix + ": Schedule violates dependency: " + unit + " on " + dep);
        return false;
      }
    }
    return true;
  }

  boolean needsToBeBuild(BuildUnit unit) {
    // Calling isConsistent here is really really slow but safe and its a check
    boolean build = scheduleMode == BuildSchedule.ScheduleMode.REBUILD_ALL || !unit.isConsistent(null);
    return build;
  }

  public static boolean validateDepGraphCycleFree(Set<BuildUnit> startUnits) {
    Set<BuildUnit> unmarkedUnits = new HashSet<>();
    Set<BuildUnit> tempMarkedUnits = new HashSet<>();
    for (BuildUnit unit : startUnits) {
      unmarkedUnits.addAll(BuildUnitUtils.findAllUnits(unit));
    }

    while (!unmarkedUnits.isEmpty()) {
      BuildUnit unit = unmarkedUnits.iterator().next();
      if (!visit(unit, unmarkedUnits, tempMarkedUnits)) {
        return false;
      }
    }
    return true;

  }

  private static boolean visit(BuildUnit u, Set<BuildUnit> unmarkedUnits, Set<BuildUnit> tempMarkedUnits) {
    if (tempMarkedUnits.contains(u)) {
      return false; // Found a cycle
    }
    if (unmarkedUnits.contains(u)) {
      tempMarkedUnits.add(u);
      for (BuildUnit dep : u.getModuleDependencies()) {
        if (!visit(dep, unmarkedUnits, tempMarkedUnits)) {
          return false;
        }
      }
      unmarkedUnits.remove(u);
      tempMarkedUnits.remove(u);
    }
    return true;
  }

  private boolean validateFlattenSchedule(List<Task> flatSchedule) {
    Set<BuildUnit> collectedUnits = new HashSet<>();
    for (int i = 0; i < flatSchedule.size(); i++) {
      Task currentTask = flatSchedule.get(i);
      // Find duplicates
      for (BuildUnit unit : currentTask.unitsToCompile) {
        if (collectedUnits.contains(unit)) {
          throw new AssertionError("Task contained twice: " + unit);
        }
      }
      collectedUnits.addAll(currentTask.unitsToCompile);

      for (BuildUnit unit : currentTask.unitsToCompile) {
        validateDeps("Flattened Schedule", unit, collectedUnits);

      }
    }
    return true;
  }

}
