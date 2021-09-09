/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.semanticversioning;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.StringTokenizer;


/**
 * @see <a href="https://semver.org/">Semantic Versioning 2.0.0</a>
 */
public class Version implements Comparable<Version>{

	private static final String DOT = ".";
	/** A separator that separates the pre-release version from the normal version. */
	private static final String PRE_RELEASE_PREFIX = "-";
	/** A separator that separates the build metadata from the normal version or the pre-release version. */
	private static final String BUILD_PREFIX = "+";

	private static final String[] EMPTY_ARRAY = new String[0];


	private final Integer major;
	private final Integer minor;
	private final Integer patch;
	private final String[] preRelease;
	private final String[] build;


	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	Version(final int major, final int minor, final int patch){
		this(major, minor, patch, EMPTY_ARRAY, EMPTY_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @param preRelease	The pre-release identifiers
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	Version(final int major, final int minor, final int patch, final String[] preRelease){
		this(major, minor, patch, preRelease, EMPTY_ARRAY);
	}

	/**
	 * Constructs a {@code Version} with the major, minor and patch version numbers.
	 *
	 * @param major	The major version number
	 * @param minor	The minor version number
	 * @param patch	The patch version number
	 * @param preRelease	The pre-release identifiers
	 * @param build	The build identifiers
	 * @throws IllegalArgumentException	If one of the version numbers is a negative integer
	 */
	Version(final int major, final int minor, final int patch, final String[] preRelease, final String[] build){
		if(major < 0 || minor < 0 || patch < 0)
			throw new IllegalArgumentException("Major, minor and patch versions MUST be non-negative integers.");

		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preRelease = preRelease;
		this.build = build;
	}

	/**
	 * Creates a new instance of {@code Version} as a result of parsing the specified version string.
	 *
	 * @param version	The string representation of the version.
	 */
	public Version(String version){
		if(StringUtils.isBlank(version))
			throw new IllegalArgumentException("Argument is not a valid version");

		version = version.trim();
		if(!startsWithNumber(version))
			throw new IllegalArgumentException("Argument is not a valid version");

		final String[] tokens = StringUtils.split(version, DOT, 3);
		final String patchOnly = StringUtils.split(version, DOT + PRE_RELEASE_PREFIX + BUILD_PREFIX)[2];
		if(hasLeadingZeros(tokens[0]) || hasLeadingZeros(tokens[1]) || hasLeadingZeros(patchOnly))
			throw new IllegalArgumentException("Numeric identifier MUST NOT contain leading zeros");

		major = Integer.valueOf(tokens[0]);
		minor = Integer.valueOf(tokens[1]);
		final String patchAndOthers = tokens[2];
		final StringTokenizer tokenizer = new StringTokenizer(patchAndOthers, PRE_RELEASE_PREFIX + BUILD_PREFIX, true);
		patch = Integer.valueOf(tokenizer.nextToken());
		String nextToken = (tokenizer.hasMoreElements()? tokenizer.nextToken(): null);
		if(PRE_RELEASE_PREFIX.equals(nextToken) && tokenizer.hasMoreElements()){
			preRelease = StringUtils.split(tokenizer.nextToken(), DOT);

			nextToken = (tokenizer.hasMoreElements()? tokenizer.nextToken(): null);

			for(final String pr : preRelease){
				final boolean numeric = StringUtils.isNumeric(pr);
				if(numeric && pr.length() > 1 && pr.charAt(0) == '0')
					throw new IllegalArgumentException("Numeric identifier MUST NOT contain leading zeros");
				if(!numeric && !StringUtils.containsOnly(pr, "-ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"))
					throw new IllegalArgumentException("Argument is not a valid version");
			}
		}
		else
			preRelease = EMPTY_ARRAY;
		if(BUILD_PREFIX.equals(nextToken) && tokenizer.hasMoreElements()){
			build = StringUtils.split(tokenizer.nextToken(), DOT);

			for(final String b : build)
				if(!StringUtils.isNumeric(b) && !StringUtils.containsOnly(b, "-ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"))
					throw new IllegalArgumentException("Argument is not a valid version");
		}
		else
			build = EMPTY_ARRAY;
		if(tokenizer.hasMoreElements())
			throw new IllegalArgumentException("Argument is not a valid version");
	}

	private static boolean hasLeadingZeros(final CharSequence token){
		return (token.length() > 1 && token.charAt(0) == '0');
	}

	private static boolean startsWithNumber(final CharSequence str){
		return (!str.isEmpty() && Character.isDigit(str.charAt(0)));
	}

	/**
	 * Checks if this version is greater than the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is greater than the other version
	 * @see #compareTo(Version other)
	 */
	public final boolean greaterThan(final Version other){
		return (compareTo(other) > 0);
	}

	/**
	 * Checks if this version is greater than or equal to the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is greater than or equal to the other version
	 * @see #compareTo(Version other)
	 */
	public final boolean greaterThanOrEqualTo(final Version other){
		return (compareTo(other) >= 0);
	}

	/**
	 * Checks if this version is less than the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is less than the other version
	 * @see #compareTo(Version other)
	 */
	public final boolean lessThan(final Version other){
		return (compareTo(other) < 0);
	}

	/**
	 * Checks if this version is less than or equal to the other version.
	 *
	 * @param other	The other version to compare to
	 * @return	{@code true} if this version is less than or equal to the other version
	 * @see #compareTo(Version other)
	 */
	public final boolean lessThanOrEqualTo(final Version other){
		return (compareTo(other) <= 0);
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Version rhs = (Version)obj;
		return (major.equals(rhs.major)
			&& minor.equals(rhs.minor)
			&& patch.equals(rhs.patch)
			&& Arrays.equals(preRelease, rhs.preRelease)
			&& Arrays.equals(build, rhs.build));
	}

	@Override
	public final int hashCode(){
		int result = (major != null? Integer.hashCode(major): 0);
		if(minor != null)
			result = 31 * result + Integer.hashCode(minor);
		if(patch != null)
			result = 31 * result + patch.hashCode();
		result = 31 * result + Arrays.hashCode(preRelease);
		result = 31 * result + Arrays.hashCode(build);
		return result;
	}

	/**
	 * Compares two {@code Version} instances.
	 * <p>
	 * This method doesn't take into account the versions' build metadata. If you want to compare the versions' build metadata
	 * use the {@code Version.compareToWithBuilds} method.</p>
	 *
	 * @param other	The object to be compared.
	 * @return	A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 * @see #compareToWithBuilds(Version other)
	 */
	@Override
	public final int compareTo(final Version other){
		final int result = compareToCore(other);
		return (result != 0? result: compareToIdentifiers(preRelease, other.preRelease));
	}

	/**
	 * Compares two {@code Version} instances taking into account their build metadata.
	 * <p>
	 * When compared build metadata is divided into identifiers. The numeric identifiers are compared numerically, and the alphanumeric
	 * identifiers are compared in the ASCII sort order.</p>
	 * <p>
	 * If one of the compared versions has no defined build metadata, this version is considered to have a lower
	 * precedence than that of the other.</p>
	 *
	 * @param other	The object to be compared.
	 * @return	A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 */
	public final int compareToWithBuilds(final Version other){
		int result = compareTo(other);
		if(result == 0)
			result = compareToIdentifiers(preRelease, other.preRelease);
		return (result != 0? result: compareToIdentifiers(build, other.build));
	}

	private int compareToCore(final Version other){
		int comparison = compareToCore(major, other.major);
		if(comparison != 0)
			return comparison;

		comparison = compareToCore(minor, other.minor);
		if(comparison != 0)
			return comparison;

		return compareToCore(patch, other.patch);
	}

	private static int compareToCore(final Integer value1, final Integer value2){
		int comparison = 0;
		if(value1 != null || value2 != null){
			if(value1 != null && value2 == null)
				return 1;
			if(value1 == null)
				return -1;
			comparison = Integer.compare(value1, value2);
		}
		return comparison;
	}

	private static int compareToIdentifiers(final String[] preRelease, final String[] otherPreRelease){
		final int result = compareIdentifierArrays(preRelease, otherPreRelease);
		//a larger set of pre-release fields has a higher precedence than a smaller set, if all the preceding identifiers are equal
		return (result != 0? result: preRelease.length - otherPreRelease.length);
	}

	private static int compareIdentifierArrays(final String[] preRelease, final String[] otherPreRelease){
		int result = (otherPreRelease.length - preRelease.length);
		for(int i = 0; i < getLeastCommonArrayLength(preRelease, otherPreRelease); i ++){
			result = compareIdentifiers(preRelease[i], otherPreRelease[i]);
			if(result != 0)
				break;
		}
		return result;
	}

	private static int getLeastCommonArrayLength(final String[] array1, final String[] array2){
		return Math.min(array1.length, array2.length);
	}

	private static int compareIdentifiers(final String identifier1, final String identifier2){
		return (StringUtils.isNumeric(identifier1) && StringUtils.isNumeric(identifier2)
			? Integer.parseInt(identifier1) - Integer.parseInt(identifier2)
			: identifier1.compareTo(identifier2));
	}

	@Override
	public final String toString(){
		final StringBuilder sb = (new StringBuilder())
			.append(major).append(DOT).append(minor).append(DOT).append(patch);
		if(preRelease.length > 0)
			sb.append(PRE_RELEASE_PREFIX).append(String.join(DOT, preRelease));
		if(build.length > 0)
			sb.append(BUILD_PREFIX).append(String.join(DOT, build));
		return sb.toString();
	}

}
