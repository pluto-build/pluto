package build.pluto.executor.config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
	private Map<String, Target> targets;
	private List<File> builderSourceDirs;
	private File builderTargetDir;
	private List<Dependency> dependencies;
	
	public void makePathsAbsolute(File parent) {
		if (builderTargetDir != null && !builderTargetDir.isAbsolute())
			builderTargetDir = new File(parent, builderTargetDir.getPath());
		if (builderSourceDirs != null) {
			List<File> newSource = new ArrayList<>(builderSourceDirs.size());
			for (File src : builderSourceDirs)
				if (!src.isAbsolute())
					newSource.add(new File(parent, src.getPath()));
				else
					newSource.add(src);
			builderSourceDirs = newSource;
		}
	}
	
	public void setTargets(List<Target> targets) {
		this.targets = new HashMap<>();
		for (Target t : targets)
			this.targets.put(t.getName(), t);
	}
	
	/*
	 * Needed for YAML-parsing Java Bean property targets.
	 */
	public List<Target> getTargets() {
		return new ArrayList<>(targets.values());
	}
	
	public Map<String, Target> getTargetsMap() {
		return this.targets;
	}

	public Target getTarget(String buildTarget) {
		return targets.get(buildTarget);
	}

	public List<File> getBuilderSourceDirs() {
		return builderSourceDirs;
	}

	public void setBuilderSourceDirs(List<File> builderSource) {
		this.builderSourceDirs = builderSource;
	}

	public File getBuilderTargetDir() {
		return builderTargetDir;
	}

	public void setBuilderTargetDir(File builderTarget) {
		this.builderTargetDir = builderTarget;
	}

	public List<Dependency> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}
}
