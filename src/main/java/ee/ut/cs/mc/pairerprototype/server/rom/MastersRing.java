package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.json.simple.JSONArray;

public class MastersRing extends ArrayList<String>{

	private static final int _MaxSlavesPerMaster = 2;
	private HashMap<String, TreeSet<String>> slaves;
	private int age;
	private String id;
	JSONArray groupmembers;

	public MastersRing(String id) {
		super();
		this.id = id;
		this.age = 0;
		groupmembers = new JSONArray();
		this.slaves = new HashMap<String, TreeSet<String>>();
	}


	public String getRightNeighbour(int nodePos ){
		if (nodePos == -1) nodePos = size();
		if (size() < 2) return null;
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

	public void addMaster(String mac){addMaster(mac,-1);}
	public void addMaster(String mac, int index){
		add(index, mac);
		slaves.put(mac, new TreeSet<String>());
		}
	public void addMaster(Instance node){addMaster(node, -1);}
	public void addMaster(Instance node, int index){
		addMaster((String) node.classValue(), index);
	}

	public void addSlave(Instance node, String master){
		getSlaves(master).add((String) node.classValue());
	}
	public void addSlave(String mac,String master){
		getSlaves(master).add(mac);
	}

	/** Adds slave to first free master found, returns false if unsuccessful*/
	public boolean addSlave(Instance slaveNode){
		for (TreeSet<String> slaveset : slaves.values()){
			if ( slaveset.size() < _MaxSlavesPerMaster) {
				slaveset.add((String)slaveNode.classValue());
				return true;
			}
		}
		return false;
	}

	@Override
	public void add(int index, String element) {
		if (index == -1 || size()<= index) super.add(element);
		else super.set(index, element);
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
	
	public void resetAge(){
		this.age = 0;
	}

	public String getLastMaster(){
		return get(size()-1);
	}

	public void removeMaster(TableEntry node){
		remove(indexOf(node.mac));
	}


	public int verifyNodes(Dataset cluster) {
		int mastersMissing = 0;
		
		//Verify slaves
		for (TreeSet<String> slaveSet: slaves.values()){
			Iterator<String >slaveIterator = slaveSet.iterator();
			while(slaveIterator.hasNext()){
				String slave = slaveIterator.next();
				if (! cluster.classes().contains(slave)){
					slaveIterator.remove();
			}
				//node is verified
			}
		}

		//Verify masters
		Iterator<String> mastersIterator = this.iterator();
		while (mastersIterator.hasNext()){
			String master = mastersIterator.next();
			if (! cluster.classes().contains(master)){
				mastersMissing += handleMissingMaster(master, mastersIterator);
			} else {
				//node is verified
			}
		}
		return 0;
	}


	private void removeFromCluster(Dataset cluster, String master) {
		int index = cluster.classIndex(master);
		cluster.remove(index);
	}

	/** Tries to replace given master with its immediate slave.
	 * @param cluster
	 * @param currentNode
	 * @return Returns 0 if master has successfully been replaced with its slave, 1 otherwise.
	 */
	private int handleMissingMaster(String master, Iterator<String> it) {
		TreeSet<String> slavesOfMaster = getSlaves(master);
		if (slavesOfMaster.isEmpty()){
			it.remove();
			slaves.remove(master);

		} else {
			String replacement = slavesOfMaster.pollFirst();
			set(indexOf(master), replacement);
			slaves.put(replacement, slavesOfMaster);
			slaves.remove(master);

			return 0;
		}
		return 1;
	}

	private void deprecateMaster(String master) {
		// TODO Auto-generated method stub

	}


	public void resizeRingOfMasters() {

	}


	public void addNewNodes(Dataset cluster) {
		boolean nodeUsed = false;
		for (String master : this){
			if (master.isEmpty()){
				for (Instance node: cluster){
					master = (String) node.classValue();
					cluster.remove(node);
					nodeUsed = true;
					break;
				}
			}
		}
			if (!nodeUsed){
				//Add the rest as slaves
				while (!cluster.isEmpty()){
					nodeUsed = false;
					Instance node = cluster.remove(0);
					
					for (TreeSet<String> slaveSet : slaves.values()){
						if (slaveSet.size() < _MaxSlavesPerMaster){
							slaveSet.add((String) node.classValue());
							nodeUsed = true;
							break;
						}
					}
					if (!nodeUsed){
						addMaster(node);
					}
				}
			}

	}


	public void addMissingMasters(Dataset cluster) {
		// TODO Auto-generated method stub
	}


	public void clearDeprecations() {
		// TODO Auto-generated method stub
	}


	public TreeSet<String> getSlaves(String master) {
		return slaves.get(master);
	}
	public HashMap<String, TreeSet<String>> getSlaves() {
		return slaves;
	}


	public void setSlaves(HashMap<String, TreeSet<String>> slaves) {
		this.slaves = slaves;
	}
	
	public ArrayList<String> getAllSlaves(){
		ArrayList<String> allSlaves = new ArrayList<>();
		for (TreeSet<String> slaveSet : slaves.values()){
			allSlaves.addAll(slaveSet);
		}
		return allSlaves;
	}


	public String getLeftNeighbour(String master) {
		return getLeftNeighbour(indexOf(master));
	}


	public String getRightNeighbour(String master) {
		return getRightNeighbour(indexOf(master));
	}

	public String getId() {
		return id;
	}

	public int getAge() {
		return age;
	}
	public void ageByOne() {
		this.age++;
	}


	public void setGroupMembers(JSONArray nicks) {
			this.groupmembers = nicks;
	}


}
