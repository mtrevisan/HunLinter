package unit731.hunlinter.services.fsa.tools;

import unit731.hunlinter.services.fsa.builders.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSA5Serializer;
import unit731.hunlinter.services.fsa.builders.FSASerializer;


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
