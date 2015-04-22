package build.pluto.test;

import build.pluto.output.Output;

public final class EmptyBuildOutput implements Output {

	/**
	 * 
	 */
	private static final long serialVersionUID = -931710373384110638L;
	
	public static final EmptyBuildOutput instance = new EmptyBuildOutput();
	
	private EmptyBuildOutput(){}

}
