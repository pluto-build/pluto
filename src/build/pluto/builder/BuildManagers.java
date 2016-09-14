package build.pluto.builder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sugarj.common.FileCommands;
import org.sugarj.common.util.Pair;

import build.pluto.BuildUnit;
import build.pluto.output.Output;
import build.pluto.util.IReporting;
import build.pluto.util.LogReporting;
import build.pluto.util.TraceReporting;

public class BuildManagers {

  private final static ThreadLocal<Map<String, BuildManager>> activeManagers = new ThreadLocal<Map<String, BuildManager>>() {
    protected Map<String, BuildManager> initialValue() {
      return new HashMap<>();
    }
  };

  private static IReporting defaultReport() {
    return new TraceReporting(new LogReporting());
  }

  public static <Out extends Output> BuildUnit<Out> readResult(BuildRequest<?, Out, ?, ?> buildReq) throws IOException {
    return BuildUnit.read(buildReq.createBuilder().persistentPath());
  }

  public static void clean(boolean dryRun, BuildRequest<?, ?, ?, ?> req) throws IOException {
    clean(dryRun, req, defaultReport());
  }

  public static void clean(boolean dryRun, BuildRequest<?, ?, ?, ?> req, IReporting report) throws IOException {
    BuildUnit<?> unit = readResult(req);
    if (unit == null)
      return;
    Set<BuildUnit<?>> allUnits = unit.getTransitiveModuleDependencies();
    for (BuildUnit<?> next : allUnits) {
      for (File p : next.getGeneratedFiles())
        deleteFile(p.toPath(), dryRun, report);
      deleteFile(next.getPersistentPath().toPath(), dryRun, report);
    }
  }

  private static void deleteFile(Path p, boolean dryRun, IReporting report) throws IOException {
    report.messageFromSystem("Delete " + p + (dryRun ? " (dry run)" : ""), false, 1);
    if (!dryRun)
      if (!Files.isDirectory(p) || p.toFile().list().length != 0)
        FileCommands.delete(p);
  }

  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq) throws Throwable {
    return build(buildReq, defaultReport());
  }

  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq, IReporting report) throws Throwable {
    return build(buildReq, report, null);
  }
  
  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq, IReporting report, String path) throws Throwable {
    Pair<BuildManager, Boolean> manager = getBuildManagerForCurrentThread(report, path);
    try {
      return manager.b ? manager.a.requireInitially(buildReq).getBuildResult() : manager.a.require(buildReq, true).getUnit().getBuildResult();
    } finally {
      if (manager.b) {
        manager.a.close();
        removeBuildManagerForCurrentThread(path);
      }
    }
  }

  public static <Out extends Output> List<Out> buildAll(Iterable<? extends BuildRequest<?, Out, ?, ?>> buildReqs) throws Throwable {
    return buildAll(buildReqs, new LogReporting());
  }
  
  public static <Out extends Output> List<Out> buildAll(Iterable<? extends BuildRequest<?, Out, ?, ?>> buildReqs, IReporting report) throws Throwable {
    return buildAll(buildReqs, report, null);
  }

  public static <Out extends Output> List<Out> buildAll(Iterable<? extends BuildRequest<?, Out, ?, ?>> buildReqs, IReporting report, String path) throws Throwable {
    Pair<BuildManager, Boolean> manager = getBuildManagerForCurrentThread(report, path);

    try {
      List<Out> out = new ArrayList<>();
      for (BuildRequest<?, Out, ?, ?> buildReq : buildReqs)
        if (buildReq != null)
          try {
            out.add(manager.b ? manager.a.requireInitially(buildReq).getBuildResult() : manager.a.require(buildReq, true).getUnit().getBuildResult());
          } catch (IOException e) {
            e.printStackTrace();
            out.add(null);
          }
      return out;
    } finally {
      if (manager.b) {
        manager.a.close();
        removeBuildManagerForCurrentThread(path);
      }
    }
  }
  
  public static void resetDynamicAnalysis() throws IOException {
    resetDynamicAnalysis(null);
  }

  public static void resetDynamicAnalysis(String path) throws IOException {
    Pair<BuildManager, Boolean> manager = getBuildManagerForCurrentThread(defaultReport(), path);
    manager.a.resetDynamicAnalysis();
  }

  private static synchronized Pair<BuildManager, Boolean> getBuildManagerForCurrentThread(IReporting report, String path) {
    final Map<String, BuildManager> map = activeManagers.get();
    BuildManager manager = map.get(path);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(report, path);
      map.put(path, manager);
    }
    return new Pair<>(manager, freshManager);
  }
  
  private static synchronized void removeBuildManagerForCurrentThread(String path) {
    final Map<String, BuildManager> map = activeManagers.get();
    map.remove(path);
  }

}
