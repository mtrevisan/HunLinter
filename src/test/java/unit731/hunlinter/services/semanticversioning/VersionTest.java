package unit731.hunlinter.services.semanticversioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionTest{

	@Test
	void shouldParseNormalVersion(){
		Version version = new Version("1.0.0");

		Assertions.assertEquals(new Version(1, 0, 0), version);
		Assertions.assertEquals("1.0.0", version.toString());
	}

	@Test
	void shouldRaiseErrorIfNumericIdentifierHasLeadingZeros(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> new Version("01.1.0"));

		Assertions.assertEquals("Numeric identifier MUST NOT contain leading zeros", exception.getMessage());
	}

	@Test
	void shouldParsePreReleaseVersion(){
		Version version = new Version("1.1.0-beta");

		Assertions.assertEquals(new Version(1, 1, 0, new String[]{"beta"}), version);
		Assertions.assertEquals("1.1.0-beta", version.toString());
	}

	@Test
	void shouldParsePreReleaseVersionAndBuild(){
		Version version = new Version("1.0.0-rc.2+build.05");

		Assertions.assertEquals(new Version(1, 0, 0, new String[]{"rc", "2"}, new String[]{"build", "05"}), version);
		Assertions.assertEquals("1.0.0-rc.2+build.05", version.toString());
	}

	@Test
	void shouldParseBuild(){
		Version version = new Version("1.2.3+build");

		Assertions.assertEquals(new Version(1, 2, 3, null, new String[]{"build"}), version);
		Assertions.assertEquals("1.2.3+build", version.toString());
	}

	@Test
	void shouldRaiseErrorForIllegalInputString(){
		for(String illegal : new String[]{"", null}){
			Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Version(illegal));

			Assertions.assertEquals("Argument is not a valid version", exception.getMessage());
		}
	}

}
