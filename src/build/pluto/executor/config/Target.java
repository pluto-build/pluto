package build.pluto.executor.config;


public class Target {
	private String name;
	private String builder;
	private Object input;
	
	@Override
	public String toString() {
		return builder + "[" + input +"]";
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBuilder() {
		return builder;
	}

	public void setBuilder(String builder) {
		this.builder = builder;
	}

	public Object getInput() {
		return input;
	}

	public void setInput(Object input) {
		this.input = input;
	}
}
