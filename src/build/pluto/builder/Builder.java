package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import build.pluto.BuildUnit;
import build.pluto.BuildUnit.State;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.IllegalDependencyException;
import build.pluto.dependency.Origin;
import build.pluto.dependency.Requirement;
import build.pluto.output.None;
import build.pluto.output.Output;
import build.pluto.output.OutputEqualStamper;
import build.pluto.output.OutputStamper;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.Stamper;
import build.pluto.tracing.FileAccessMode;
import build.pluto.tracing.FileDependency;
import build.pluto.tracing.ITracer;
import build.pluto.tracing.Tracer;

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

  public final static File PLUTO_HOME = new File(System.getProperty("user.home"), ".pluto");
  
  private final In input;

  transient BuildUnit<Out> result;
  private transient BuildUnit<Out> previousResult;
  
  transient BuildUnitProvider manager;
  private transient Stamper defaultStamper;
  
  private transient BuildRequest<?, ?, ?, ?> lastBuildReq;

  public Builder(In input) {
    this.input = input;
  }

  protected In getInput() {
    return input;
  }
  
  protected BuildRequest<?, ?, ?, ?> lastBuildReq() {
    return lastBuildReq;
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
  public abstract File persistentPath(In input);

  final File persistentPath() {
    return this.persistentPath(input).getAbsoluteFile();
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
   * Returns the {@link CycleHandlerFactory} this builder provides if it is
   * involved in a cycle. By default the builder does not support a cycle and
   * this method returns null.
   * 
   * @return the {@link CycleHandlerFactory} which can be used to resolve cycles
   *         that include this builder
   */
  protected CycleHandlerFactory getCycleSupport() {
    return null;
  }

  protected Stamper defaultStamper() {
    return LastModifiedStamper.instance;
  }

  Out triggerBuild(BuildUnit<Out> result, BuildUnitProvider manager, BuildUnit<Out> previousResult) throws Throwable {
    this.result = result;
    this.previousResult = previousResult;
    this.manager = manager;
    this.defaultStamper = defaultStamper();
    try {
      if (this.useFileDependencyDiscovery())
        manager.tracer.ensureStarted();
      Out res = build(this.input);
      if (this.useFileDependencyDiscovery()) {
        generateCurrentFileDependencies();
        //this.report("Stopping tracer...");
        //manager.tracer.stop();
      }
      return res;
    } finally {
      this.result = null;
      this.previousResult = null;
      this.manager = null;
      this.defaultStamper = null;
    }
  }

  private void generateCurrentFileDependencies() throws Tracer.TracingException {
    List<FileDependency> fileDeps = manager.tracer.popDependencies();
    this.report(fileDeps.toString());
    for (FileDependency d: fileDeps) {
      if (!d.getFile().getAbsoluteFile().equals(this.persistentPath().getAbsoluteFile())) {
        if (d.getMode() == FileAccessMode.READ_MODE)
          this.require(d.getFile());
        if (d.getMode() == FileAccessMode.WRITE_MODE)
          this.provide(d.getFile());
      }
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
   * @param ostamper
   *          the stamper used for stamping the required build's output
   * @return the output of the build
   * @throws IOException
   */
  public 
//@formatter:off
  <In_ extends Serializable, 
   Out_ extends Output, 
   B_ extends Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>, 
   SubIn_ extends In_>
//@formatter:on
  Out_ requireBuild(F_ factory, SubIn_ input, OutputStamper<? super Out_> ostamper) throws IOException {
    return requireBuild(new BuildRequest<In_, Out_, B_, F_>(factory, input, ostamper));
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
  public
//@formatter:off
  <In_ extends Serializable, 
   Out_ extends Output, 
   B_ extends Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>, 
   SubIn_ extends In_>
//@formatter:on
  Out_ requireBuild(F_ factory, SubIn_ input) throws IOException {
    return requireBuild(factory, input, OutputEqualStamper.instance());
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
  private 
//@formatter:off
  <In_ extends Serializable,
   Out_ extends Output, 
   B_ extends Builder<In_, Out_>, 
   F_ extends BuilderFactory<In_, Out_, B_>>
//@formatter:on
  Out_ requireBuild(BuildRequest<In_, Out_, B_, F_> req) throws IOException {
    if (this.useFileDependencyDiscovery())
      try {
        this.generateCurrentFileDependencies();
      } catch (Tracer.TracingException e) {
        // TODO: What to do here?
        e.printStackTrace();
      }
    lastBuildReq = req;
    BuildRequirement<Out_> e = manager.require(req, true);
    result.requires(e);
    if (this.useFileDependencyDiscovery())
      try {
        manager.tracer.popDependencies();
      } catch (ITracer.TracingException e1) {
        e1.printStackTrace();
      }
    return e.getUnit().getBuildResult();
  }

  /**
   * Requires that the build result of all given {@link BuildRequest}s is
   * consistent such that provided files can be required.
   * 
   * @param origin
   *          all requirements which are needed to be consistent
   * @throws IOException
   */
   public Collection<? extends Output> requireBuild(Origin origin) throws IOException {
    if (origin == null)
      return null;

    Collection<Output> outs = new ArrayList<>();
    for (BuildRequest<?, ?, ?, ?> req : origin.getReqs())
      outs.add(requireBuild(req));
    return outs;
  }

  public void requireOther(Requirement req) {
      result.requireOther(req);
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
      File path = result.getPersistentPath().getAbsoluteFile();
      for (File f : e.deps)
        if (f.getAbsoluteFile().equals(path))
          try {
            requireBuild(result.getGeneratedBy());
            return;
          } catch (IOException e1) {
            throw new RuntimeException(e1);
          }
      throw e;
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
  
  public void provide(FileRequirement req) {
    result.generates(req);
  }


  public void setState(State state) {
    result.setState(state);
  }

  protected final void report(String message) {
    manager.report.messageFromBuilder(message, false, this);
  }

  protected final void reportError(String message) {
    manager.report.messageFromBuilder(message, true, this);
  }

  protected BuildUnit<Out> getBuildUnit() {
    return result;
  }
  
  protected BuildUnit<Out> getPreviousBuildUnit() {
    return previousResult;
  }

  /**
   * This method determines if the builder explicitly calls provide and require, or if these file dependencies are automatically detected by a tracer.
   * If this method returns true, the defaultStamper() is used for all dependencies.
   * @return
   */
  protected boolean useFileDependencyDiscovery() { return false; }
}
