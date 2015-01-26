package org.sugarj.cleardep;

import java.util.HashSet;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit.ModuleVisitor;
import org.sugarj.util.Predicate;

public class CompilationUnitUtils {

  public static Set<CompilationUnit> findUnitsWithMatch(final Predicate<? super CompilationUnit> predicate, CompilationUnit startUnit, boolean reverse) {
    final Set<CompilationUnit> results = new HashSet<>();

    ModuleVisitor<Void> visitor =new ModuleVisitor<Void>() {

      @Override
      public Void visit(CompilationUnit mod, Mode<?> mode) {
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

    startUnit.visit(visitor, null, reverse);

    return results;
  }

  public static Set<CompilationUnit> findUnitsWithChangedSourceFiles(CompilationUnit root) {
    return findUnitsWithMatch(new Predicate<CompilationUnit>() {

      @Override
      public boolean isFullfilled(CompilationUnit t) {
        return !t.isPersisted() || !t.isConsistentWithSourceArtifacts(null);
      }

    }, root, false);
  }
  
  public static Set<CompilationUnit> findInconsistentUnits(CompilationUnit root) {
	    return findUnitsWithMatch(new Predicate<CompilationUnit>() {

	      @Override
	      public boolean isFullfilled(CompilationUnit t) {
	        return !t.isPersisted() || !t.isConsistentShallow(null);
	      }

	    }, root, false);
	  }

  
  public static Set<CompilationUnit> findAllUnits(CompilationUnit root) {
    return findUnitsWithMatch(new Predicate<CompilationUnit>() {

      @Override
      public boolean isFullfilled(CompilationUnit t) {
        return true;
      }
    }, root, false);
  }

}
