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
import org.sugarj.common.Log;

import build.pluto.BuildUnit;
import build.pluto.output.Output;

public class BuildManagers {

  private final static Map<Thread, BuildManager> activeManagers = new HashMap<>();

  public static <Out extends Output> BuildUnit<Out> readResult(BuildRequest<?, Out, ?, ?> buildReq) throws IOException {
    return BuildUnit.read(buildReq.createBuilder().persistentPath());
  }

  public static void clean(boolean dryRun, BuildRequest<?, ?, ?, ?> req) throws IOException {
    BuildUnit<?> unit = readResult(req);
    if (unit == null)
      return;
    Set<BuildUnit<?>> allUnits = unit.getTransitiveModuleDependencies();
    for (BuildUnit<?> next : allUnits) {
      for (File p : next.getGeneratedFiles())
        deleteFile(p.toPath(), dryRun);
      deleteFile(next.getPersistentPath().toPath(), dryRun);
    }
  }

  private static void deleteFile(Path p, boolean dryRun) throws IOException {
    Log.log.log("Delete " + p + (dryRun ? " (dry run)" : ""), Log.CORE);
    if (!dryRun)
      if (!Files.isDirectory(p) || Files.list(p).findAny().isPresent())
        FileCommands.delete(p);
  }

  public static <Out extends Output> Out build(BuildRequest<?, Out, ?, ?> buildReq) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager();
      activeManagers.put(current, manager);
    }

    try {
      return manager.requireInitially(buildReq).getBuildResult();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

  public static <Out extends Output> List<Out> buildAll(BuildRequest<?, Out, ?, ?>[] buildReqs) {
    Thread current = Thread.currentThread();
    BuildManager manager = activeManagers.get(current);
    boolean freshManager = manager == null;
    if (freshManager) {
      manager = new BuildManager();
      activeManagers.put(current, manager);
    }

    try {
      List<Out> out = new ArrayList<>();
      for (BuildRequest<?, Out, ?, ?> buildReq : buildReqs)
        if (buildReq != null)
          try {
            out.add(manager.requireInitially(buildReq).getBuildResult());
          } catch (IOException e) {
            e.printStackTrace();
            out.add(null);
          }
      return out;
    } finally {
      if (freshManager)
        activeManagers.remove(current);
    }
  }

}
