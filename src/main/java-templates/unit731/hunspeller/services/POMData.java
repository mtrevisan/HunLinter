package unit731.hunspeller.services;


public final class POMData{

	private static final String VERSION = "${project.version}";


	public static String getVersion(){
		return VERSION;
	}

}
