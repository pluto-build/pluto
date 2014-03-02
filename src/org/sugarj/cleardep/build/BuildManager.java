package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.dependency.FileRequirement;
import org.sugarj.cleardep.dependency.Requirement;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;

import com.cedarsoftware.util.DeepEquals;

public class BuildManager {

  private final Map<? extends Path, Stamp> editedSourceFiles;
  private RequireStack requireStack;

  private BuildRequest<?, ?, ?, ?> rebuildTriggeredBy = null;

  private Set<BuildUnit> consistentUnits;

  public BuildManager() {
    this(null);
  }

  public BuildManager(Map<? extends Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
    this.requireStack = new RequireStack();
    this.consistentUnits = new HashSet<>();
  }
private <T extends Serializable, E extends CompilationUnit> void setUpMetaDependency(Builder<T, E> builder, E depResult) throws IOException {
    if (depResult != null) {
      // require the meta builder...
      URL res = builder.getClass().getResource(builder.getClass().getSimpleName() + ".class");
      Path builderClass = new AbsolutePath(res.getFile());
      Path depFile = Xattr.getDefault().getGenBy(builderClass);
      if (!FileCommands.exists(depFile)) {
        Log.log.logErr("Warning: Builder was not built using meta builder. Consistency for builder changes are not tracked...", Log.DETAIL);
      } else {
        CompilationUnit metaBuilder = CompilationUnit.readUnchecked(CompilationUnit.class, depFile);

        depResult.addModuleDependency(metaBuilder);
        depResult.addExternalFileDependency(builderClass);
        for (Path p: metaBuilder.getExternalFileDependencies()) {
          depResult.addExternalFileDependency(p);
        }
      }
    }
  }  protected
  < T extends Serializable, 
    E extends BuildUnit,
    B extends Builder<T, E>,
    F extends BuilderFactory<T, E, B>
    > E executeBuilder(Builder<T, E> builder, Path dep, BuildRequest<T, E, B, F> buildReq) throws IOException {

    E depResult = BuildUnit.create(builder.resultClass(), builder.defaultStamper(), dep, buildReq);
    

    String taskDescription = builder.taskDescription();
    int inputHash = DeepEquals.deepHashCode(builder.input);

    BuildStackEntry entry = this.requireStack.push(builder.sourceFactory, dep);
    try {
      depResult.setState(BuildUnit.State.IN_PROGESS);

      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);

      setUpMetaDependency(builder, depResult);
      
      // call the actual builder

      builder.triggerBuild(depResult);
      // build(depResult, input);

      if (!depResult.isFinished())
        depResult.setState(BuildUnit.State.SUCCESS);
      depResult.write();
    } catch (RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.requires(required.result);
      depResult.setState(BuildUnit.State.FAILURE);

      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();

      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(BuildUnit.State.FAILURE);
      
      if (inputHash != DeepEquals.deepHashCode(builder.input))
        throw new AssertionError("API Violation detected: Builder mutated its input.");
      depResult.write();
      
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
      this.consistentUnits.add(assertConsistency(depResult));
      BuildStackEntry poppedEntry = this.requireStack.pop();
      assert poppedEntry == entry : "Got the wrong build stack entry from the requires stack";
    }

    if (depResult.getState() == BuildUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }

  public <T extends Serializable, E extends BuildUnit, B extends Builder<T, E>, F extends BuilderFactory<T, E, B>> E require(BuildRequest<T, E, B, F> buildReq) throws IOException {
    if (rebuildTriggeredBy == null) {
      rebuildTriggeredBy = buildReq;
      Log.log.beginTask("Incrementally rebuild inconsistent units", Log.CORE);
    }

    try {
    Builder<T, E> builder = buildReq.createBuilder(this);
    Path dep = builder.persistentPath();
      E depResult = BuildUnit.read(builder.resultClass(), dep, buildReq);
    
      if (depResult == null)
        return executeBuilder(builder, dep, buildReq);
      
      if (consistentUnits.contains(depResult))
        return assertConsistency(depResult);

      if (!depResult.isConsistentNonrequirements())
        return executeBuilder(builder, dep, buildReq);

      for (Requirement req : depResult.getRequirements()) {
        if (req instanceof FileRequirement) {
          FileRequirement freq = (FileRequirement) req;
          if (!freq.isConsistent())
            return executeBuilder(builder, dep, buildReq);
        }
        else if (req instanceof BuildRequirement) {
          BuildRequirement breq = (BuildRequirement) req;
          if (!breq.isConsistent())
            return executeBuilder(builder, dep, buildReq);
          require(breq.req);
        }
      }

      consistentUnits.add(assertConsistency(depResult));
      return depResult;
      } finally {
      if (rebuildTriggeredBy == buildReq)
          Log.log.endTask();
      }
    }

  private <E extends BuildUnit> E assertConsistency(E depResult) {
//    if (!depResult.isConsistent(null))
//      throw new AssertionError("Build manager does not guarantee soundness");
    return depResult;
  }
}
