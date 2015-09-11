package build.pluto.builder.bulk;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.dependency.BuildRequirement;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.IgnoreOutputStamper;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;
import build.pluto.stamp.Stamper;

public abstract class BulkBuilder<In extends Serializable, Out extends Output, SubIn extends Serializable> 
	extends Builder<In, BulkBuilder.BulkOutput<Out>> {

	public BulkBuilder(In input) {
		super(input);
	}
	
	public static class BulkOutput<Out extends Output> implements Output {
		private static final long serialVersionUID = 8175118767052712262L;
		private final Map<File, Set<File>> tracedRequired;
		private final Map<File, List<FileRequirement>> tracedProvided;
		public final Out out;
		public BulkOutput(Map<File, Set<File>> tracedRequired, Map<File, List<FileRequirement>> tracedProvided, Out out) {
			this.tracedRequired = Collections.unmodifiableMap(tracedRequired);
			this.tracedProvided = Collections.unmodifiableMap(tracedProvided);
			this.out = out;
		}
	}

	private Set<File> alreadyRequired = new HashSet<>();
	private Map<File, Set<File>> tracedRequired = new HashMap<>();
	private Map<File, List<FileRequirement>> tracedProvided = new HashMap<>();
	
	protected abstract Collection<File> requiredFiles(In input);
	protected abstract Collection<SubIn> splitInput(In input, Set<File> changedFiles);
	protected abstract BuildRequest<
		? extends SubIn,
		? extends Output, 
		? extends Builder<SubIn, ? extends Output>, 
		? extends BuilderFactory<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>>> 
		makeSubRequest(SubIn subInput);
	/**
	 * Calls `require(File, File)` to register the required files for each subinput.
	 * Calls `provide(File, File)` to register the provided files for each subinput.
	 */
	protected abstract Out buildBulk(In input, Collection<SubIn> splitInput, Set<File> changedFiles) throws Throwable;
	

	protected void require(File source, File file) {
		require(source, file, defaultStamper());
	}
	protected void require(File source, File file, Stamper stamper) {
		Set<File> files = tracedRequired.get(source);
		if (files == null) {
			files = new HashSet<>();
			tracedRequired.put(source, files);
		}
		files.add(file);
		if (alreadyRequired.add(file)) {
			Stamp stamp = stamper.stampOf(file);
		    FileRequirement freq = new FileRequirement(file, stamp);
			super.requireOther(freq);
		}
	}
	protected void require(File source, Requirement req) {
		Set<File> files = tracedRequired.get(source);
		if (files == null) {
			files = new HashSet<>();
			tracedRequired.put(source, files);
		}
		
		if (req instanceof FileRequirement) {
			File file = ((FileRequirement) req).file;
			files.add(file);
			if (alreadyRequired.add(file))
			      super.requireOther(req);
		}
		super.requireOther(req);

	}

	protected void provide(File source, File file) {
		provide(source, file, LastModifiedStamper.instance.stampOf(file));
	}
	protected void provide(File source, File file, Stamp stamp) {
		provide(source, new FileRequirement(file, stamp));
	}
	protected void provide(File source, FileRequirement req) {
		List<FileRequirement> files = tracedProvided.get(source);
		if (files == null) {
			files = new ArrayList<>();
			tracedProvided.put(source, files);
		}
		files.add(req);
		super.provide(req);
	}

	@Override
	protected BulkOutput<Out> build(In input) throws Throwable {
		Map<File, Set<File>> previousRequired = getPreviousRequired();
		Map<File, List<FileRequirement>> previousProvided = getPreviousProvided();
		
		Collection<File> requiredFiles = requiredFiles(input);
		
		Set<File> changedFiles = new HashSet<>(requiredFiles);
		if (getPreviousBuildUnit() != null)
			for (Requirement req : getPreviousBuildUnit().getRequirements())
				if (req instanceof FileRequirement) {
					FileRequirement freq = (FileRequirement) req;
					if (changedFiles.contains(freq.file) && freq.isConsistent()) {
						changedFiles.remove(freq.file);
					}
				}
		
		for (File changedFile : changedFiles)
			for (Entry<File,Set<File>> e : previousRequired.entrySet())
				if (e.getValue().contains(changedFile))
					changedFiles.add(e.getKey());

		Collection<SubIn> splitInput = splitInput(input, changedFiles);
		
		Out out = buildBulk(input, splitInput, changedFiles);

		for (File file : requiredFiles)
			if (!changedFiles.contains(file)) {
				Set<File> required = previousRequired.get(file);
				if (required != null)
					for (File require : required)
						require(file, require);
						
				List<FileRequirement> provided = previousProvided.get(file);
				if (provided != null)
					for (FileRequirement provide : provided)
						provide(file, provide);
			}
		
		for (SubIn subInput : splitInput) 
			makeShadowBuildUnit(subInput);
		
		return new BulkOutput<>(tracedRequired, tracedProvided, out);
	}

	private Map<File, List<FileRequirement>> getPreviousProvided() {
		if (getPreviousBuildUnit() != null && getPreviousBuildUnit().getBuildResult() != null)
			return getPreviousBuildUnit().getBuildResult().tracedProvided;
		return Collections.emptyMap();
	}

	private Map<File, Set<File>> getPreviousRequired() {
		if (getPreviousBuildUnit() != null && getPreviousBuildUnit().getBuildResult() != null)
			return getPreviousBuildUnit().getBuildResult().tracedRequired;
		return Collections.emptyMap();
	}

	private void makeShadowBuildUnit(SubIn subInput) throws IOException {
		BuildRequest<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>, ? extends BuilderFactory<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>>> 
			req = makeSubRequest(subInput);
		
		Builder<SubIn, ? extends Output> builder = req.createBuilder();
		File path = builder.persistentPath(subInput);
		
		BuildUnit<? extends Output> unit = BuildUnit.create(path, req);
		unit.requires(new BuildRequirement<>(
				getBuildUnit(), 
				getBuildUnit().getGeneratedBy(), 
				IgnoreOutputStamper.IGNORE_OUTPUT_STAMP));
		unit.setState(BuildUnit.State.SUCCESS);
		unit.write();
	}
}
