package unit731.hunlinter.services.fsa.builders;

import morfologik.fsa.builders.CFSA2Serializer;
import morfologik.fsa.builders.FSA5Serializer;


/** The serialization and encoding format to use for compressing the automaton. */
public enum SerializationFormat{
	FSA5{
		@Override
		FSABinarySerializer getSerializer(){
			return new FSA5Serializer();
		}
	},

	CFSA2{
		@Override
		CFSA2Serializer getSerializer(){
			return new CFSA2Serializer();
		}
	};


	abstract FSABinarySerializer getSerializer();

}
