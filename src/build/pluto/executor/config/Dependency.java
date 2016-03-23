package build.pluto.executor.config;

public class Dependency {
	public String kind;
	public Object input;
	
	@Override
	public String toString() {
		return kind + "[" + input + "]";
	}
}
