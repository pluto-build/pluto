package org.sugarj.cleardep.stamp;

import java.util.HashSet;
import java.util.Set;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

public class GeneratedFilesModuleStamper implements ModuleStamper {

  private final Set<String> filter;
  private final CollectionStamper cStamper;
  
  public GeneratedFilesModuleStamper(Stamper fileStamper) {
    this(fileStamper, null);
  }
  
  public GeneratedFilesModuleStamper(Stamper fileStamper, Set<String> filter) {
    this.filter = filter;
    this.cStamper = new CollectionStamper(fileStamper);
  }
  
  @Override
  public ModuleStamp stampOf(CompilationUnit mod) {
    if (filter == null)
      return new GeneratedFilesModuleStamp(this, cStamper.stampOf(mod.getGeneratedFiles()));
    
    Set<Path> filteredGeneratedFiles = new HashSet<>();
    for (Path p : mod.getGeneratedFiles())
      if (filter.contains(FileCommands.getExtension(p)))
        filteredGeneratedFiles.add(p);
    
    return new GeneratedFilesModuleStamp(this, cStamper.stampOf(filteredGeneratedFiles));
  }

  
  public static class GeneratedFilesModuleStamp implements ModuleStamp {

    private static final long serialVersionUID = -5500385476695468042L;

    private final ModuleStamper stamper;
    private final Stamp filesStamp;
    
    public GeneratedFilesModuleStamp(ModuleStamper stamper, Stamp filesStamp) {
      this.stamper = stamper;
      this.filesStamp = filesStamp;
    }
    
    @Override
    public boolean equals(ModuleStamp o) {
      return o instanceof GeneratedFilesModuleStamp && ((GeneratedFilesModuleStamp) o).filesStamp.equals(filesStamp);
    }

    @Override
    public ModuleStamper getModuleStamper() {
      return stamper;
    }
    
  }
}
