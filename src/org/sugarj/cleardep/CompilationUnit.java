package org.sugarj.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.sugarj.cleardep.stamp.ModuleStamp;
import org.sugarj.cleardep.stamp.PersistableEntityModuleStamper;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.cleardep.stamp.Util;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.cleardep.xattr.XattrAttributeViewStrategy;
import org.sugarj.common.AppendingIterable;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * Dependency management for modules.
 * 
 * @author Sebastian Erdweg
 */
abstract public class CompilationUnit extends PersistableEntity {

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

	/**
	 * Mirrors store alternative compilation results for the same compilation unit compiled with different modes.
	 */
	protected List<CompilationUnit> mirrors;
	protected Mode<?> mode;

	protected Synthesizer syn;
	protected Stamper defaultStamper;
	
	protected Map<CompilationUnit, ModuleStamp> moduleDependencies;
	protected Map<RelativePath, Stamp> sourceArtifacts;
	protected Map<Path, Stamp> externalFileDependencies;
	protected Map<Path, Stamp> generatedFiles;

	// **************************
	// Methods for initialization
	// **************************

	public static <E extends CompilationUnit> E create(Class<E> cl, Stamper stamper, Mode<E> mode, Synthesizer syn, Path dep) throws IOException {
		E e = PersistableEntity.tryReadElseCreate(cl, dep);
		e.init();
		e.defaultStamper = stamper;
		e.mode = mode;
		e.syn = syn;
		if (syn != null)
			syn.markSynthesized(e);
		
		return e;
	}

	/**
	 * Reads a CompilationUnit from memory or disk. The returned Compilation unit may or may not be consistent.
	 */
	@SuppressWarnings("unchecked")
	public static <E extends CompilationUnit> E read(Class<E> clazz, Mode<E> mode, Path... deps) throws IOException {
	  Set<Path> seen = new HashSet<>();
		for (Path dep : deps) {
		  if (seen.contains(dep))
		    continue;
		  seen.add(dep);
		  
		  E e = PersistableEntity.read(clazz, dep);
			if (e == null)
  		  continue;
  		
  		if (mode.canAccept(e))
  		  return mode.accept(e);
  		for (CompilationUnit m : e.mirrors) {
  		  if (seen.contains(m.persistentPath))
  		    continue;
  		  seen.add(m.persistentPath);
  		  if (mode.canAccept((E) m))
  		    return mode.accept((E) m);
  		}
		}
		return null;
	}
	
	/**
	 * Reads a CompilationUnit from memory or disk. The returned Compilation unit is guaranteed to be consistent.
	 * 
	 * @return null if no consistent compilation unit is available.
	 */
	@SuppressWarnings("unchecked")
  public static <E extends CompilationUnit> E readConsistent(Class<E> clazz, Mode<E> mode, Map<? extends Path, Stamp> editedSourceFiles, Path... deps) throws IOException {
	  Set<Path> seen = new HashSet<>();
	  for (Path dep : deps) {
	    if (seen.contains(dep))
        continue;
      seen.add(dep);
      
	    E e = PersistableEntity.read(clazz, dep);
      if (e == null)
        continue;
      
      if (mode.canAccept(e) && e.isConsistent(editedSourceFiles, mode))
        return mode.accept(e);
      for (CompilationUnit m : e.mirrors) {
        if (seen.contains(m.persistentPath))
          continue;
        seen.add(m.persistentPath);
        if (mode.canAccept((E) m) && m.isConsistent(editedSourceFiles, mode))
          return mode.accept((E) m);
      }
	  }
    return null;
  }

//	protected void copyContentTo(CompilationUnit compiled, Mode<?> mode) {
//		compiled.sourceArtifacts.putAll(sourceArtifacts);
//
//		for (Entry<CompilationUnit, Integer> entry : moduleDependencies.entrySet()) {
//			CompilationUnit dep = entry.getKey();
//			if (dep.compiledCompilationUnit == null)
//				compiled.moduleDependencies.put(dep, entry.getValue());
//			else
//				compiled.addModuleDependency(dep.compiledCompilationUnit);
//		}
//
//		for (Entry<CompilationUnit, Integer> entry : circularModuleDependencies.entrySet()) {
//			CompilationUnit dep = entry.getKey();
//			if (dep.compiledCompilationUnit == null)
//				compiled.circularModuleDependencies.put(dep, entry.getValue());
//			else
//			  compiled.circularModuleDependencies.put(dep.compiledCompilationUnit, dep.compiledCompilationUnit.getInterfaceHash());
//		}
//
//		for (Path p : externalFileDependencies.keySet())
//			compiled.addExternalFileDependency(FileCommands.tryCopyFile(targetDir, compiled.targetDir, p));
//
//		for (Path p : generatedFiles.keySet())
//			compiled.addGeneratedFile(FileCommands.tryCopyFile(targetDir, compiled.targetDir, p));
//	}

//	protected void liftEditedToCompiled() throws IOException {
//		ModuleVisitor<Void> liftVisitor = new ModuleVisitor<Void>() {
//			@Override
//			public Void visit(CompilationUnit mod, Mode mode) {
//				if (mod.compiledCompilationUnit == null)
//					return null;
//				// throw new IllegalStateException("compiledCompilationUnit of "
//				// + mod +
//				// " must not be null");
//
//				CompilationUnit edited = mod;
//				CompilationUnit compiled = mod.compiledCompilationUnit;
//				compiled.init();
//				edited.copyContentTo(compiled);
//
//				try {
//					compiled.write();
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//
//				return null;
//			}
//
//			@Override
//			public Void combine(Void t1, Void t2) {
//				return null;
//			}
//
//			@Override
//			public Void init() {
//				return null;
//			}
//
//			@Override
//			public boolean cancel(Void t) {
//				return false;
//			}
//		};
//
//		if (editedCompilationUnit != null)
//			editedCompilationUnit.visit(liftVisitor);
//		else
//			this.visit(liftVisitor);
//	}

	@Override
	protected void init() {
	  mirrors = new ArrayList<>();
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
          public Boolean visit(CompilationUnit mod, Mode<?> mode) {
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
          throw new IllegalDependencyException("Build unit " + getPersistentPath() + " has a hidden dependency on file " + file + " without build-unit dependency on " + dep + ", which generated this file. The current builder " + FileCommands.fileName(getPersistentPath()) + " should mark a dependency to " + FileCommands.fileName(dep) + " by `requiring` the corresponding builder.");
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
	public void addModuleDependency(CompilationUnit mod) {
	  addModuleDependency(mod, PersistableEntityModuleStamper.minstance.stampOf(mod));
	}
	public void addModuleDependency(CompilationUnit mod, ModuleStamp stamp) {
	  Objects.requireNonNull(mod);
	  Objects.requireNonNull(stamp);
		this.moduleDependencies.put(mod, stamp);
	}

	/**
	
	 * @param mod
	 * @see GraphUtils#repairGraph(Set)
	 */
	protected void removeModuleDependency(CompilationUnit mod) {
		// Just remove from both maps because mod is exactly in one
		this.moduleDependencies.remove(mod);
	}

//	public void updateModuleDependencyInterface(CompilationUnit mod) {
//		if (mod == null) {
//			throw new NullPointerException("Cannot handle null unit");
//		}
//		if (this.moduleDependencies.containsKey(mod)) {
//			this.moduleDependencies.put(mod, mod.getInterfaceHash());
//		} else if (this.circularModuleDependencies.containsKey(mod)) {
//			this.circularModuleDependencies.put(mod, mod.getInterfaceHash());
//		} else {
//			throw new IllegalArgumentException("Given CompilationUnit " + mod + " is not a dependency of this module");
//		}
//	}


	// *********************************
	// Methods for querying dependencies
	// *********************************

	public boolean dependsOn(CompilationUnit other) {
		return getModuleDependencies().contains(other) ;
	}

	public boolean dependsOnTransitively(final CompilationUnit other) {
	  return visit(new ModuleVisitor<Boolean>() {
      @Override
      public Boolean visit(CompilationUnit mod, Mode<?> mode) {
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

	public Synthesizer getSynthesizer() {
		return syn;
	}

	// ********************************************
	// Methods for checking compilation consistency
	// ********************************************

	protected abstract boolean isConsistentExtend();
	
	public Mode<?> getMode() {
	  return mode;
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
		if (hasPersistentVersionChanged())
			return false;
		
		if (!isFinished())
      return false;

		if (!isConsistentWithSourceArtifacts(editedSourceFiles))
			return false;

		for (Entry<Path, Stamp> e : generatedFiles.entrySet())
			if (!Util.stampEqual(e.getValue(), e.getKey()))
				return false;

		for (Entry<Path, Stamp> e : externalFileDependencies.entrySet())
			if (!Util.stampEqual(e.getValue(), e.getKey()))
				return false;

		if (!isConsistentModuleDependencies())
		  return false;
		
		if (!isConsistentExtend())
			return false;

		return true;
	}

	public boolean isConsistentModuleDependencies() {
		if (!this.isConsistentModuleDependenciesMap(this.moduleDependencies)) {
			return false;
		}
//		return this.isConsistentModuleDependenciesMap(this.circularModuleDependencies);
		return true;
	}

	private boolean isConsistentModuleDependenciesMap(Map<CompilationUnit, ModuleStamp> unitMap) {
		for (Entry<CompilationUnit, ModuleStamp> e : unitMap.entrySet())
		  if (!Util.stampEqual(e.getValue(), e.getKey()))
		    return false;
		  
		return true;
	}

	public boolean isConsistent(final Map<? extends Path, Stamp> editedSourceFiles, Mode<?> mode) {
		ModuleVisitor<Boolean> isConsistentVisitor = new ModuleVisitor<Boolean>() {
			@Override
			public Boolean visit(CompilationUnit mod, Mode<?> mode) {
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
		return visit(isConsistentVisitor, mode);
	}

	// *************************************
	// Methods for visiting the module graph
	// *************************************

	public static interface ModuleVisitor<T> {
		public T visit(CompilationUnit mod, Mode<?> mode);

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
		return visit(visitor, null);
	}

	public <T> T visit(ModuleVisitor<T> visitor, Mode<?> thisMode) {
		return visit(visitor, thisMode, false);
	}

	public <T> T visit(ModuleVisitor<T> visitor, Mode<?> thisMode, boolean reverseOrder) {
		List<CompilationUnit> topologicalOrder = GraphUtils.sortTopologicalFrom(this);
		if (reverseOrder) {
			Collections.reverse(topologicalOrder);
		}

		// System.out.println("Calculated list: " + sortedUnits);

		// Now iterate over the calculated list
		T result = visitor.init();
		for (CompilationUnit mod : topologicalOrder) {
			Mode<?> mode = thisMode;
			// Mode for required modules iff mod it not this and thismode not
			// null
			if (this != mod && mode != null) {
				mode = mode.getModeForRequiredModules();
			}
			T newResult = visitor.visit(mod, mode);
			result = visitor.combine(result, newResult);
			if (visitor.cancel(result))
				break;
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
	  mirrors = (List<CompilationUnit>) in.readObject();
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
			ModuleStamp interfaceHash = (ModuleStamp) in.readObject();
			if (mod == null)
				throw new IOException("Required module cannot be read: " + path);
			moduleDependencies.put(mod, interfaceHash);
		}

		boolean hasSyn = in.readBoolean();
		if (hasSyn) {
			Set<CompilationUnit> modules = new HashSet<CompilationUnit>();
			int modulesCount = in.readInt();
			for (int i = 0; i < modulesCount; i++) {
				String clName = (String) in.readObject();
				Class<? extends CompilationUnit> cl = (Class<? extends CompilationUnit>) getClass().getClassLoader().loadClass(clName);
				Path path = (Path) in.readObject();
				CompilationUnit mod = PersistableEntity.read(cl, path);
				if (mod == null)
					throw new IOException("Required module cannot be read: " + path);
				modules.add(mod);
			}
			Map<Path, Stamp> files = (Map<Path, Stamp>) in.readObject();
			syn = new Synthesizer(modules, files);
		}
	}
	
	public void write() throws IOException {
    super.write(defaultStamper);
  }

	 /**
   *  Contributed state `mode` must be written by subclass.
   */
	@Override
	protected void writeEntity(ObjectOutputStream out) throws IOException {
	  out.writeObject(state);
	  out.writeObject(mirrors);
		out.writeObject(sourceArtifacts = Collections.unmodifiableMap(sourceArtifacts));
		out.writeObject(generatedFiles = Collections.unmodifiableMap(generatedFiles));
		out.writeObject(externalFileDependencies = Collections.unmodifiableMap(externalFileDependencies));

		out.writeInt(moduleDependencies.size());
		for (Entry<CompilationUnit, ModuleStamp> entry : moduleDependencies.entrySet()) {
			CompilationUnit mod = entry.getKey();
			assert mod.isPersisted() : "Required compilation units must be persisted.";
			out.writeObject(mod.getClass().getCanonicalName());
			out.writeObject(mod.persistentPath);
			out.writeObject(entry.getValue());
		}

		out.writeBoolean(syn != null);
		if (syn != null) {
			out.writeInt(syn.generatorModules.size());
			for (CompilationUnit mod : syn.generatorModules) {
				out.writeObject(mod.getClass().getCanonicalName());
				out.writeObject(mod.persistentPath);
			}

			out.writeObject(syn.files);
		}
	}
}
