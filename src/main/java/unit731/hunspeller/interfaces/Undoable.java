package unit731.hunspeller.interfaces;


public interface Undoable{

	void onUndoChange(boolean canUndo);

	void onRedoChange(boolean canRedo);

}
