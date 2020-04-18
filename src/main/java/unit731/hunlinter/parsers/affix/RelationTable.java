package unit731.hunlinter.parsers.affix;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.RegexHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class RelationTable{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Error reading line ''{0}'': bad number of entries, it must be '<option> <substitutions>'");
	private static final MessageFormat BAD_OPTION = new MessageFormat("Error reading line ''{0}'': bad option, it must be {1}");

	//aß(ss) > a, ß, ss
	private static final Pattern PATTERN = RegexHelper.pattern("(?<!\\()(?![^\\(]*\\))");


	private final AffixOption affixOption;
	private List<String[]> table;


	public RelationTable(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	public void parse(final ParsingContext context){
		try{
			final Scanner scanner = context.getScanner();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

			table = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				ParserHelper.assertNotEOF(scanner);

				final String line = scanner.nextLine();
				final String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				final String[] substitutions = RegexHelper.split(parts[1], PATTERN);
				for(int j = 0; j < substitutions.length; j ++){
					final String substitution = substitutions[j];
					if(substitution.length() > 1)
						substitutions[j] = substitution.substring(1, substitution.length() - 1);
				}
				table.add(substitutions);
			}
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final String[] parts, final ParsingContext context){
		if(parts.length != 2)
			throw new LinterException(WRONG_FORMAT.format(new Object[]{context}));
		if(!affixOption.getCode().equals(parts[0]))
			throw new LinterException(BAD_OPTION.format(new Object[]{context, affixOption.getCode()}));
	}

	public String extractAsList(){
		return table.stream()
			.map(list -> String.join(StringUtils.SPACE, list))
			.collect(Collectors.joining(", "));
	}

	@Override
	public String toString(){
		return "[affixOption=" + affixOption + ',' + "table=" + table + ']';
	}

}
