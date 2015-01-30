package org.sugarj.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

  public static SimpleCompilationUnit create(Stamper stamper, SimpleMode mode, Synthesizer syn, Path dep) throws IOException {
    return CompilationUnit.create(SimpleCompilationUnit.class, stamper, mode, syn, dep);
  }
}