package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.CompilationUnit;

public class RequiredBuilderFailed extends RuntimeException {
  private static final long serialVersionUID = 3080806736856580512L;

  public static class BuilderResult {

    public Builder<?, ?> builder;
    public CompilationUnit result;
    public BuilderResult(Builder<?, ?> builder, CompilationUnit result) {
      this.builder = builder;
      this.result = result;
    }
  }
  
  private List<BuilderResult> builders;
  
  public <T> RequiredBuilderFailed(Builder<?, ?> builder, CompilationUnit result, Throwable cause) {
    super(cause);
    builders = new ArrayList<>();
    builders.add(new BuilderResult(builder, result));
  }
  
  public void addBuilder(Builder<?, ?> builder, CompilationUnit result) {
    builders.add(new BuilderResult(builder, result));
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
