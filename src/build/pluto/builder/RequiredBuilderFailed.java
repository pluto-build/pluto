package build.pluto.builder;

import java.util.ArrayList;
import java.util.List;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;

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
  
  /**
   * The cause of a required builder failed exception discards the last added builder
   * 
   * @see java.lang.Throwable#getCause()
   */
  @Override
  public synchronized Throwable getCause() {
    if (builders.size() == 1)
      return super.getCause();
    
    RequiredBuilderFailed e = new RequiredBuilderFailed(builders.get(0), super.getMessage());
    int to = builders.size() - 1;
    for (int i = 1; i < to; i++) // omit last builder
      e.addBuilder(builders.get(i));
    return e;
  }
  
  /**
   * @return the original exception that made the innermost build fail.
   */
  public Throwable getRootCause() {
    return super.getCause();
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
    return "Required builder failed. Error occurred in build step \"" + p.getRequest().createBuilder().description() + "\": " + (super.getCause() == null ? super.getMessage() : super.getCause().getMessage());
  }

  protected <Out extends Output> RequiredBuilderFailed enqueueBuilder(BuildUnit<Out> depResult, BuildRequest<?, Out, ?, ?> buildReq) {
    return enqueueBuilder(depResult, buildReq, true);
  }
  protected <Out extends Output> RequiredBuilderFailed enqueueBuilder(BuildUnit<Out> depResult, BuildRequest<?, Out, ?, ?> buildReq, boolean addDependency) {
    BuildRequirement<?> required = getLastAddedBuilder();
    if (addDependency)
      depResult.requires(required);
    depResult.setState(BuildUnit.State.FAILURE);
  
    addBuilder(new BuildRequirement<>(depResult, buildReq));
    return this;
  }
  
  static RequiredBuilderFailed init(BuildRequirement<?> buildReq, Throwable cause) {
    return new RequiredBuilderFailed(buildReq, cause);
  }
}
