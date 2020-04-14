package unit731.hunlinter.services.fsa.stemming;


/**
 * Known {@link SequenceEncoderInterface}s.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public enum EncoderType{

	SUFFIX{
		@Override
		public SequenceEncoderInterface get(){
			return new TrimSuffixEncoder();
		}
	},
	PREFIX{
		@Override
		public SequenceEncoderInterface get(){
			return new TrimPrefixAndSuffixEncoder();
		}
	},
	INFIX{
		@Override
		public SequenceEncoderInterface get(){
			return new TrimInfixAndSuffixEncoder();
		}
	},
	NONE{
		@Override
		public SequenceEncoderInterface get(){
			return new NoEncoder();
		}
	};

	public abstract SequenceEncoderInterface get();

}
