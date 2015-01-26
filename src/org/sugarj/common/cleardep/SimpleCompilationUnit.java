package org.sugarj.common.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class SimpleCompilationUnit extends CompilationUnit {
  private static final long serialVersionUID = -83007451176964292L;

  @Override
  protected boolean isConsistentExtend() {
    return true;
  }
  
  @Override
  protected void writeEntity(ObjectOutputStream oos) throws IOException {
    super.writeEntity(oos);
    oos.writeObject(getMode());
  }
  
  @Override
  protected void readEntity(ObjectInputStream ois, Stamper stamper) throws IOException, ClassNotFoundException {
    super.readEntity(ois, stamper);
    mode = (Mode<?>) ois.readObject();
  }

  public static SimpleCompilationUnit create(Stamper stamper, SimpleMode mode, Synthesizer syn, Map<RelativePath, Stamp> sourceFiles, Path dep) throws IOException {
    return CompilationUnit.create(SimpleCompilationUnit.class, stamper, mode, syn, sourceFiles, dep);
  }
  
  public static SimpleCompilationUnit read(Stamper stamper, SimpleMode mode, Path... deps) throws IOException {
    return CompilationUnit.read(SimpleCompilationUnit.class, stamper, mode, deps);
  }
  
  public static SimpleCompilationUnit readConsistent(Stamper stamper, SimpleMode mode, Map<RelativePath, Stamp> editedSourceFiles, Path... deps) throws IOException {
    return CompilationUnit.readConsistent(SimpleCompilationUnit.class, stamper, mode, editedSourceFiles, deps);
  }
}