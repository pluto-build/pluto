package org.sugarj.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.sugarj.cleardep.build.BuildRequirement;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.cleardep.stamp.Util;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * Dependency management for modules.
 * 
 * @author Sebastian Erdweg
 */
public class CompilationUnit extends PersistableEntity {

	public static final long serialVersionUID = -5713504273621720673L;
	
	public static final Xattr xattr = Xattr.getDefault();
	
	public static enum State {
	  NEW, INITIALIZED, IN_PROGESS, SUCCESS, FAILURE;
	  
	  public static State finished(boolean success) {
	    return success ? SUCCESS : FAILURE;
	  }
	}

	public CompilationUnit() { /* for deserialization only */ }
	
	private State state = State.NEW;

	protected Stamper defaultStamper;
	
	protected Map<CompilationUnit, BuildRequirement<?, ?, ?, ?>> moduleDependencies;
	protected Map<RelativePath, Stamp> sourceArtifacts;
	protected Map<Path, Stamp> externalFileDependencies;
	protected Map<Path, Stamp> generatedFiles;
	
	protected BuildRequirement<?, ?, ?, ?> generatedBy;

	// **************************
	// Methods for initialization
	// **************************

	public static <E extends CompilationUnit> E create(Class<E> cl, Stamper stamper, Path dep) throws IOException {
	  return create(cl, stamper, dep, null);
	}
	
	public static <E extends CompilationUnit> E create(Class<E> cl, Stamper stamper, Path dep, BuildRequirement<?, E, ?, ?> generatedBy) throws IOException {
		E e = PersistableEntity.tryReadElseCreate(cl, dep);
		e.init();
		e.defaultStamper = stamper;
		e.generatedBy = generatedBy;
		return e;
	}

	final public static <E extends CompilationUnit> E read(Class<E> clazz, Path dep, BuildRequirement<?, E, ?, ?> generatedBy) throws IOException {
	  E e = read(clazz, dep);
	  if (e != null && e.generatedBy.equals(generatedBy))
	    return e;
    return null;
  }
	
	/**
	 * Reads a CompilationUnit from memory or disk. The returned Compilation unit is guaranteed to be consistent.
	 * 
	 * @return null if no consistent compilation unit is available.
	 */
  public static <E extends CompilationUnit> E readConsistent(Class<E> clazz, Map<? extends Path, Stamp> editedSourceFiles, Path dep, BuildRequirement<?, E, ?, ?> generatedBy) throws IOException {
	  E e = read(clazz, dep, generatedBy);
	  if (e != null && e.isConsistent(editedSourceFiles))
	    return e;
	  return null;
  }

	@Override
	protected void init() {
		sourceArtifacts = new HashMap<>();
		moduleDependencies = new HashMap<>();
		externalFileDependencies = new HashMap<>();
		generatedFiles = new HashMap<>();
		state = State.INITIALIZED;
	}

	// *******************************
	// Methods for adding dependencies
	// *******************************

	public void addSourceArtifact(RelativePath file) {
		addSourceArtifact(file, defaultStamper.stampOf(file));
	}

	public void addSourceArtifact(RelativePath file, Stamp stampOfFile) {
		sourceArtifacts.put(file, stampOfFile);
		checkUnitDependency(file);
	}
	
  public void addExternalFileDependency(Path file) {
		addExternalFileDependency(file, defaultStamper.stampOf(file));
	}

	public void addExternalFileDependency(Path file, Stamp stampOfFile) {
		externalFileDependencies.put(file, stampOfFile);
		checkUnitDependency(file);
	}
	
	private void checkUnitDependency(Path file) {
	  if (FileCommands.exists(file)) {
	    try {
        final Path dep = xattr.getGenBy(file);
        if (dep == null)
          return;
        
        boolean foundDep = visit(new ModuleVisitor<Boolean>() {
          @Override
          public Boolean visit(CompilationUnit mod) {
            return dep.equals(mod.getPersistentPath());
          }

          @Override
          public Boolean combine(Boolean t1, Boolean t2) {
            return t1 || t2;
          }

          @Override
          public Boolean init() {
            return false;
          }

          @Override
          public boolean cancel(Boolean t) {
            return t;
          }
        });
        
        if (!foundDep)
          throw new IllegalDependencyException("Build unit " + FileCommands.tryGetRelativePath(getPersistentPath()) + " has a hidden dependency on file " + FileCommands.tryGetRelativePath(file) + " without build-unit dependency on " + dep + ", which generated this file. The current builder " + FileCommands.fileName(getPersistentPath()) + " should mark a dependency to " + FileCommands.tryGetRelativePath(dep) + " by `requiring` the corresponding builder.");
      } catch (IOException e) {
        Log.log.log("WARNING: Could not verify build-unit dependency due to exception \"" + e.getMessage() + "\" while reading metadata: " + file, Log.IMPORT);
      }
	  }
  }

  public void addGeneratedFile(Path file) {
		addGeneratedFile(file, defaultStamper.stampOf(file));
	}

	public void addGeneratedFile(Path file, Stamp stampOfFile) {
		generatedFiles.put(file, stampOfFile);
		try {
		  if (FileCommands.exists(file)) 
		    xattr.setGenBy(file, this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
	}
	
	public void addModuleDependency(CompilationUnit mod, BuildRequirement<?, ?, ?, ?> req) {
	  Objects.requireNonNull(mod);
	  Objects.requireNonNull(req);
		this.moduleDependencies.put(mod, req);
	}

	/**
	
	 * @param mod
	 * @see GraphUtils#repairGraph(Set)
	 */
	protected void removeModuleDependency(CompilationUnit mod) {
		// Just remove from both maps because mod is exactly in one
		this.moduleDependencies.remove(mod);
	}


	// *********************************
	// Methods for querying dependencies
	// *********************************

	public boolean dependsOn(CompilationUnit other) {
		return getModuleDependencies().contains(other) ;
	}

	public boolean dependsOnTransitively(final CompilationUnit other) {
	  return visit(new ModuleVisitor<Boolean>() {
      @Override
      public Boolean visit(CompilationUnit mod) {
        return mod.equals(other);
      }

      @Override
      public Boolean combine(Boolean t1, Boolean t2) {
        return t1 || t2;
      }

      @Override
      public Boolean init() {
        return false;
      }

      @Override
      public boolean cancel(Boolean t) {
        return t;
      }
    });
	}

	public Set<RelativePath> getSourceArtifacts() {
		return sourceArtifacts.keySet();
	}

	public Set<CompilationUnit> getModuleDependencies() {
		return moduleDependencies.keySet();
	}

	public Set<Path> getExternalFileDependencies() {
		return externalFileDependencies.keySet();
	}

	public Set<Path> getGeneratedFiles() {
		return generatedFiles.keySet();
	}
	
	public BuildRequirement<?, ?, ?, ?> getGeneratedBy() {
    return generatedBy;
  }

	public Set<Path> getCircularFileDependencies() throws IOException {
		Set<Path> dependencies = new HashSet<Path>();
		Set<CompilationUnit> visited = new HashSet<>();
		LinkedList<CompilationUnit> queue = new LinkedList<>();
		queue.add(this);

		while (!queue.isEmpty()) {
			CompilationUnit res = queue.pop();
			visited.add(res);

			for (Path p : res.generatedFiles.keySet())
				if (!dependencies.contains(p) && FileCommands.exists(p))
					dependencies.add(p);
			for (Path p : res.externalFileDependencies.keySet())
				if (!dependencies.contains(p) && FileCommands.exists(p))
					dependencies.add(p);

			for (CompilationUnit nextDep : res.getModuleDependencies())
				if (!visited.contains(nextDep) && !queue.contains(nextDep))
					queue.addFirst(nextDep);
		}

		return dependencies;
	}


	// ********************************************
	// Methods for checking compilation consistency
	// ********************************************

	protected boolean isConsistentExtend() {
	  return true;
	}
	
	public State getState() {
	  return state;
	}

	public Path getPersistentPath() {
    return persistentPath;
  }
  	
  public void setState(State state) {
	  this.state = state;
	}
	
	public boolean isFinished() {
	  return state == State.FAILURE || state == State.SUCCESS;
	}
	
	public boolean hasFailed() {
	  return state == State.FAILURE;
	}

	protected boolean isConsistentWithSourceArtifacts(Map<? extends Path, Stamp> editedSourceFiles) {
//		if (sourceArtifacts.isEmpty())
//			return false;

		boolean hasEdits = editedSourceFiles != null;
		for (Entry<RelativePath, Stamp> e : sourceArtifacts.entrySet()) {
		  Stamp editStamp = hasEdits ? editedSourceFiles.get(e.getKey()) : null;
			if (editStamp != null && !editStamp.equals(e.getValue())) {
				return false;
			} else if (editStamp == null && !Util.stampEqual(e.getValue(), e.getKey())) {
				return false;
			}
		}

		return true;
	}
	
	public boolean isConsistentShallow(Map<? extends Path, Stamp> editedSourceFiles) {
	  return isConsistentShallowReason(editedSourceFiles) == InconsistenyReason.NO_REASON;
	}

	  
	public static enum InconsistenyReason implements Comparable<InconsistenyReason>{
	  NO_REASON, DEPENDENCIES_NOT_CONSISTENT, FILES_NOT_CONSISTENT, OTHER, 
	  
	}
	  
	public InconsistenyReason isConsistentShallowReason(Map<? extends Path, Stamp> editedSourceFiles) {
		if (hasPersistentVersionChanged())
			return InconsistenyReason.OTHER;
		
		if (!isFinished())
      return InconsistenyReason.OTHER;

		if (!isConsistentWithSourceArtifacts(editedSourceFiles))
			return InconsistenyReason.FILES_NOT_CONSISTENT;

		for (Entry<Path, Stamp> e : generatedFiles.entrySet())
			if (!Util.stampEqual(e.getValue(), e.getKey()))
				return InconsistenyReason.FILES_NOT_CONSISTENT;

		for (Entry<Path, Stamp> e : externalFileDependencies.entrySet())
			if (!Util.stampEqual(e.getValue(), e.getKey()))
				return InconsistenyReason.FILES_NOT_CONSISTENT;

		if (!isConsistentModuleDependencies())
		  return InconsistenyReason.DEPENDENCIES_NOT_CONSISTENT;
		
		if (!isConsistentExtend())
			return InconsistenyReason.OTHER;

		return InconsistenyReason.NO_REASON;
	}

	public boolean isConsistentModuleDependencies() {
		if (!this.isConsistentModuleDependenciesMap(this.moduleDependencies)) {
			return false;
		}
//		return this.isConsistentModuleDependenciesMap(this.circularModuleDependencies);
		return true;
	}

	private boolean isConsistentModuleDependenciesMap(Map<CompilationUnit, BuildRequirement<?, ?, ?, ?>> unitMap) {
		for (Entry<CompilationUnit, BuildRequirement<?, ?, ?, ?>> e : unitMap.entrySet())
		  if (!e.getKey().getGeneratedBy().equals(e.getValue()))
		    return false;
		  
		return true;
	}

	public boolean isConsistent(final Map<? extends Path, Stamp> editedSourceFiles) {
		ModuleVisitor<Boolean> isConsistentVisitor = new ModuleVisitor<Boolean>() {
			@Override
			public Boolean visit(CompilationUnit mod) {
				return mod.isConsistentShallow(editedSourceFiles);
			}

			@Override
			public Boolean combine(Boolean t1, Boolean t2) {
				return t1 && t2;
			}

			@Override
			public Boolean init() {
				return true;
			}

			@Override
			public boolean cancel(Boolean t) {
				return !t;
			}
		};
		return visit(isConsistentVisitor);
	}

	// *************************************
	// Methods for visiting the module graph
	// *************************************

	public static interface ModuleVisitor<T> {
		public T visit(CompilationUnit mod);

		public T combine(T t1, T t2);

		public T init();

		public boolean cancel(T t);
	}

	/**
	 * Visits the module graph for this module. If a module m1 depends
	 * transitively on m2 and m2 does not depends transitively on m1, then m1 is
	 * visited before m2. If there is a cyclic dependency between module m1 and
	 * m2 and m1 depends on m2 transitively on the spanning directed acyclic
	 * graph, then m1 is visited before m2.
	 */
	public <T> T visit(ModuleVisitor<T> visitor) {
	  Queue<CompilationUnit> queue = new ArrayDeque<>();
	  queue.add(this);
	  
	  Set<CompilationUnit> seenUnits = new HashSet<>();
    seenUnits.add(this);
	  
	  T result = visitor.init();
	  while(!queue.isEmpty()) {
	    CompilationUnit toVisit = queue.poll();
      T newResult = visitor.visit(toVisit);
      result = visitor.combine(result, newResult);
      if (visitor.cancel(result))
        break;
      
      for (CompilationUnit dep : toVisit.getModuleDependencies()) {
        if (!seenUnits.contains(dep)) {
          queue.add(dep);
          seenUnits.add(dep);
        }
      }
	  }
	  
	return result;
	}

	
	// *************************
	// Methods for serialization
	// *************************

	/**
	 *  Contributed state `mode` must be read by subclass.
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void readEntity(ObjectInputStream in) throws IOException, ClassNotFoundException {
	  state = (State) in.readObject();
		sourceArtifacts = (Map<RelativePath, Stamp>) in.readObject();
		generatedFiles = (Map<Path, Stamp>) in.readObject();
		externalFileDependencies = (Map<Path, Stamp>) in.readObject();

		int moduleDepencyCount = in.readInt();
		moduleDependencies = new HashMap<>(moduleDepencyCount);
		for (int i = 0; i < moduleDepencyCount; i++) {
			String clName = (String) in.readObject();
			Class<? extends CompilationUnit> cl = (Class<? extends CompilationUnit>) getClass().getClassLoader().loadClass(clName);
			Path path = (Path) in.readObject();
			CompilationUnit mod = PersistableEntity.read(cl, path);
			BuildRequirement<?, ?, ?, ?> req = (BuildRequirement<?, ?, ?, ?>) in.readObject();
			if (mod == null)
				throw new IOException("Required module cannot be read: " + path);
			moduleDependencies.put(mod, req);
		}

		boolean hasGeneratedBy = in.readBoolean();
		if (hasGeneratedBy) {
		  this.generatedBy = (BuildRequirement<?, ?, ?, ?>) in.readObject();
		}
	}
	
	public void write() throws IOException {
    super.write(defaultStamper);
  }

	@Override
	protected void writeEntity(ObjectOutputStream out) throws IOException {
	  out.writeObject(state);
		out.writeObject(sourceArtifacts = Collections.unmodifiableMap(sourceArtifacts));
		out.writeObject(generatedFiles = Collections.unmodifiableMap(generatedFiles));
		out.writeObject(externalFileDependencies = Collections.unmodifiableMap(externalFileDependencies));

		out.writeInt(moduleDependencies.size());
		for (Entry<CompilationUnit, BuildRequirement<?, ?, ?, ?>> entry : moduleDependencies.entrySet()) {
			CompilationUnit mod = entry.getKey();
			assert mod.isPersisted() : "Required compilation units must be persisted.";
			out.writeObject(mod.getClass().getCanonicalName());
			out.writeObject(mod.persistentPath);
			out.writeObject(entry.getValue());
		}

		out.writeBoolean(this.generatedBy != null);
		out.writeObject(generatedBy);
		
	}
}
