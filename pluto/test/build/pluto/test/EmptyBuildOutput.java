package build.pluto.test;

import java.io.Serializable;

public final class EmptyBuildOutput implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -931710373384110638L;
	
	public static final EmptyBuildOutput instance = new EmptyBuildOutput();
	
	private EmptyBuildOutput(){}

}
