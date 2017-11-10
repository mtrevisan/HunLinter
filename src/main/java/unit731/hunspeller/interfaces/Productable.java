package unit731.hunspeller.interfaces;


public interface Productable{

	String getWord();
	String[] getRuleFlags();
	String[] getDataFields();

	boolean containsRuleFlag(String ruleFlag);
	boolean containsDataField(String dataField);

}
