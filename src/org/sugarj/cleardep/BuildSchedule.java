package org.sugarj.cleardep;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.path.RelativePath;

public class BuildSchedule {


  public static enum TaskState {
    OPEN, IN_PROGESS, SUCCESS, FAILURE;
  }

  private static int maxID = 0;

  public static class Task {

    Set<CompilationUnit> unitsToCompile;
    Set<Task> requiredTasks;
    Set<Task> tasksRequiringMe;
    private TaskState state;
    private int id;

    protected Task() {
      this(new HashSet<CompilationUnit>());
    }

    protected Task(CompilationUnit unit) {
      this();
      this.unitsToCompile.add(unit);
    }
    
    protected Task(Set<CompilationUnit> units) {
    	Objects.requireNonNull(units);
      this.unitsToCompile = units;
      this.requiredTasks = new HashSet<>();
      this.tasksRequiringMe = new HashSet<>();
      this.state = TaskState.OPEN;
      id = maxID;
      maxID++;
    }

    protected boolean containsUnits(CompilationUnit unit) {
      return this.unitsToCompile.contains(unit);
    }

    /**
     * The task needs to be build iff one unit is not finished, not shallow consistent, or not consistent to interfaces.
     */
    public boolean needsToBeBuild(Map<RelativePath, Stamp> editedSourceFiles) {
      for (CompilationUnit u : this.unitsToCompile) {
        if (!u.isFinished() || 
            !u.isConsistentShallow(editedSourceFiles) || 
            !u.isConsistentToDependencyInterfaces()) {
          return true;
        }
      }

      return false;
    }

    public boolean isCompleted() {
      return this.state == TaskState.SUCCESS || this.state == TaskState.FAILURE;
    }

    public TaskState getState() {
      return this.state;
    }

    public Set<CompilationUnit> getUnitsToCompile() {
      return unitsToCompile;
    }

    protected void addUnit(CompilationUnit unit) {
      this.unitsToCompile.add(unit);
    }

    protected void addRequiredTask(Task task) {
      if (task == this) {
        throw new IllegalArgumentException("Cannot require itself");
      }
      this.requiredTasks.add(task);
      task.tasksRequiringMe.add(this);
    }

    protected boolean requires(Task task) {
      return this.requiredTasks.contains(task);
    }

    protected boolean hasNoRequiredTasks() {
      return this.requiredTasks.isEmpty();
    }
    
    protected void remove() {
    	for (Task t : this.requiredTasks) {
    		t.tasksRequiringMe.remove(this);
    	}
    	for (Task t : this.tasksRequiringMe) {
    		t.requiredTasks.remove(this);
    	}
    }

    @Override
    public String toString() {
      String reqs = "";
      for (Task dep : this.requiredTasks) {
        reqs += dep.id + ",";
      }
      String s = "Task_" + id + "_(" + reqs + ")[";
      for (CompilationUnit u : this.unitsToCompile)
        for (RelativePath p : u.getSourceArtifacts())
          s += p.getRelativePath() + ", ";
      s += "]";
      return s;
    }

    @Override
    public int hashCode() {
      return this.id;
    }

  }


  private Set<Task> rootTasks;
  private List<Task> orderedSchedule;
  
  protected BuildSchedule() {
    this.rootTasks = new HashSet<>();
  }
  
  protected void setOrderedTasks(List<Task> tasks) {
    this.orderedSchedule = tasks;
  }
  
  public List<Task> getOrderedSchedule() {
    return orderedSchedule;
  }

  protected void addRootTask(Task task) {
    assert task.hasNoRequiredTasks() : "Given task is not a root";
    this.rootTasks.add(task);
  }


  public static enum ScheduleMode {
    REBUILD_ALL, REBUILD_INCONSISTENT;
  }


  @Override
  public String toString() {
    String str = "BuildSchedule: " + this.rootTasks.size() + " root tasks\n";
    return str;

  }
}
