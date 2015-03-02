package org.sugarj.cleardep.dependency;

import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequirement<
  T extends Serializable, 
  E extends CompilationUnit, 
  B extends Builder<T, E>,
  F extends BuilderFactory<T, E, B>
> implements Serializable, Requirement {
  private static final long serialVersionUID = -1598265221666746521L;
  
  public final F factory;
  public final T input;

  public BuildRequirement(F factory, T input) {
    this.factory = factory;
    this.input = input;
  }

  public Builder<T, E> createBuilder(BuildManager manager) {
    return factory.makeBuilder(input, manager);
  }
  
  public boolean deepEquals(Object o) {
    return DeepEquals.deepEquals(this, o);
  }
  
  public int deepHashCode() {
    return DeepEquals.deepHashCode(this);
  }
  
  @Override
  public String toString() {
    return "BuildReq(" + factory.getClass().getName() + ")";
  }
}
