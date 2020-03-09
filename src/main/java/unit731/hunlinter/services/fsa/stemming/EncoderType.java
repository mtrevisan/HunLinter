package unit731.hunlinter.services.fsa.stemming;


/** Known {@link ISequenceEncoder}s. */
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
