package org.sugarj.cleardep.build;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.path.Path;

public abstract class Builder<C extends BuildContext, T extends Serializable, E extends CompilationUnit> {
  protected final C context;
  protected final BuilderFactory<C, T, E, ? extends Builder<C,T,E>> sourceFactory;
  
  public<F extends BuilderFactory<C, T, E, ? extends Builder<C,T,E>>> Builder(C context, F sourceFactory) {
    this.context = context;
    this.sourceFactory = sourceFactory;
  }
  
  /**
   * Provides the task description for the builder and its input.
   * The description is used for console logging when the builder is run.
   * 
   * @return the task description or `null` if no logging is wanted.
   */
  protected abstract String taskDescription(T input);
  protected abstract Path persistentPath(T input);
  protected abstract Class<E> resultClass();
  protected abstract Stamper defaultStamper();
  protected abstract void build(E result, T input) throws IOException;
  
  public CompilationUnit require(T input, Mode<E> mode) throws IOException {
    return this.context.getBuildManager().require(this, input, mode);
  }
  
  public RequirableCompilationUnit<C> requireLater(final T input, final Mode<E> mode) {
    return new DefaultRequireableCompilationUnit<>(this, input, mode);
  }
  
  public static interface RequirableCompilationUnit<C extends BuildContext> extends Serializable{
    // TODO: remove context argument again when context is provided by a builder
    public CompilationUnit require(C context) throws IOException;
  }
  
  private static class DefaultRequireableCompilationUnit<C extends BuildContext, T extends Serializable, E extends CompilationUnit> implements RequirableCompilationUnit<C>, Externalizable{
    
    /**
     * 
     */
    private static final long serialVersionUID = -7662824082815926229L;
    private Builder<C,T,E> builder;
    private T input;
    private Mode<E> mode;
    
    // TODO: remove this attributes when context is provided by another builder
    private BuilderFactory<C, T, E, ? extends Builder<C,T,E>> factory;

    public DefaultRequireableCompilationUnit(Builder<C,T,E> builder, T input, Mode<E> mode) {
      this.builder = builder;
      this.input = input;
      this.mode = mode;
      this.factory = builder.sourceFactory;
    }
    
    @Override
    public CompilationUnit require(C context) throws IOException {
      if (this.builder == null) {
        this.builder = factory.makeBuilder(context);
      }
      return this.builder.require(this.input, this.mode);
    }
    

    private void writeObject(ObjectOutputStream out) throws IOException {
      
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeObject(factory);
      out.writeObject(input);
      out.writeObject(mode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      this.factory = (BuilderFactory<C, T, E, ? extends Builder<C, T, E>>) in.readObject();
      input = (T) in.readObject();
      mode = (Mode<E>) in.readObject();
    }
    
  }
}
