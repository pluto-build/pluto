package build.pluto;

import build.pluto.buildmaven.input.ArtifactConstraint;

public class MavenDependencies {
    public static final ArtifactConstraint SUGARJ_COMMON = 
        new ArtifactConstraint(
            "org.sugarj",
            "common",
            "1.6.0-SNAPSHOT",
            null,
            null);
    
    public static final ArtifactConstraint JAVA_UTIL =
        new ArtifactConstraint(
            "com.cedarsoftware",
            "java-util-pluto-bugfix",
            "1.19.4-SNAPSHOT",
            null,
            null);
    
    public static final ArtifactConstraint COMMONS_CLI =
        new ArtifactConstraint(
            "commons-cli",
            "commons-cli",
            "1.3.1",
            null,
            null);

    public static final ArtifactConstraint JUNIT =
        new ArtifactConstraint(
            "junit",
            "junit",
            "4.12",
            null,
            null);
}
