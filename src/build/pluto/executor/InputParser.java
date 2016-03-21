package build.pluto.executor;

import java.io.File;
import java.io.Serializable;

import build.pluto.executor.config.yaml.YamlObject;

public interface InputParser<In extends Serializable> extends Serializable {
	public In parse(YamlObject input, String target, File workingDir) throws Throwable;
}
