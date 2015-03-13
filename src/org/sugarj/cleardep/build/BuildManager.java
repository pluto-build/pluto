package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.BuildCycle.Result.UnitResultTuple;
import org.sugarj.cleardep.build.BuildCycleException.CycleState;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.dependency.BuildOutputRequirement;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.dependency.DuplicateBuildUnitPathException;
import org.sugarj.cleardep.dependency.DuplicateFileGenerationException;
import org.sugarj.cleardep.dependency.FileRequirement;
import org.sugarj.cleardep.dependency.Requirement;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager extends BuildUnitProvider {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends Serializable> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    return build(buildReq, null);
  }

  public static <Out extends Serializable> Out build(BuildRequest<?, Out, ?, ?> buildReq, Map<? extends Path, Stamp> editedSourceFiles) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(editedSourceFiles);
      activeManagers.put(current, manager);
    }

    try {
      return manager.requireInitially(buildReq).getBuildResult();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  public static <Out extends Serializable> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs) {
    return buildAll(buildReqs, null);
  }

  public static <Out extends Serializable> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs, Map<? extends Path, Stamp> editedSourceFiles) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(editedSourceFiles);
      activeManagers.put(current, manager);
    }

    try {
      List<Out> out = new ArrayList<>();
      for (BuildRequest<?, Out, ?, ?> buildReq : buildReqs)
        if (buildReq != null)
          try {
            out.add(manager.requireInitially(buildReq).getBuildResult());
          } catch (IOException e) {
            e.printStackTrace();
            out.add(null);
          }
      return out;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  private final Map<? extends Path, Stamp> editedSourceFiles;
  private ExecutingStack executingStack;

  // private transient ConsistencyManager consistencyManager;
  private transient Map<Path, Set<Path>> assumedCyclicConsistency;
  private transient Set<Path> consistentUnits;
  private transient Set<Path> knownInconsistentUnits;
  private transient Deque<Path> requireStack;

  private transient Map<Path, BuildUnit<?>> generatedFiles;

  protected BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.executingStack = new ExecutingStack();
    // this.consistencyManager = new ConsistencyManager();
    this.assumedCyclicConsistency = new HashMap<>();
    this.consistentUnits = new HashSet<>();
    this.knownInconsistentUnits = new HashSet<>();
    this.requireStack = new LinkedList<>();
    this.generatedFiles = new HashMap<>();
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends Serializable>
  // @formatter:on
  void setUpMetaDependency(Builder<In, Out> builder, BuildUnit<Out> depResult) throws IOException {
    if (depResult != null) {
      // require the meta builder...
      URL res = builder.getClass().getResource(builder.getClass().getSimpleName() + ".class");
      Path builderClass = new AbsolutePath(res.getFile());
      Path depFile = Xattr.getDefault().getGenBy(builderClass);
      if (!FileCommands.exists(depFile)) {
        Log.log.logErr("Warning: Builder was not built using meta builder. Consistency for builder changes are not tracked...", Log.DETAIL);
      } else {
        BuildUnit<Serializable> metaBuilder = BuildUnit.read(depFile);

        depResult.requires(metaBuilder);
        depResult.requires(builderClass, LastModifiedStamper.instance.stampOf(builderClass));

        // TODO: needed?
        //for (Path p : metaBuilder.getExternalFileDependencies()) {
        //  depResult.requires(p, LastModifiedStamper.instance.stampOf(p));
        //}
      }
    }
  }

  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  // @formatter:on
  BuildUnit<Out> executeBuilder(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq) throws IOException {

    this.knownInconsistentUnits.add(dep);
    Set<Path> cyclicAssumptions = this.assumedCyclicConsistency.get(dep);
 //   if (cyclicAssumptions != null) {
      for (Path assumed : this.requireStack) {
        this.knownInconsistentUnits.add(assumed);
        
        this.consistentUnits.remove(assumed);
      }
  //  }

    resetGenBy(dep, BuildUnit.read(dep));
    BuildUnit<Out> depResult = BuildUnit.create(dep, buildReq);
    int inputHash = DeepEquals.deepHashCode(builder.input);

    String taskDescription = builder.taskDescription();

    BuildStackEntry<Out> entry = null;
    // First step: cycle detection
   

    if (taskDescription != null)
      Log.log.beginTask(taskDescription, Log.CORE);
    
    entry = this.executingStack.push(depResult);

    depResult.setState(BuildUnit.State.IN_PROGESS);

    try {
      try {

        // setUpMetaDependency(builder, depResult);

        // call the actual builder
        Out out = builder.triggerBuild(depResult, this);
        depResult.setBuildResult(out);

        if (!depResult.isFinished())
          depResult.setState(BuildUnit.State.SUCCESS);
        // build(depResult, input);
      } catch (BuildCycleException e) {
        tryCompileCycle(e);
      }

    } catch (BuildCycleException e) {
      stopBuilderInCycle(builder, dep, buildReq, depResult, e);
    } catch (RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.requires(required.result);
      depResult.setState(BuildUnit.State.FAILURE);

      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(BuildUnit.State.FAILURE);

      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {

      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();
      assertConsistency(depResult);
     // if (cyclicAssumptions == null || cyclicAssumptions.isEmpty()) {
        this.consistentUnits.add(dep);
        this.assumedCyclicConsistency.remove(dep);
     // }
      this.knownInconsistentUnits.remove(dep);

      if (taskDescription != null)
        Log.log.endTask();
      BuildStackEntry<?> poppedEntry = this.executingStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  protected CycleSupport searchForCycleSupport(BuildCycle cycle) {
    for (BuildRequirement<?> requirement : cycle.getCycleComponents()) {
      CycleSupport support = requirement.req.createBuilder().getCycleSupport();
      if (support != null && support.canCompileCycle(cycle)) {
        return support;
      }
    }
    return null;
  }

  @Override
  protected void tryCompileCycle(BuildCycleException e) throws Throwable {
    if (e.getCycleState() == CycleState.UNHANDLED) {

      Log.log.log("Detected a dependency cycle with root " + e.getCycleComponents().get(0).unit.getPersistentPath(), Log.CORE);

      e.setCycleState(CycleState.NOT_RESOLVED);
      BuildCycle cycle = new BuildCycle(e.getCycleComponents());
      CycleSupport cycleSupport = this.searchForCycleSupport(cycle);
      if (cycleSupport == null) {
        throw e;
      }

      Log.log.beginTask("Compile cycle with: " + cycleSupport.getCycleDescription(cycle), Log.CORE);
      try {
        try {
        BuildCycle.Result result = cycleSupport.compileCycle(this, cycle);
        e.setCycleResult(result);
        e.setCycleState(CycleState.RESOLVED);
        } catch (BuildCycleException cyclicEx) {
          e.setCycleState(cyclicEx.getCycleState());
          e.setCycleResult(cyclicEx.getCycleResult());
        } catch (Throwable t) {
          throw t;
        }
      } finally {
        Log.log.endTask();
        throw e;
     }
    } else {

      throw e;
    }
  }

  // @formatter:off
  private 
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>> 
  //@formatter:on
  void stopBuilderInCycle(Builder<In, Out> builder, Path dep, BuildRequest<In, Out, B, F> buildReq, BuildUnit<Out> depResult, BuildCycleException e) {
    // This is the exception which has been rethrown above, but we cannot
    // handle it
    // here because compiling the cycle needs to be in the major try block
    // where normal
    // units are compiled too

    // Set the result to the unit
    if (e.getCycleState() == CycleState.RESOLVED) {
      if (e.getCycleResult() == null) {
        Log.log.log("Error: Cyclic builder does not provide a cycleResult " + e.hashCode(), Log.CORE);
        throw new AssertionError("Cyclic builder does not provide a cycleResult");
      }
      UnitResultTuple<Out> tuple = e.getCycleResult().getUnitResult(depResult);
      if (tuple == null) {
        throw new AssertionError("Cyclic builder does not provide a result for " + depResult.getPersistentPath());
      }
      tuple.setOutputToUnit();
    }
    Log.log.log("Stopped because of cycle", Log.CORE);
    if (e.isUnitFirstInvokedOn(dep, buildReq.factory)) {
      if (e.getCycleState() != CycleState.RESOLVED) {
        Log.log.log("Unable to find builder which can compile the cycle", Log.CORE);
        // Cycle cannot be handled
        throw new RequiredBuilderFailed(builder, depResult, e);
      } else {

        if (this.executingStack.getNumContains(e.getCycleComponents().get(0).unit) == 1) {
          Log.log.log("but cycle has been compiled", Log.CORE);
        } else {
          throw e;
        }
      }
    } else {
      // Kill depending builders
      throw e;
    }
  }
  
//@formatter:off
  protected
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> requireInitially (BuildRequest<In, Out, B, F> buildReq) throws IOException {
    Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
    try {
      return require(null, buildReq);
    } finally {
      Log.log.endTask();
    }
  }

  @Override
  //@formatter:off
  protected
    <In extends Serializable,
     Out extends Serializable,
     B extends Builder<In, Out>,
     F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildUnit<Out> require(BuildUnit<?> source, BuildRequest<In, Out, B, F> buildReq) throws IOException {
    
    BuildUnit<Out> depResult = null;
    Path dep = null;
   
    
   
      Builder<In, Out> builder = buildReq.createBuilder();
      dep = builder.persistentPath();
      depResult = BuildUnit.read(dep);
      
      Set<Path> cyclicAssumptions = assumedCyclicConsistency.get(dep);
      if (cyclicAssumptions == null) {
        cyclicAssumptions = new HashSet<Path>();
        assumedCyclicConsistency.put(dep, cyclicAssumptions);
      }
  /*  
     Log.log.beginTask("Require " + ((RelativePath)dep).getRelativePath(), Log.CORE);
      Log.log.log("Consistent:     " + consistentUnits.contains(dep), Log.CORE);
      Log.log.log("Not Consistent: " + knownInconsistentUnits.contains(dep), Log.CORE);
      Log.log.log("Assumptions:    " + cyclicAssumptions, Log.CORE);
      Log.log.log("Knon Consist:   " + consistentUnits, Log.CORE);
      Log.log.log("Knon Consist:   " + knownInconsistentUnits, Log.CORE);
*/

      if (knownInconsistentUnits.contains(dep)) {
        depResult = BuildUnit.create(dep, buildReq);
        depResult =null;
      }

      if (depResult == null)
        return executeBuilder(builder, dep, buildReq);

      if (!depResult.getGeneratedBy().deepEquals(buildReq))
        return executeBuilder(builder, dep, buildReq);

      for (Path p : cyclicAssumptions) {
        if (this.knownInconsistentUnits.contains(p)) {
          return executeBuilder(builder, dep, buildReq);
        }
      }

      if (this.consistentUnits.contains(dep))
        return depResult;

      if (this.requireStack.contains(dep)) {
        Set<Path> unitAssumptions = assumedCyclicConsistency.get(source.getPersistentPath());
        unitAssumptions.addAll(cyclicAssumptions);
        unitAssumptions.add(dep);
        cyclicAssumptions.addAll(unitAssumptions);
        cyclicAssumptions.add(source.getPersistentPath());
        cyclicAssumptions.add(dep);
        assumedCyclicConsistency.put(dep, cyclicAssumptions);
        
        return depResult;
      }
      requireStack.push(dep);

      try {
        if (!depResult.isConsistentNonrequirements())
          return executeBuilder(builder, dep, buildReq);

        for (Requirement req : depResult.getRequirements()) {
          if (req instanceof FileRequirement) {
            FileRequirement freq = (FileRequirement) req;
            if (!freq.isConsistent())
              return executeBuilder(builder, dep, buildReq);
          } else if (req instanceof BuildRequirement) {
            BuildRequirement<?> breq = (BuildRequirement<?>) req;
            int numBefore = 0;
            if (breq.unit != null && assumedCyclicConsistency.containsKey(breq.unit.getPersistentPath())) {
              numBefore = assumedCyclicConsistency.get(breq.unit.getPersistentPath()).size();
            }
           
            BuildUnit<?> unit = require(depResult, breq.req);
            Set<Path> depAssumptions = assumedCyclicConsistency.get(unit.getPersistentPath());
            if (depAssumptions != null) {
              int sizeAfter = depAssumptions.size();
              cyclicAssumptions.addAll(depAssumptions);
              if (numBefore < sizeAfter) {
                depAssumptions.add(dep);
              }
            
            }
            if (this.consistentUnits.contains(dep))
              return depResult;
          } else if (req instanceof BuildOutputRequirement) {
            if (! req.isConsistent())
              return executeBuilder(builder, dep, buildReq);
          }
        }
        this.consistentUnits.add(dep);

      } finally {
        requireStack.pop();
      }
      
     

      return depResult;
    
  }

  private <Out extends Serializable> BuildUnit<Out> assertConsistency(BuildUnit<Out> depResult) {
    BuildUnit<?> other = generatedFiles.put(depResult.getPersistentPath(), depResult);
    if (other != null && other != depResult)
      throw new DuplicateBuildUnitPathException("Build unit " + depResult + " has same persistent path as build unit " + other);

    for (FileRequirement freq : depResult.getGeneratedFileRequirements()) {
      other = generatedFiles.put(freq.path, depResult);
      if (other != null && other != depResult)
        throw new DuplicateFileGenerationException("Build unit " + depResult + " generates same file as build unit " + other);
    }

    // if (!depResult.isConsistent(null))
    // throw new AssertionError("Build manager does not guarantee soundness");
    return depResult;
  }

  private void resetGenBy(Path dep, BuildUnit<?> depResult) throws IOException {
    if (depResult != null)
      for (Path p : depResult.getGeneratedFiles())
        BuildUnit.xattr.removeGenBy(p);
  }
}
