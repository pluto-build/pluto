package org.sugarj.cleardep.build;

import java.util.ArrayList;
import java.util.List;

import org.sugarj.cleardep.BuildUnit;
import org.sugarj.cleardep.BuildUnit.State;

public class RequiredBuilderFailed extends RuntimeException {
  private static final long serialVersionUID = 3080806736856580512L;

  public static class BuilderResult {

    public Builder<?, ?> builder;
    public BuildUnit<?> result;
    public BuilderResult(Builder<?, ?> builder, BuildUnit<?> result) {
      this.builder = builder;
      this.result = result;
    }
  }
  
  private List<BuilderResult> builders;
  
  public RequiredBuilderFailed(Builder<?, ?> builder, BuildUnit<?> result, Throwable cause) {
    super(cause);
    builders = new ArrayList<>();
    builders.add(new BuilderResult(builder, result));
  }
  
  public void addBuilder(Builder<?, ?> builder, BuildUnit<?> result) {
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
    return "Required builder failed. Error occurred in build step \"" + p.builder.description() + "\": " + getCause().getMessage();
  }

  static RequiredBuilderFailed enqueueBuilder(RequiredBuilderFailed e, BuildUnit<?> depResult, Builder<?,?> builder) {
    BuilderResult required = e.getLastAddedBuilder();
    depResult.requires(required.result);
    depResult.setState(BuildUnit.State.FAILURE);
  
    e.addBuilder(builder, depResult);
    return e;
  }
  
  static RequiredBuilderFailed init(Builder<?, ?> builder, BuildUnit<?> result, Throwable cause) {
    return new RequiredBuilderFailed(builder, result, cause);
  }
}
