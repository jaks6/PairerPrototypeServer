package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.Collection;

public class MastersRing extends ArrayList<String>{

	public MastersRing() {
		super();
	}
	
	
	public String getRightNeighbour(int nodePos ){
		if (size() < 2) return null;
		if (nodePos == -1) nodePos = size();
		if (size()-1 <= nodePos ) return get(0);
		else return get(nodePos+1);
	}
	
	public String getLeftNeighbour(int pos){
		if (size() < 1) return null;
		if (pos == -1) pos = size();
		if (pos != 0 ){
			return get(pos-1);
		} else {
			return getLastMaster();
		}
	}

	@Override
	public void add(int index, String element) {
		if (index == -1) super.add(element);
		else super.add(index, element);
	}


	public MastersRing(Collection<? extends String> c) {
		super(c);
	}

	public MastersRing(int initialCapacity) {
		super(initialCapacity);
	}
	
	public boolean isDeprecated(int index){
		return get(index).isEmpty();
	}
	public void deprecate(String nodeMac){
		deprecate(indexOf(nodeMac));
	}
	public void deprecate(int index){
		set(index, "");
	}
	
	public String getLastMaster(){
		return get(size()-1);
	}
	
	public void removeMaster(TableEntry node){
		remove(indexOf(node.mac));
	}
	
	/** Appends empty strings to the end of the ring until it is of the new desired size */
	public void resizeRingOfMasters(int newSize) {
		for (int i = size(); i < newSize; i++){
			add("");
		}
	}

}
