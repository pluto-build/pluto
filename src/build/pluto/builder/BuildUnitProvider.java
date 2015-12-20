package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit;
import build.pluto.dependency.BuildRequirement;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.util.IReporting;

public abstract class BuildUnitProvider {

  protected final IReporting report;
  
  public BuildUnitProvider(IReporting report) {
    this.report = report;
  }
  
  public abstract
  //@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
 BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq, boolean needBuildResult) throws IOException;

  
  public
  //@formatter:off
  <In extends Serializable,
   Out extends Output,
   B extends Builder<In, Out>,
   F extends BuilderFactory<In, Out, B>>
  //@formatter:on
  BuildRequirement<Out> require(BuildRequest<In, Out, B, F> buildReq) throws IOException {
    return require(buildReq, true);
  }

  protected abstract Throwable tryCompileCycle(BuildCycleException e);

  
  
  // @formatter:off
  protected 
    <In extends Serializable,
     Out extends Output>
  // @formatter:on
  void setUpMetaDependency(Builder<In, Out> builder, BuildUnit<Out> depResult) throws IOException {
    if (depResult != null) {
      Path path = FileCommands.getRessourcePath(builder.getClass());
      if(path == null) {
        return;
      }
      File builderClass = path.toFile();

      File[] depFiles = DynamicAnalysis.XATTR.getGenBy(builderClass);
      boolean requireMeta = depFiles == null || depFiles.length == 0;
      
      if (depFiles != null) 
        for (File depFile : depFiles) 
          if (depFile.exists()) {
            try {
              BuildUnit<Out> metaBuilder = BuildUnit.read(depFile);
              if (metaBuilder != null) {
                depResult.requireMeta(metaBuilder);
                requireMeta = true;
              }
            } catch (RuntimeException e) {
              if (e.getCause() instanceof ClassNotFoundException) {
                /*
                 * Do nothing. This happens when the file was generated
                 * in separate JVM and requires builder classfiles not
                 * available to the current JVM. 
                 */
                report.messageFromSystem("Info: Could not setup metadependency to build script because class " + e.getMessage() + " is not available on current classpath.", false, 6);
              }
              else
                throw e;
            }
          }

      if (requireMeta)
        depResult.requires(builderClass, LastModifiedStamper.instance.stampOf(builderClass));
    }
  }
}
