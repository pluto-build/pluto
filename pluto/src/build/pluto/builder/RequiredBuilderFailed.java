package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;

public class RequiredBuilderFailed extends RuntimeException {
  private static final long serialVersionUID = 3080806736856580512L;

  private List<BuildRequirement<?>> builders;
  
  public RequiredBuilderFailed(BuildRequirement<?> buildReq, Throwable cause) {
    super(cause);
    builders = new ArrayList<>();
    builders.add(buildReq);
  }
  
  public RequiredBuilderFailed(BuildRequirement<?> buildReq, String message) {
    super(message);
    builders = new ArrayList<>();
    builders.add(buildReq);
  }
  
  private void addBuilder(BuildRequirement<?> buildReq) {
    builders.add(buildReq);
  }
  
  public BuildRequirement<?> getLastAddedBuilder() {
    return builders.get(builders.size() - 1);
  }

  public List<BuildRequirement<?>> getBuilders() {
    return builders;
  }
  
  @Override
  public String getMessage() {
    BuildRequirement<?> p = builders.get(0);
    return "Required builder failed. Error occurred in build step \"" + p.getRequest().createBuilder().description() + "\": " + (getCause() == null ? super.getMessage() : getCause().getMessage());
  }

  protected RequiredBuilderFailed enqueueBuilder(BuildUnit<?> depResult, Builder<?,?> builder) {
    return enqueueBuilder(depResult, builder, true);
  }
  protected RequiredBuilderFailed enqueueBuilder(BuildUnit<?> depResult, Builder<?,?> builder, boolean addDependency) {
    BuildRequirement<?> required = getLastAddedBuilder();
    if (addDependency)
      depResult.requires(required);
    depResult.setState(BuildUnit.State.FAILURE);
  
    addBuilder(required);
    return this;
  }
  
  static RequiredBuilderFailed init(BuildRequirement<?> buildReq, Throwable cause) {
    return new RequiredBuilderFailed(buildReq, cause);
  }
}
