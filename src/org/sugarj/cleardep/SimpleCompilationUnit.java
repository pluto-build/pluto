package org.sugarj.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

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
  protected void readEntity(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    super.readEntity(ois);
    mode = (Mode<?>) ois.readObject();
  }
  
  public static SimpleCompilationUnit readConsistent(Mode<SimpleCompilationUnit> mode, Map<? extends Path, Stamp> editedSourceFiles, Path... deps) throws IOException {
    return CompilationUnit.readConsistent(SimpleCompilationUnit.class, mode, editedSourceFiles, deps);
  }
  
  public static SimpleCompilationUnit read(Mode<SimpleCompilationUnit> mode, Path... deps) throws IOException {
    return CompilationUnit.read(SimpleCompilationUnit.class, mode, deps);
  }

  public static SimpleCompilationUnit create(Stamper stamper, SimpleMode mode, Synthesizer syn, Path dep) throws IOException {
    return CompilationUnit.create(SimpleCompilationUnit.class, stamper, mode, syn, dep);
  }
}