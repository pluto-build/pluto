package org.sugarj.cleardep;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.sugarj.cleardep.build.BuildRequest;
import org.sugarj.cleardep.dependency.BuildRequirement;
import org.sugarj.cleardep.dependency.FileRequirement;
import org.sugarj.cleardep.dependency.Requirement;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.cleardep.stamp.Util;
import org.sugarj.cleardep.xattr.Xattr;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

/**
 * Dependency management for modules.
 * 
 * @author Sebastian Erdweg
 */
final public class BuildUnit<Out extends Serializable> extends PersistableEntity {

  public static final long serialVersionUID = -2821414386853890682L;

  public static final Xattr xattr = Xattr.getDefault();
	
	public static enum State {
	  NEW, INITIALIZED, IN_PROGESS, SUCCESS, FAILURE;
	  
	  public static State finished(boolean success) {
	    return success ? SUCCESS : FAILURE;
	  }
	}

	public BuildUnit() { /* for deserialization only */ }
	
	private State state = State.NEW;

	protected Stamper defaultStamper;
	
	protected List<Requirement> requirements;
	protected Set<FileRequirement> generatedFiles;

	protected Out buildResult;

	protected transient Set<BuildUnit<?>> requiredUnits;
	protected transient Set<Path> requiredFiles;
	
	protected BuildRequest<?, Out, ?, ?> generatedBy;
	
	// **************************
	// Methods for initialization
	// **************************

	public static <Out extends Serializable> BuildUnit<Out> create(Path dep, BuildRequest<?, Out, ?, ?> generatedBy, Stamper stamper) throws IOException {
		@SuppressWarnings("unchecked")
    BuildUnit<Out> e = PersistableEntity.create(BuildUnit.class, dep);
		e.defaultStamper = stamper;
		e.generatedBy = generatedBy;
		return e;
	}

	final public static <Out extends Serializable> BuildUnit<Out> read(Path dep, BuildRequest<?, Out, ?, ?> generatedBy) throws IOException {
	  @SuppressWarnings("unchecked")
	  BuildUnit<Out> e = PersistableEntity.read(BuildUnit.class, dep);
	  if (e != null && e.generatedBy.deepEquals(generatedBy)) {
	    e.generatedBy = generatedBy;
	    return e;
	  }
    return null;
  }
	
	/**
	 * Reads a CompilationUnit from memory or disk. The returned Compilation unit is guaranteed to be consistent.
	 * 
	 * @return null if no consistent compilation unit is available.
	 */
  public static <Out extends Serializable> BuildUnit<Out> readConsistent(Path dep, BuildRequest<?, Out, ?, ?> generatedBy, Map<? extends Path, Stamp> editedSourceFiles) throws IOException {
	  BuildUnit<Out> e = read(dep, generatedBy);
	  if (e != null && e.isConsistent(editedSourceFiles))
	    return e;
	  return null;
  }

	@Override
	protected void init() {
	  requirements = new ArrayList<>();
	  generatedFiles = new HashSet<>();
	  
	  requiredUnits = new HashSet<>();
		requiredFiles = new HashSet<>();

		state = State.INITIALIZED;
	}

	// *******************************
	// Methods for adding dependencies
	// *******************************

  public void requires(Path file) {
		requires(file, defaultStamper.stampOf(file));
	}

	public void requires(Path file, Stamp stampOfFile) {
		requirements.add(new FileRequirement(file, stampOfFile));
		requiredFiles.add(file);
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
          public Boolean visit(BuildUnit<?> mod) {
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

  public void generates(Path file) {
		generates(file, defaultStamper.stampOf(file));
	}

	public void generates(Path file, Stamp stampOfFile) {
		generatedFiles.add(new FileRequirement(file, stampOfFile));
		try {
		  if (FileCommands.exists(file)) 
		    xattr.setGenBy(file, this);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
	}
	
	public <Out_ extends Serializable> void requires(BuildUnit<Out_> mod) {
	  Objects.requireNonNull(mod);
	  requirements.add(new BuildRequirement<Out_>(mod, mod.getGeneratedBy()));
	  requiredUnits.add(mod);
	}

	/**
	 * @deprecated Probably doesn't work any longer.
	 * @param mod
	 * @see GraphUtils#repairGraph(Set)
	 */
	@Deprecated
	protected void removeModuleDependency(BuildUnit<?> mod) {
		this.requiredUnits.remove(mod);
	}


	// *********************************
	// Methods for querying dependencies
	// *********************************

	public boolean dependsOn(BuildUnit<?> other) {
		return getModuleDependencies().contains(other) ;
	}

	public boolean dependsOnTransitively(final BuildUnit<?> other) {
	  return visit(new ModuleVisitor<Boolean>() {
      @Override
      public Boolean visit(BuildUnit<?> mod) {
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

	public Set<Path> getSourceArtifacts() {
		return requiredFiles;
	}

	public Set<BuildUnit<?>> getModuleDependencies() {
		return requiredUnits;
	}

	public Set<Path> getExternalFileDependencies() {
		return requiredFiles;
	}

	public Set<Path> getGeneratedFiles() {
	  Set<Path> set = new HashSet<>();
	  for (FileRequirement freq : generatedFiles)
	    set.add(freq.path);
		return set;
	}
	
	public List<Requirement> getRequirements() {
    return requirements;
  }
	
	public BuildRequest<?, Out, ?, ?> getGeneratedBy() {
    return generatedBy;
  }
	
	public Out getBuildResult() {
    return buildResult;
  }

	public void setBuildResult(Out out) {
    this.buildResult = out;
  }
	
//	public Set<Path> getCircularFileDependencies() throws IOException {
//		Set<Path> dependencies = new HashSet<Path>();
//		Set<CompilationUnit> visited = new HashSet<>();
//		LinkedList<CompilationUnit> queue = new LinkedList<>();
//		queue.add(this);
//
//		while (!queue.isEmpty()) {
//			CompilationUnit res = queue.pop();
//			visited.add(res);
//
//			for (Path p : res.generatedFiles.keySet())
//				if (!dependencies.contains(p) && FileCommands.exists(p))
//					dependencies.add(p);
//			for (Path p : res.requiredFiles.keySet())
//				if (!dependencies.contains(p) && FileCommands.exists(p))
//					dependencies.add(p);
//
//			for (CompilationUnit nextDep : res.getModuleDependencies())
//				if (!visited.contains(nextDep) && !queue.contains(nextDep))
//					queue.addFirst(nextDep);
//		}
//
//		return dependencies;
//	}


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
		boolean hasEdits = editedSourceFiles != null;
		for (Requirement req : requirements) 
		  if (req instanceof FileRequirement) {
		    FileRequirement freq = (FileRequirement) req;
  		  Stamp editStamp = hasEdits ? editedSourceFiles.get(freq.path) : null;
  			if (editStamp != null && !editStamp.equals(freq.stamp)) {
  				return false;
  			} else if (editStamp == null && !Util.stampEqual(freq.stamp, freq.path)) {
  				return false;
  			}
		}

		return true;
	}
	
	public boolean isConsistentShallow(Map<? extends Path, Stamp> editedSourceFiles) {
	  return isConsistentShallowReason(editedSourceFiles) == InconsistenyReason.NO_REASON;
	}

	  
	public static enum InconsistenyReason implements Comparable<InconsistenyReason>{
	  NO_REASON, DEPENDENCIES_INCONSISTENT, FILES_NOT_CONSISTENT, OTHER, 
	  
	}
	
	public boolean isConsistentNonrequirements() {
	  if (hasPersistentVersionChanged())
      return false;
    
    if (!isFinished())
      return false;

    for (FileRequirement freq : generatedFiles)
      if (!freq.isConsistent())
        return false;

    if (!isConsistentExtend())
      return false;

    return true;
	}
	  
	public InconsistenyReason isConsistentShallowReason(Map<? extends Path, Stamp> editedSourceFiles) {
		if (hasPersistentVersionChanged())
			return InconsistenyReason.OTHER;
		
		if (!isFinished())
      return InconsistenyReason.OTHER;

		for (FileRequirement freq : generatedFiles)
      if (!freq.isConsistent())
    		return InconsistenyReason.FILES_NOT_CONSISTENT;

		for (Requirement req : requirements)
		  if (req instanceof FileRequirement && !((FileRequirement) req).isConsistent())
		    return InconsistenyReason.FILES_NOT_CONSISTENT;
		  else if (req instanceof BuildRequirement && !((BuildRequirement<?>) req).isConsistent())
		    return InconsistenyReason.DEPENDENCIES_INCONSISTENT;
		
		if (!isConsistentExtend())
			return InconsistenyReason.OTHER;

		return InconsistenyReason.NO_REASON;
	}

	public boolean isConsistent(final Map<? extends Path, Stamp> editedSourceFiles) {
		ModuleVisitor<Boolean> isConsistentVisitor = new ModuleVisitor<Boolean>() {
			@Override
			public Boolean visit(BuildUnit<?> mod) {
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
		public T visit(BuildUnit<?> mod);

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
	  Queue<BuildUnit<?>> queue = new ArrayDeque<>();
	  queue.add(this);
	  
	  Set<BuildUnit<?>> seenUnits = new HashSet<>();
    seenUnits.add(this);
	  
	  T result = visitor.init();
	  while(!queue.isEmpty()) {
	    BuildUnit<?> toVisit = queue.poll();
      T newResult = visitor.visit(toVisit);
      result = visitor.combine(result, newResult);
      if (visitor.cancel(result))
        break;
      
      for (BuildUnit<?> dep : toVisit.getModuleDependencies()) {
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
	protected void readEntity(ObjectInputStream in, Stamper stamper) throws IOException, ClassNotFoundException {
	  defaultStamper = stamper;
	  
	  state = (State) in.readObject();
	  requirements = (List<Requirement>) in.readObject();
	  generatedFiles = (Set<FileRequirement>) in.readObject();
	  generatedBy = (BuildRequest<?, Out, ?, ?>) in.readObject();
	  buildResult = (Out) in.readObject();
	  
	  requiredFiles = new HashSet<>();
	  requiredUnits = new HashSet<>();
	  
	  for (Requirement req : requirements)
	    if (req instanceof FileRequirement)
	      requiredFiles.add(((FileRequirement) req).path);
	    else if (req instanceof BuildRequirement)
	      requiredUnits.add(((BuildRequirement<?>) req).unit);
	}
	
	public void write() throws IOException {
    super.write(defaultStamper);
  }

	@Override
	protected void writeEntity(ObjectOutputStream out) throws IOException {
	  out.writeObject(state);
	  out.writeObject(requirements = Collections.unmodifiableList(requirements));
		out.writeObject(generatedFiles = Collections.unmodifiableSet(generatedFiles));
		out.writeObject(generatedBy);
		out.writeObject(buildResult);
	}
}
