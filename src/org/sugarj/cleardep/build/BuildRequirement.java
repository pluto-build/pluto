package org.sugarj.cleardep.build;

import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequirement<
  T extends Serializable, 
  E extends CompilationUnit, 
  B extends Builder<T, E>,
  F extends BuilderFactory<T, E, B>
> implements Serializable {
  private static final long serialVersionUID = -1598265221666746521L;
  
  public final F factory;
  public final T input;

  public BuildRequirement(F factory, T input) {
    this.factory = factory;
    this.input = input;
  }

  public E createBuilderAndRequire(BuildManager manager) throws IOException {
    return manager.require(factory.makeBuilder(input, manager));
  }
  
  public boolean equals(Object o) {
    return DeepEquals.deepEquals(this, o);
  }
  
  @Override
  public int hashCode() {
    return DeepEquals.deepHashCode(this);
  }
}
