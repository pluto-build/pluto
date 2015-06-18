package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.IllegalDependencyException;
import build.pluto.output.None;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.Stamper;

/**
 * The builder class is the abstract base class of each builder. It contains an
 * unimplemented build method, which each concrete builder needs to implement
 * and provides some methods to specify dependencies of a build. They are used
 * in the concrete implementation of the build method. Each file, the builder
 * reads or uses in any way needs a preceding call to a require Method, each
 * file the builder generates needs a later provide call. If the build result of
 * another builder is needed, this is required by a requireBuild call. If a file
 * is generated by another builder, the file can only be required, if the build
 * was required transitively. At no point the builder is allowed to mutate its
 * input.
 * 
 * @author Sebastian Erdweg, Moritz Lichter
 *
 * @param <In>
 *          the type of the builder input
 * @param <Out>
 *          the type of the builder output
 */
public abstract class Builder<In extends Serializable, Out extends Output> {

  private final In input;

  transient BuildUnit<Out> result;
  transient BuildUnitProvider manager;
  private transient Stamper defaultStamper;

  public Builder(In input) {
    this.input = input;
  }

  protected In getInput() {
    return input;
  }

  /**
   * Provides the task description for the builder and its input. The
   * description is used for console logging when the builder is run.
   * 
   * @return the task description or `null` if no logging is wanted.
   */
  protected abstract String description(In input);

  public final String description() {
    return this.description(this.input);
  }

  /**
   * Returns the file to persist the build summary for the input of the builder.
   * If for two inputs i1 and i2 and {@code !i1.equals(i2)}, then the absolute
   * paths of this method for two builders for i1 and i2 must be distinct.
   * 
   * @return the file where to persist the build summary
   */
  protected abstract File persistentPath(In input);

  final File persistentPath() {
    return this.persistentPath(input);
  }

  /**
   * Performs the build action for the input of the builder. The method may
   * throw any exception, in that case, the build is regarded as failed. In this
   * method the various requireBuild, require and provide methods of the builder
   * must be used to require other builder and require and provide files.
   * 
   * @return the result of building, if no in memory result is there and the
   *         build generates file only, you may want to use {@link None}.
   * @throws Throwable
   *           any exception raised during compilation to fail the build
   */
  protected abstract Out build(In input) throws Throwable;

  final Out build() throws Throwable {
    return build(this.input);
  }

  /**
   * Returns the {@link CycleSupportFactory} this builder provides if it is
   * involved in a cycle. By default the builder does not support a cycle and
   * this method returns null.
   * 
   * @return the {@link CycleSupportFactory} which can be used to resolve cycles
   *         that include this builder
   */
  protected CycleSupportFactory getCycleSupport() {
    return null;
  }

  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  Out triggerBuild(BuildUnit<Out> result, BuildUnitProvider manager) throws Throwable {
    this.result = result;
    this.manager = manager;
    this.defaultStamper = defaultStamper();
    try {
      return build(this.input);
    } finally {
      this.result = null;
      this.manager = null;
      this.defaultStamper = null;
    }
  }

  /**
   * Requires that the build result of the given factory with the given build
   * result is consistent. This may execute the builder or not. After a call
   * this builder may require any file provided by this build (or a transitive
   * build).
   * 
   * @param factory
   *          the builder factory, which produces a builder used to build
   * @param input
   *          the build input
   * @return the output of the build
   * @throws IOException
   */
  protected 
//@formatter:off
  <In_ extends Serializable, 
   Out_ extends Output, 
   B_ extends Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>, 
   SubIn_ extends In_>
//@formatter:on
  Out_ requireBuild(F_ factory, SubIn_ input) throws IOException {
    return requireBuild(new BuildRequest<In_, Out_, B_, F_>(factory, input));
  }

  /**
   * Requires the result of the given build request is consistent. After such a
   * call this builder may require any file provided by building the build
   * request (including transitively provided files).
   * 
   * @param req
   *          the request to build
   * @return the build output of the request
   * @throws IOException
   */
  protected 
//@formatter:off
  <In_ extends Serializable,
   Out_ extends Output, 
   B_ extends Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>>
//@formatter:on
  Out_ requireBuild(BuildRequest<In_, Out_, B_, F_> req) throws IOException {
    BuildRequirement<Out_> e = manager.require(req, true);
    result.requires(e);
    return e.getUnit().getBuildResult();
  }

  /**
   * Requires that the build result of all given {@link BuildRequest}s is
   * consistent such that provided files can be required.
   * 
   * @param reqs
   *          all requirements which are needed to be consistent
   * @throws IOException
   */
  protected void requireBuild(Collection<? extends BuildRequest<?, ?, ?, ?>> reqs) throws IOException {
    if (reqs != null)
      for (BuildRequest<?, ?, ?, ?> req : reqs) {
        BuildRequirement<?> e = manager.require(req, false);
        result.requires(e);
      }
  }

  /**
   * Requires that the build result of all given {@link BuildRequest}s is
   * consistent such that provided files can be required.
   * 
   * @param reqs
   *          all requirements which are needed to be consistent
   * @throws IOException
   */
  protected void requireBuild(BuildRequest<?, ?, ?, ?>[] reqs) throws IOException {
    if (reqs != null)
      for (BuildRequest<?, ?, ?, ?> req : reqs) {
        requireBuild(req);
      }
  }

  /**
   * Requires the given file stamped with the default stamper of this builder.
   * The call to require needs to be placed before any calculation is done which
   * depends on the file.
   * 
   * @param p
   *          the required file
   */
  public void require(File p) {
    require(p, defaultStamper.stampOf(p));
  }

  /**
   * Requires the given file stamped with the given stamper. The call to require
   * needs to be placed before any calculation is done which depends on the
   * file.
   * 
   * @param p
   *          the required file
   */
  public void require(File p, Stamper stamper) {
    require(p, stamper.stampOf(p));
  }

  public void require(File p, Stamp stamp) {
    try {
      result.requires(p, stamp);
    } catch (IllegalDependencyException e) {
      if (e.dep.equals(result.getPersistentPath()))
        try {
          requireBuild(result.getGeneratedBy());
        } catch (IOException e1) {
          throw new RuntimeException(e1);
        }
    }
  }

  /**
   * States that this builder provides the given file. The provide call has to
   * be made after the file has been generated completely. The file is stamped
   * with the last modified stamper. As long the last modified time does not
   * change, the file is regarded as consistent.
   * 
   * @param p
   *          the provided file
   */
  public void provide(File p) {
    result.generates(p, LastModifiedStamper.instance.stampOf(p));
  }

  /**
   * Provides the given file stamped with the given stamper. As long as the
   * stamp does not change, the file is regarded as consistent.The provide call
   * has to be made after the file has been generated completely.
   * 
   * @param p
   *          the provided file
   * @param stamper
   *          the stamper used to stamp the file
   */
  public void provide(File p, Stamper stamper) {
    result.generates(p, stamper.stampOf(p));
  }

  public void setState(State state) {
    result.setState(state);
  }

}
