package org.sugarj.cleardep;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class Synthesizer {
  public Set<CompilationUnit> generatorModules;
  public Map<Path, Stamp> files;
  
  public Synthesizer(Set<CompilationUnit> modules, Map<Path, Stamp> files) {
    this.generatorModules = modules;
    this.files = files;
  }

  public Synthesizer(Stamper stamper, Set<CompilationUnit> modules, Set<Path> files) {
    this.generatorModules = modules;
    this.files = new HashMap<>();
    for (Path p : files)
      this.files.put(p, stamper.stampOf(p));
  }

  public void markSynthesized(CompilationUnit synthesizedModule) {
    for (CompilationUnit m : generatorModules)
      synthesizedModule.addModuleDependency(m);
    for (Entry<Path, Stamp> e : files.entrySet())
      synthesizedModule.addExternalFileDependency(e.getKey(), e.getValue());
  }
}
