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

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

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
      if (!Files.isDirectory(p) || Files.list(p).findAny().isPresent())
        FileCommands.delete(p);
  }

  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    return build(buildReq, defaultReport());
  }
  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq, IReporting report) {
    Pair<BuildManager, Boolean> manager = getBuildManagerForCurrentThread(report);
    try {
      return manager.b ? manager.a.requireInitially(buildReq).getBuildResult() : manager.a.require(buildReq, true).getUnit().getBuildResult();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (manager.b)
        activeManagers.remove(Thread.currentThread());
    }
  }

  public static <Out extends Output> List<Out> buildAll(Iterable<BuildRequest<?, Out, ?, ?>> buildReqs) {
    return buildAll(buildReqs);
  }
  public static <Out extends Output> List<Out> buildAll(Iterable<BuildRequest<?, Out, ?, ?>> buildReqs, IReporting report) {
    Pair<BuildManager, Boolean> manager = getBuildManagerForCurrentThread(report);

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
      if (manager.b)
        activeManagers.remove(Thread.currentThread());
    }
  }

  private static Pair<BuildManager, Boolean> getBuildManagerForCurrentThread(IReporting report) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager(report);
      activeManagers.put(current, manager);
    }
    return new Pair<>(manager, freshManager);
  }


}
