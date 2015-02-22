package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit;

public class RequiredBuilderFailed extends RuntimeException {
  private static final long serialVersionUID = 3080806736856580512L;

  public static class BuilderResult {
    public Builder<?, CompilationUnit> builder;
    public CompilationUnit result;
    public BuilderResult(Builder<?, CompilationUnit> builder, CompilationUnit result) {
      this.builder = builder;
      this.result = result;
    }
  }
  
  private List<BuilderResult> builders;
  
  @SuppressWarnings("unchecked")
  public <T> RequiredBuilderFailed(Builder<?, ?> builder, CompilationUnit result, Throwable cause) {
    super(cause);
    builders = new ArrayList<>();
    builders.add(new BuilderResult((Builder<?, CompilationUnit>) builder, result));
  }
  
  @SuppressWarnings("unchecked")
  public void addBuilder(Builder<?, ?> builder, CompilationUnit result) {
    builders.add(new BuilderResult((Builder<?, CompilationUnit>) builder, result));
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
    return "Required builder failed. Error occurred in build step \"" + p.builder.taskDescription() + "\": " + getCause().getMessage();
  }
}
