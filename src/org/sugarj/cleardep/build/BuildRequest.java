package org.sugarj.cleardep.build;

import java.io.Serializable;

import org.sugarj.cleardep.output.BuildOutput;

import com.cedarsoftware.util.DeepEquals;

public class BuildRequest<
  In extends Serializable, 
  Out extends BuildOutput, 
  B extends Builder<In, Out>,
  F extends BuilderFactory<In, Out, B>
> implements Serializable {
  private static final long serialVersionUID = -1598265221666746521L;
  
  public final F factory;
  public final In input;

  public BuildRequest(F factory, In input) {
    this.factory = factory;
    this.input = input;
  }

  public Builder<In, Out> createBuilder() {
    return factory.makeBuilder(input);
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
