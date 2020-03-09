package unit731.hunlinter.services.fsa.builders;


/** The serialization and encoding format to use for compressing the automaton. */
public enum SerializationFormat{
	FSA5{
		@Override
		public FSASerializer getSerializer(){
			return new FSA5Serializer();
		}
	},

	CFSA2{
		@Override
		public CFSA2Serializer getSerializer(){
			return new CFSA2Serializer();
		}
	};


	public abstract FSASerializer getSerializer();

}
