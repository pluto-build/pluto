package build.pluto;

import java.util.HashSet;
import java.util.Set;

import org.sugarj.common.util.Predicate;

import build.pluto.BuildUnit.ModuleVisitor;

public class BuildUnitUtils {

  public static Set<BuildUnit<?>> findUnitsWithMatch(final Predicate<BuildUnit<?>> predicate, BuildUnit<?> startUnit, boolean reverse) {
    final Set<BuildUnit<?>> results = new HashSet<>();

    ModuleVisitor<Void> visitor =new ModuleVisitor<Void>() {

      @Override
      public Void visit(BuildUnit<?> mod) {
        if (predicate.isFullfilled(mod)) {
          results.add(mod);
        }
        return null;
      }

      @Override
      public Void combine(Void t1, Void t2) {
        return null;
      }

      @Override
      public Void init() {
        return null;
      }

      @Override
      public boolean cancel(Void t) {
        return false;
      }
    };

    startUnit.visit(visitor);

    return results;
  }

  public static Set<BuildUnit<?>> findUnitsWithChangedSourceFiles(BuildUnit<?> root) {
    return findUnitsWithMatch(new Predicate<BuildUnit<?>>() {

      @Override
      public boolean isFullfilled(BuildUnit<?> t) {
        return !t.isPersisted() || !t.isConsistentWithSourceArtifacts(null);
      }

    }, root, false);
  }
  
  public static Set<BuildUnit<?>> findInconsistentUnits(BuildUnit<?> root) {
	    return findUnitsWithMatch(new Predicate<BuildUnit<?>>() {

	      @Override
	      public boolean isFullfilled(BuildUnit<?> t) {
	        return !t.isPersisted() || !t.isConsistentShallow(null);
	      }

	    }, root, false);
	  }

  
  public static Set<BuildUnit<?>> findAllUnits(BuildUnit<?> root) {
    return findUnitsWithMatch(new Predicate<BuildUnit<?>>() {

      @Override
      public boolean isFullfilled(BuildUnit<?> t) {
        return true;
      }
    }, root, false);
  }

}
