package unit731.hunspeller.parsers.affix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.services.FileHelper;

import java.io.File;
import java.io.IOException;


class AffixParserTest{

	private final AffixParser affParser = new AffixParser();


	@Test
	void verifyOk() throws IOException{
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"CIRCUMFIX A",
			"COMPOUNDFLAG B");

		affParser.parse(affFile);
	}

	@Test
	void verifyKo(){
		File affFile = FileHelper.getTemporaryUTF8File("xxx", ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"CIRCUMFIX A",
			"COMPOUNDFLAG A");

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> affParser.parse(affFile));
		Assertions.assertEquals("Repeated flags in multiple tags", exception.getMessage());
	}

}
