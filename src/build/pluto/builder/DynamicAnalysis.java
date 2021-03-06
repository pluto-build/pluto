package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sugarj.common.StringCommands;

import com.cedarsoftware.util.DeepEquals;
import com.cedarsoftware.util.Traverser;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.ModuleVisitor;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.DuplicateBuildUnitPathException;
import build.pluto.dependency.DuplicateFileGenerationException;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.IllegalDependencyException;
import build.pluto.dependency.Requirement;
import build.pluto.dependency.database.MultiMapDatabase;
import build.pluto.output.Output;
import build.pluto.util.AbsoluteComparedFile;
import build.pluto.util.IReporting;

public class DynamicAnalysis implements AutoCloseable {
  private final MultiMapDatabase<File, File> genBy;
  private final IReporting report;
  private Map<File, BuildUnit<?>> generatedFiles;
  private Map<Output, BuildUnit<?>> generatedOutput;

  public DynamicAnalysis(IReporting report, MultiMapDatabase<File, File> genBy) {
    this.genBy = genBy;
    this.report = report;
    this.generatedFiles = new HashMap<>();
    this.generatedOutput = new HashMap<>();
  }

  @Override
  public void close() throws IOException {
    genBy.close();
  }

  public void resetAnalysis() throws IOException {
    genBy.clear();
  }

  public Collection<File> getGenBy(File generated) throws IOException {
    return genBy.get(generated);
  }

  public void reset(BuildUnit<?> unit) throws IOException {
    if (unit != null) {
      Set<File> files = unit.getGeneratedFiles();
      genBy.removeForEach(files, unit.getPersistentPath());
      for (File f : files) {
        generatedFiles.remove(f);
      }
    }
  }

  public void check(BuildUnit<?> unit, Integer inputHash) throws IOException {
    checkInput(unit, inputHash);
    checkGeneratedFilesOverlap(unit);
    checkUnitDependency(unit);
    checkGeneratedOutputs(unit);
  }

  /**
   * The input may not have been changed during the build.
   */
  private void checkInput(BuildUnit<?> unit, Integer inputHash) throws AssertionError {
    if (inputHash != null && inputHash != DeepEquals.deepHashCode(unit.getGeneratedBy().input))
      throw new AssertionError("API Violation detected: Builder mutated its input.");
  }

  /**
   * The build unit must have a unique persistent path and may not generated
   * files previously generated by another build unit.
   */
  private void checkGeneratedFilesOverlap(BuildUnit<?> unit) throws IOException {
    BuildUnit<?> other = generatedFiles.put(unit.getPersistentPath(), unit);
    if (other != null && other != unit)
      throw new DuplicateBuildUnitPathException("Build unit " + unit + " has same persistent path as build unit " + other);

    Set<File> files = unit.getGeneratedFiles();
    genBy.addForEach(files, unit.getPersistentPath());
    for (File file : files) {
      other = generatedFiles.put(file, unit);
      if (other != null && other != unit) {
        BuildRequest<?, ?, ?, ?> unitReq = unit.getGeneratedBy();
        BuildRequest<?, ?, ?, ?> otherReq = other.getGeneratedBy();
        boolean overlapOK = unitReq.factory.isOverlappingGeneratedFileCompatible(file, unitReq.input, otherReq.factory, otherReq.input);

        if (!overlapOK)
          throw new DuplicateFileGenerationException("Build unit " + unit + " generates same file as build unit " + other);
      }
    }
  }

  /**
   * When a build unit A requires a file that was generated by a build unit B,
   * then build unit A must already have a build requirement on build unit B.
   */
  private void checkUnitDependency(BuildUnit<?> unit) {
    Set<BuildUnit<?>> requiredUnits = new HashSet<>();

    for (Requirement req : unit.getRequirements()) {
      if (req instanceof BuildRequirement<?>)
        requiredUnits.add(((BuildRequirement<?>) req).getUnit());
      else if (req instanceof FileRequirement) {
        File file = ((FileRequirement) req).file;
        if (file.exists()) {
          Collection<File> deps = null;
          try {
            deps = genBy.get(file);
          } catch (IOException e) {
            report.messageFromSystem("WARNING: Could not verify build-unit dependency due to exception \"" + e.getMessage() + "\" while reading metadata: " + file, true, 0);
          }

          if (deps != null && !deps.isEmpty()) {
            boolean foundDep = false;
            for (File dep : deps)
              if (AbsoluteComparedFile.equals(unit.getPersistentPath(), dep)) {
                foundDep = true;
                break;
              }

            if (!foundDep && deps.size() == 1)
              foundDep = unit.visit(new IsConnectedTo(deps.iterator().next()), requiredUnits);
            else if (!foundDep)
              foundDep = unit.visit(new IsConnectedToAny(deps), requiredUnits);

            if (!foundDep && deps.size() == 1)
              throw new IllegalDependencyException(deps, "Build unit " + unit.getPersistentPath() + " has a hidden dependency on file " + file + " without build-unit dependency on " + deps.iterator().next() + ", which generated this file. " + "The builder " + unit.getGeneratedBy().createBuilder().description() + " should " + "mark a dependency to " + deps.iterator().next() + " by `requiring` the corresponding builder.");
            else if (!foundDep)
              throw new IllegalDependencyException(deps, "Build unit " + unit.getPersistentPath() + " has a hidden dependency on file " + file + " without build-unit dependency on at least one of [" + StringCommands.printListSeparated(deps, ", ") + "], all " + "of which generated this file. " + "The builder " + unit.getGeneratedBy().createBuilder().description() + " should " + "mark a dependency to one of [" + StringCommands.printListSeparated(deps, ", ") + "] by `requiring` the corresponding" + " builder.");
          }
        }
      }
    }
  }

  /**
   * A builder must declare build requirements on all builders whose outputs it
   * uses (including outputs provided via the build input).
   */
  private void checkGeneratedOutputs(final BuildUnit<?> unit) {
    if (unit.getBuildResult() != null)
      generatedOutput.put(unit.getBuildResult(), unit);

    Traverser.traverse(unit.getGeneratedBy().input, new Traverser.Visitor() {
      @Override
      public void process(Object o) {
        if (o instanceof Output) {
          BuildUnit<?> generator = generatedOutput.get(o);
          if (generator != null) {
            File dep = generator.getPersistentPath();
            boolean foundDep = AbsoluteComparedFile.equals(unit.getPersistentPath(), dep) || unit.visit(new IsConnectedTo(dep));
            if (!foundDep)
              throw new IllegalDependencyException(Collections.singleton(dep), "Build unit " + dep + " has a hidden dependency on the " + "in-memory output of build unit " + generator + ". " + "The builder " + unit.getGeneratedBy().createBuilder().description() + " should " + "mark a dependency to " + dep + " by `requiring` the corresponding builder.");
          }
        }
      }
    });
  }

  private static class IsConnectedTo implements ModuleVisitor<Boolean> {
    private final File requiredUnit;

    public IsConnectedTo(File requiredUnit) {
      this.requiredUnit = Objects.requireNonNull(requiredUnit);
    }

    @Override
    public Boolean visit(BuildUnit<?> mod) {
      return AbsoluteComparedFile.equals(requiredUnit, mod.getPersistentPath());
    }

    @Override
    public Boolean combine(Boolean t1, Boolean t2) {
      return t1 || t2;
    }

    @Override
    public Boolean init() {
      return false;
    }

    @Override
    public boolean cancel(Boolean t) {
      return t;
    }
  }

  private static class IsConnectedToAny implements ModuleVisitor<Boolean> {
    private final Set<File> requireAtLeastOne;

    public IsConnectedToAny(Collection<File> requireAtLeastOne) {
      Objects.requireNonNull(requireAtLeastOne);
      this.requireAtLeastOne = new HashSet<File>();
      for (File f : requireAtLeastOne)
        this.requireAtLeastOne.add(f.getAbsoluteFile());
    }

    @Override
    public Boolean visit(BuildUnit<?> mod) {
      return requireAtLeastOne.contains(mod.getPersistentPath().getAbsoluteFile());
    }

    @Override
    public Boolean combine(Boolean t1, Boolean t2) {
      return t1 || t2;
    }

    @Override
    public Boolean init() {
      return false;
    }

    @Override
    public boolean cancel(Boolean t) {
      return t;
    }
  }
}
