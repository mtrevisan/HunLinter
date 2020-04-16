package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;


class DictionaryTest{

	@Test
	void testReadFromFile() throws IOException{
		try(
				InputStream dictInput = getClass().getResource("/services/fsa/stemming/test-infix.dict").openStream();
				InputStream infoInput = getClass().getResource("/services/fsa/stemming/test-infix.info").openStream();
			){
			Assertions.assertNotNull(Dictionary.read(dictInput, infoInput));
		}
	}

}
