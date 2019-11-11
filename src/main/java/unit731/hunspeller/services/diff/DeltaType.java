package unit731.hunspeller.services.diff;


/** Specifies the type of the delta */
public enum DeltaType{

	/** A change in the original */
	CHANGE,
	/** A delete from the original */
	DELETE,
	/** An insert into the original */
	INSERT,
	/** An do nothing */
	EQUAL

}
