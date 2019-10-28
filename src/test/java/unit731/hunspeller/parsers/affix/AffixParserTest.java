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
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"CIRCUMFIX A",
			"COMPOUNDFLAG B");

		affParser.parse(affFile, language);
	}

	@Test
	void verifyKo(){
		String language = "xxx";
		File affFile = FileHelper.getTemporaryUTF8File(language, ".aff",
			"SET UTF-8",
			"COMPLEXPREFIXES",
			"CIRCUMFIX A",
			"COMPOUNDFLAG A");

		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> affParser.parse(affFile, language));
		Assertions.assertEquals("Same flags present in multiple options", exception.getMessage());
	}

}
