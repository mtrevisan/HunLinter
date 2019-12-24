package unit731.hunspeller.parsers.thesaurus;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.services.StringHelper;


public class ThesaurusEntry implements Comparable<ThesaurusEntry>{

	public static final String PIPE = "|";
	public static final String POS_AND_MEANS = PIPE + ":";
	public static final String MEANS = PIPE + ",";


	private final String synonym;
	private final List<MeaningEntry> meanings;


	public ThesaurusEntry(final String synonym, final List<MeaningEntry> meanings){
		Objects.requireNonNull(synonym);
		Objects.requireNonNull(meanings);

		this.synonym = synonym;
		this.meanings = meanings;
	}

	public ThesaurusEntry(final String line, final LineNumberReader br) throws IOException{
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		final String[] data = StringUtils.split(line, POS_AND_MEANS);

		synonym = data[0];
		final int numEntries = Integer.parseInt(data[1]);
		meanings = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			final String meaning = br.readLine();
			if(meaning == null)
				throw new EOFException("Unexpected EOF while reading Thesaurus file");

			meanings.add(new MeaningEntry(meaning));
		}
	}

	public String getSynonym(){
		return synonym;
	}

	public String joinMeanings(final String separator){
		return StringHelper.join(separator, meanings);
	}

	public void setMeanings(final String[] lines){
		meanings.clear();
		Arrays.stream(lines)
			.map(MeaningEntry::new)
			.forEachOrdered(meanings::add);
	}

	public void addMeaning(MeaningEntry meaningEntry){
		meanings.add(meaningEntry);
	}

	public int getMeaningsEntries(){
		return meanings.size();
	}

	public boolean hasSamePartOfSpeech(final String[] partOfSpeeches){
		return meanings.stream()
			.map(MeaningEntry::getPartOfSpeeches)
			.anyMatch(pos -> Arrays.equals(pos, partOfSpeeches));
	}

	public void saveToIndex(BufferedWriter writer, int idx) throws IOException{
		writer.write(synonym);
		writer.write(ThesaurusEntry.PIPE);
		writer.write(Integer.toString(idx));
		writer.write(StringUtils.LF);
	}

	public int saveToData(BufferedWriter dataWriter, Charset charset) throws IOException{
		final int meaningsEntries = getMeaningsEntries();
		saveToIndex(dataWriter, meaningsEntries);
		int meaningsLength = 1;
		for(final MeaningEntry meaning : meanings){
			final String m = meaning.toString();
			dataWriter.write(m);
			dataWriter.write(StringUtils.LF);

			meaningsLength += m.getBytes(charset).length;
		}
		return meaningsLength + StringUtils.LF.length() * meaningsEntries;
	}

	public boolean contains(final List<String> partOfSpeeches, final List<String> meanings){
		final List<String> mm = new ArrayList<>(meanings);
		return (mm.remove(synonym) && meanings.contains(synonym) && this.meanings.stream().anyMatch(meaning -> meaning.containsAllMeanings(partOfSpeeches, mm)));
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(": ");
		sj.add(synonym);
		meanings.forEach(meaning -> sj.add(StringHelper.join(", ", meaning)));
		return sj.toString();
	}

	@Override
	public int compareTo(final ThesaurusEntry other){
		return new CompareToBuilder()
			.append(synonym, other.synonym)
			.append(meanings, other.meanings)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final ThesaurusEntry rhs = (ThesaurusEntry)obj;
		return new EqualsBuilder()
			.append(synonym, rhs.synonym)
			.append(meanings, rhs.meanings)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(synonym)
			.append(meanings)
			.toHashCode();
	}

}
