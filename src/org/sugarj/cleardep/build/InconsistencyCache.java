package org.sugarj.cleardep.build;

import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.DEPENDENCIES_INCONSISTENT;
import static org.sugarj.cleardep.CompilationUnit.InconsistenyReason.NO_REASON;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.GraphUtils;
import org.sugarj.cleardep.CompilationUnit.InconsistenyReason;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.path.Path;

public class InconsistencyCache {
  
  private final Map<? extends Path, Stamp> editedSourceFiles;
  private Map<Path, InconsistenyReason> extendedInconsistencyMap = new HashMap<>();
  
  
  
  public InconsistencyCache(Map<? extends Path, Stamp> editedSourceFiles) {
    super();
    this.editedSourceFiles = editedSourceFiles;
  }

  protected InconsistenyReason getInconsistencyReasonSure(Path dep) {
    Objects.requireNonNull(dep);
    InconsistenyReason inconsistency = extendedInconsistencyMap.get(dep);
    if (inconsistency != null) {
      return inconsistency;
    }
    throw new AssertionError("Caller did not ensures that unit has been cached");
  }
  
  protected InconsistenyReason getInconsistencyReasonTry(Path dep) {
    Objects.requireNonNull(dep);
    return  extendedInconsistencyMap.get(dep);
  }
  
  protected boolean isConsistentTry(Path dep)  {
    return getInconsistencyReasonTry(dep) == InconsistenyReason.NO_REASON;
  }
  
  protected boolean isConsistentSure(Path dep)  {
    return getInconsistencyReasonSure(dep) == InconsistenyReason.NO_REASON;
  }
  
  protected <E extends CompilationUnit> void fillFor(E rootUnit) throws IOException {
    List<Set<CompilationUnit>> sccs = GraphUtils.calculateStronglyConnectedComponents(Collections.singleton(rootUnit));

    for (final Set<CompilationUnit> scc : sccs) {
      fillInconsistentCacheForScc(scc);
    }
  }

  protected void fillInconsistentCacheForScc(final Set<CompilationUnit> scc) {
    boolean sccConsistent = true;
    for (CompilationUnit unit : scc) {
      InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
      if (reason == null) {
        reason = unit.isConsistentShallowReason(this.editedSourceFiles);
        if (reason.compareTo(DEPENDENCIES_INCONSISTENT) < 0) {
          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep) && extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
              reason = DEPENDENCIES_INCONSISTENT;
              break;
            }
          }
        }
      }
      sccConsistent &= reason == NO_REASON;
      extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
    }
    if (!sccConsistent && scc.size() > 1) {
      for (CompilationUnit unit : scc) {
        if (extendedInconsistencyMap.get(unit.getPersistentPath()).compareTo(DEPENDENCIES_INCONSISTENT) < 0) {
          extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_INCONSISTENT);
        }
      }
    }
  }
  

  protected void updateCacheForScc(final Set<CompilationUnit> scc) {
    boolean sccConsistent = true;
    for (CompilationUnit unit : scc) {
      InconsistenyReason reason = extendedInconsistencyMap.get(unit.getPersistentPath());
      if (reason != NO_REASON) {
        if (reason == null || reason.compareTo(DEPENDENCIES_INCONSISTENT) >= 0) {
          reason = unit.isConsistentShallowReason(this.editedSourceFiles);
        } else {
          reason = NO_REASON;
        }

        if (reason.compareTo(DEPENDENCIES_INCONSISTENT) <= 0) {
          for (CompilationUnit dep : unit.getModuleDependencies()) {
            if (!scc.contains(dep) && extendedInconsistencyMap.get(dep.getPersistentPath()) != NO_REASON) {
              reason = DEPENDENCIES_INCONSISTENT;
              break;
            }
          }
        }

        sccConsistent &= reason == NO_REASON;
        extendedInconsistencyMap.put(unit.getPersistentPath(), reason);
      }
    }
    if (!sccConsistent && scc.size() > 1) {
      for (CompilationUnit unit : scc) {
        if (extendedInconsistencyMap.get(unit.getPersistentPath()).compareTo(NO_REASON) == 0) {
          extendedInconsistencyMap.put(unit.getPersistentPath(), DEPENDENCIES_INCONSISTENT);
        }
      }
    }
  }
  
  protected void setConsistent(Path depPath) {
    this.extendedInconsistencyMap.put(depPath, NO_REASON);
  }
  
  protected void set(Path depPath, InconsistenyReason reason) {
    this.extendedInconsistencyMap.put(depPath, reason);
  }


}
