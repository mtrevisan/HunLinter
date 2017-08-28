package unit731.hunspeller.services;


/**
 * @see <a href="http://blog.soebes.de/blog/2014/01/02/version-information-into-your-appas-with-maven/">Version Informations Into Your Apps With Maven</a>
 */
public final class POMData{

	private static final String GROUP_ID = "${project.groupId}";
	private static final String ARTIFACT_ID = "${project.artifactId}";
	private static final String VERSION = "${project.version}";
	private static final String BUILD_TIMESTAMP = "${build.timestamp}";


	public static String getGroupID(){
		return GROUP_ID;
	}

	public static String getArtifactID(){
		return ARTIFACT_ID;
	}

	public static String getVersion(){
		return VERSION;
	}

	public static String getBuildTimestamp(){
		return BUILD_TIMESTAMP;
	}

}
