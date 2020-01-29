package unit731.hunlinter.services.semanticversioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionCompareTest{

	@Test
	void shouldBeReflexive(){
		Version v1 = new Version("2.3.7");

		Assertions.assertEquals(v1, v1);
	}

	@Test
	void shouldBeSymmetric(){
		Version v1 = new Version("2.3.7");
		Version v2 = new Version("2.3.7");

		Assertions.assertEquals(v1, v2);
		Assertions.assertEquals(v2, v1);
	}

	@Test
	void shouldBeTransitive(){
		Version v1 = new Version("2.3.7");
		Version v2 = new Version("2.3.7");
		Version v3 = new Version("2.3.7");

		Assertions.assertEquals(v1, v2);
		Assertions.assertEquals(v2, v3);
		Assertions.assertEquals(v1, v3);
	}

	@Test
	void shouldBeConsistent(){
		Version v1 = new Version("2.3.7");
		Version v2 = new Version("2.3.7");

		Assertions.assertEquals(v1, v2);
		Assertions.assertEquals(v1, v2);
		Assertions.assertEquals(v1, v2);
	}

	@Test
	void shouldReturnFalseIfOtherVersionIsNull(){
		Version v1 = new Version("2.3.7");
		Version v2 = null;

		Assertions.assertNotEquals(v1, v2);
	}

	@Test
	void shouldIgnoreBuildMetadataWhenCheckingForEquality(){
		Version v1 = new Version("2.3.7-beta+build");
		Version v2 = new Version("2.3.7-beta");

		Assertions.assertEquals(v1, v2);
	}

	@Test
	void preReleaseShouldHaveLowerPrecedenceThanAssociatedNormal(){
		Version v1 = new Version("1.3.7");
		Version v2 = new Version("1.3.7-alpha");

		Assertions.assertTrue(v1.compareTo(v2) > 0);
		Assertions.assertTrue(v2.compareTo(v1) < 0);
	}

	@Test
	void preRelease1(){
		Version v1 = new Version("2.3.7-alpha");
		Version v2 = new Version("2.3.7-beta");

		Assertions.assertTrue(v1.lessThan(v2));
	}

	@Test
	void preRelease2(){
		Version v1 = new Version("2.3.7-beta.1");
		Version v2 = new Version("2.3.7-beta.2");

		Assertions.assertTrue(v1.lessThan(v2));
	}

}
