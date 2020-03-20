package unit731.hunlinter.services.fsa.stemming;


/**
 * Known {@link ISequenceEncoder}s.
 *
 * @see "org.carrot2.morfologik-parent, 2.1.8-SNAPSHOT, 2020-01-02"
 */
public enum EncoderType{

	SUFFIX{
		@Override
		public ISequenceEncoder get(){
			return new TrimSuffixEncoder();
		}
	},
	PREFIX{
		@Override
		public ISequenceEncoder get(){
			return new TrimPrefixAndSuffixEncoder();
		}
	},
	INFIX{
		@Override
		public ISequenceEncoder get(){
			return new TrimInfixAndSuffixEncoder();
		}
	},
	NONE{
		@Override
		public ISequenceEncoder get(){
			return new NoEncoder();
		}
	};

	public abstract ISequenceEncoder get();

}
