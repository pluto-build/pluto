package build.pluto.executor.loaddep;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import build.pluto.dependency.Origin;
import build.pluto.executor.config.yaml.YamlObject;
import build.pluto.output.Output;

public class FileLoadDependency implements LoadDependency {

	public static class Factory implements LoadDependencyFactory {
		@Override
		public String kind() { return "file"; }

		@Override
		public LoadDependency create(File workingDir, YamlObject input) {
			File file = new File(input.asString());
			if (!file.isAbsolute())
				file = new File(workingDir, file.getPath());
			return new FileLoadDependency(file);
		}
	}
	
	private final File file;
	
	public FileLoadDependency(File file) {
		this.file = file;
	}
	
	@Override
	public List<File> loadSimple() {
		return Collections.singletonList(file);
	}

	@Override
	public Origin loadComplex() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<File> filesFromOutputs(Collection<? extends Output> outputs) {
		throw new UnsupportedOperationException();
	}

}
