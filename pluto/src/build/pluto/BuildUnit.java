package build.pluto;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import build.pluto.builder.BuildRequest;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.MetaBuildRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;

/**
 * Dependency management for modules.
 * 
 * @author Sebastian Erdweg
 */
public final class BuildUnit<Out extends Output> extends PersistableEntity {

  public static final long serialVersionUID = -2821414386853890682L;

  public static enum State {
	  NEW, INITIALIZED, IN_PROGESS, SUCCESS, FAILURE;
	  
	  public static State finished(boolean success) {
	    return success ? SUCCESS : FAILURE;
	  }
	}

	public BuildUnit() { /* for deserialization only */ }
	
	private State state = State.NEW;

	protected List<Requirement> requirements;
	protected Set<FileRequirement> generatedFiles;

	protected Out buildResult;

	private transient Set<BuildUnit<?>> requiredUnits;
	private transient Set<File> requiredFiles;
	
	protected BuildRequest<?, Out, ?, ?> generatedBy;
	
	// **************************
	// Methods for initialization
	// **************************

	public static <Out extends Output> BuildUnit<Out> create(File dep, BuildRequest<?, Out, ?, ?> generatedBy) throws IOException {
		@SuppressWarnings("unchecked")
    BuildUnit<Out> e = PersistableEntity.create(BuildUnit.class, dep);
		e.generatedBy = generatedBy;
		return e;
	}
	
	final public static <Out extends Output> BuildUnit<Out> read(File dep) throws IOException {
	  @SuppressWarnings("unchecked")
    BuildUnit<Out> e = PersistableEntity.read(BuildUnit.class, dep);
	  return e;
	}

	@Override
	protected void init() {
	  super.init();
	  requirements = new ArrayList<>();
	  generatedFiles = new HashSet<>();
	  
	  requiredUnits = new HashSet<>();
		requiredFiles = new HashSet<>();

		state = State.INITIALIZED;
		generatedBy = null;
		buildResult = null;
	}

	// *******************************
	// Methods for adding dependencies
	// *******************************

	public void requires(File file, Stamp stampOfFile) {
		requirements.add(new FileRequirement(file, stampOfFile));
		requiredFiles.add(file);
	}
	
	public void generates(File file, Stamp stampOfFile) {
		generatedFiles.add(new FileRequirement(file, stampOfFile));
	}
	
	public <Out_ extends Output> void requires(BuildRequirement<Out_> req) {
	  Objects.requireNonNull(req);
	  requirements.add(req);
	  requiredUnits.add(req.getUnit());
	}
	
	public <Out_ extends Output> void requireMeta(BuildUnit<Out_> mod) {
	  Objects.requireNonNull(mod);
	  requirements.add(new MetaBuildRequirement<Out_>(mod, mod.getGeneratedBy()));
    requiredUnits.add(mod);
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

	public Set<File> getSourceArtifacts() {
		return requiredFiles;
	}

	private Set<BuildUnit<?>> getModuleDependencies() {
	  if (requiredUnits == null) {
	    requiredUnits = new HashSet<>();
	    for (Requirement req : requirements)
	      if (req instanceof BuildRequirement)
	        if (((BuildRequirement<?>) req).getUnit() != null) {
	        requiredUnits.add(((BuildRequirement<?>) req).getUnit());
	        }
	  }
		return requiredUnits;
	}
	
	public Set<BuildUnit<?>> getTransitiveModuleDependencies() {
    final Set<BuildUnit<?>> transitiveUnits = new HashSet<>();
    visit(new ModuleVisitor<Void>() {
      @Override
      public Void visit(BuildUnit<?> mod) {
        transitiveUnits.add(mod);
        return null;
      }

      @Override
      public Void combine(Void t1, Void t2) {
        return null;
      }

      @Override
      public Void init() {
        return null;
      }

      @Override
      public boolean cancel(Void t) {
        return false;
      }
    });
    return transitiveUnits;
  }
  
	public Set<File> getExternalFileDependencies() {
	  if (requiredFiles == null) {
      requiredFiles = new HashSet<>();
      for (Requirement req : requirements)
        if (req instanceof FileRequirement)
          requiredFiles.add(((FileRequirement) req).file);
    }
	  return requiredFiles;
	}

	public Set<File> getGeneratedFiles() {
	  Set<File> set = new HashSet<>();
	  for (FileRequirement freq : generatedFiles)
	    set.add(freq.file);
		return set;
	}
	
	public Set<FileRequirement> getGeneratedFileRequirements() {
	  return Collections.unmodifiableSet(generatedFiles);
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

	// ********************************************
	// Methods for checking compilation consistency
	// ********************************************

	public State getState() {
	  return state;
	}

	public File getPersistentPath() {
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

  public boolean isConsistentShallow() {
    return isConsistentShallowReason() == InconsistenyReason.NO_REASON;
	}

	public static enum InconsistenyReason implements Comparable<InconsistenyReason>{
    NO_REASON, DEPENDENCIES_INCONSISTENT, FILES_NOT_CONSISTENT, PERSISTENT_VERSION_CHANGED, NOT_FINISHED,
	  
	}

  public InconsistenyReason isConsistentNonrequirementsReason() {
    if (hasPersistentVersionChanged())
      return InconsistenyReason.PERSISTENT_VERSION_CHANGED;

    if (!isFinished())
      return InconsistenyReason.NOT_FINISHED;

    for (FileRequirement freq : generatedFiles)
      if (!freq.isConsistent())
        return InconsistenyReason.FILES_NOT_CONSISTENT;

    return InconsistenyReason.NO_REASON;
  }
	  
  public InconsistenyReason isConsistentShallowReason() {
		if (hasPersistentVersionChanged()) {
      return InconsistenyReason.PERSISTENT_VERSION_CHANGED;
		}
		
    if (!isFinished()) {
      return InconsistenyReason.NOT_FINISHED;
    }
		
		for (FileRequirement freq : generatedFiles)
      if (!freq.isConsistent()) {
        return InconsistenyReason.FILES_NOT_CONSISTENT;
      }

		for (Requirement req : requirements)
		  if (req instanceof FileRequirement && !((FileRequirement) req).isConsistent()) {
		    return InconsistenyReason.FILES_NOT_CONSISTENT;
		  }else if (req instanceof BuildRequirement && !((BuildRequirement<?>) req).isConsistent()) {
		    return InconsistenyReason.DEPENDENCIES_INCONSISTENT;
		  }
		
		return InconsistenyReason.NO_REASON;
	}

  public boolean isConsistent() {
		ModuleVisitor<Boolean> isConsistentVisitor = new ModuleVisitor<Boolean>() {
			@Override
			public Boolean visit(BuildUnit<?> mod) {
        return mod.isConsistentShallow();
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
	  return visit(visitor, null);
	}
	public <T> T visit(ModuleVisitor<T> visitor, Set<BuildUnit<?>> init) {
	  Queue<BuildUnit<?>> queue = new ArrayDeque<>();
	  if (init == null) {
	    queue.add(this);
	  }
	  else
	    queue.addAll(init);
	  
	  Set<BuildUnit<?>> seenUnits = new HashSet<>();
	  seenUnits.addAll(queue);
	  
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
	protected void readEntity(ObjectInputStream in) throws IOException, ClassNotFoundException {
	  state = (State) in.readObject();
	  requirements = (List<Requirement>) in.readObject();
	  generatedFiles = (Set<FileRequirement>) in.readObject();
	  generatedBy = (BuildRequest<?, Out, ?, ?>) in.readObject();
	  buildResult = (Out) in.readObject();
	}
	
	public void write() throws IOException {
    super.write(LastModifiedStamper.instance);
  }

	@Override
	protected void writeEntity(ObjectOutputStream out) throws IOException {
	  out.writeObject(state);
	  out.writeObject(requirements = Collections.unmodifiableList(requirements));
		out.writeObject(generatedFiles = Collections.unmodifiableSet(generatedFiles));
		out.writeObject(generatedBy);
		out.writeObject(buildResult);
	}
	
	@Override
	public String toString() {
	  return "BuildUnit(" + generatedBy.factory + ": " + persistentPath + ")";
	}
}
