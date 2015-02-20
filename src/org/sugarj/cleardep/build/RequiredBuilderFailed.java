package org.sugarj.cleardep.build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit;

public class RequiredBuilderFailed extends RuntimeException {
  private static final long serialVersionUID = 3080806736856580512L;

  public static class BuilderResult {
    public Builder<BuildContext, Serializable, CompilationUnit> builder;
    public Serializable input;
    public CompilationUnit result;
    public BuilderResult(Builder<BuildContext, Serializable, CompilationUnit> builder, Serializable input, CompilationUnit result) {
      this.builder = builder;
      this.input = input;
      this.result = result;
    }
  }
  
  private List<BuilderResult> builders;
  
  @SuppressWarnings("unchecked")
  public <T> RequiredBuilderFailed(Builder<?, ?, ?> builder, Serializable input, CompilationUnit result, Throwable cause) {
    super(cause);
    builders = new ArrayList<>();
    builders.add(new BuilderResult((Builder<BuildContext, Serializable, CompilationUnit>) builder, input, result));
  }
  
  @SuppressWarnings("unchecked")
  public void addBuilder(Builder<?, ?, ?> builder, Serializable input, CompilationUnit result) {
    builders.add(new BuilderResult((Builder<BuildContext, Serializable, CompilationUnit>) builder, input, result));
  }
  
  public BuilderResult getLastAddedBuilder() {
    return builders.get(builders.size() - 1);
  }

  public List<BuilderResult> getBuilders() {
    return builders;
  }
  
  @Override
  public String getMessage() {
    BuilderResult p = builders.get(0);
    return "Required builder failed. Error occurred in build step \"" + p.builder.taskDescription(p.input) + "\": " + getCause().getMessage();
  }
}
