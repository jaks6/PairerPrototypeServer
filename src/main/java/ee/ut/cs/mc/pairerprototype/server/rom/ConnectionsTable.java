package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ee.ut.cs.mc.pairerprototype.server.clustering.RecordingInstance;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
/*** 
 * A Table for describing the network links between the nodes in a single ring of masters network.
 * Each row corresponds to a single node.
 * @author jaks
 *
 */
public class ConnectionsTable {

	String groupId;
	private ConcurrentHashMap<String, TableEntry> table = new ConcurrentHashMap<String, TableEntry>();
	int currentMaxSize = 0;
	int freeSlaveSlots = 0;
	int currentMasterCount = 0;
	
	private ArrayList<String> mastersList = new ArrayList<>();
	
	public void addSlave(String name, TableEntry node){
		table.put(name, node);
	}
	
	public void addMaster(String name, TableEntry node, int pos){
		table.put(name, node);
		String leftNeighbour = null;
		String rightNeighbour = null;
		
		
		if (mastersList.size() >= 2){
			if (pos!=-1){
				rightNeighbour = mastersList.get(pos+1);
			} else {
				rightNeighbour = mastersList.get(0);
			}		
			
		}
		if (mastersList.size() >= 1){
			
			//Grow the circle:
			if (pos > 0){
				leftNeighbour = mastersList.get(pos-1);
			} else {
				leftNeighbour = mastersList.get(mastersList.size()-1);
			}
			try {
				//1st master must accept connections from new master, and no longer accept from previous "left" neighbour
				table.get(rightNeighbour).acceptFromList.remove(leftNeighbour);
				addConnection(rightNeighbour,name);
			} catch (NullPointerException e) {
				//No neighbour
			}
			try{
			//Last master must connect to new master:
			table.get(leftNeighbour).connectToList.remove(rightNeighbour);
			addConnection(name,leftNeighbour);
			} catch (NullPointerException e) {
				//No neighbour
			}
		}
		
		
		if (pos!=-1) mastersList.add(pos, name);
		else mastersList.add(name);
		currentMasterCount++;
	}
	
	public void addMaster(String name, TableEntry node){
		addMaster(name,node,-1);
	}
	
	/** Removes the node from the table, clearing all links tied to it */
	public void removeNode(String nodeName){
		TableEntry nodeToRemove = table.get(nodeName);
		
		//Ensure other nodes wont listen for connections from this node
		for (String slave: nodeToRemove.connectToList){
			removeMasterConnection(nodeName, slave);
		}
		
		//Ensure other nodes wont connect to this node
		for (String master: nodeToRemove.acceptFromList){
			removeSlaveConnection(nodeName, master);
		}
		
		table.remove(nodeToRemove);
	}
	
	private void addSlaveConnection(String slaveName, String masterName){
		table.get(masterName).acceptFromList.add(slaveName);
	}
	private void removeSlaveConnection(String slaveName, String masterName){
		table.get(masterName).acceptFromList.remove(slaveName);
	}
	private void addConnection(String masterName, String slaveName){
		table.get(slaveName).connectToList.add(masterName);
		table.get(masterName).acceptFromList.add(slaveName);
	}
	private void removeMasterConnection(String masterName, String slaveName){
		table.get(slaveName).connectToList.remove(masterName);
	}
	
	
	/** Goes through all masters in the table, checking 
	 * if they exist in the provided cluster
	 * @return */
	public int verifyMasters(Dataset cluster) {
		int mastersMissing = 0;
		Iterator<Entry<String, TableEntry>> tableIterator = table.entrySet().iterator();
		while(tableIterator.hasNext()){
			Entry<String, TableEntry> entry = tableIterator.next();
			TableEntry currentNode = entry.getValue();
			if (currentNode.isMaster()){
				if (! cluster.classes().contains(currentNode.mac)){
					mastersMissing += handleMissingMaster(cluster, currentNode);
				}
			}
		}
		return mastersMissing;
	}

	/** Tries to replace given master with its immediate slave.
	 * @param cluster
	 * @param currentNode
	 * @return Returns 0 if master has successfully been replaced with its slave, 1 otherwise.
	 */
	public int handleMissingMaster(Dataset cluster, TableEntry currentNode) {
		if (currentNode.acceptFromList.isEmpty()){
			RemoveMaster(currentNode);
			return 1;
		}
		else {
			String replacement = immediateSlaveExists(currentNode, cluster);
			if (replacement != null){
				ReplaceMaster(currentNode, replacement);
				return 0;
			} else {
				return 1;
			}
		}
	}

	public void ReplaceMaster(TableEntry currentNode, String replacement) {
		TableEntry replacementEntry = table.get(replacement);
		replacementEntry.connectToList = currentNode.connectToList;
		currentNode.acceptFromList.remove(replacement);
		replacementEntry.acceptFromList = currentNode.acceptFromList;
		
		int oldMastersRingIndex = mastersList.indexOf(currentNode.mac);
		mastersList.set(oldMastersRingIndex, replacement);
		table.remove(currentNode.mac);
	}

	/** Determines if atleast one of the masters slaves also exists in the network, returns it if so */
	private String immediateSlaveExists(TableEntry master, Dataset cluster) {
		for (String slave: master.acceptFromList){
			if (cluster.classes().contains(slave) &&  //If the slave exists in the current cluster
					! table.get(slave).isMaster()){		//and isnt already a master
				return slave;
			}
		}
		return null;
	}

	private void RemoveMaster(TableEntry currentNode) {
		//Remove all connections to the master
		//!TODO replace master with some slave, preferring its own slaves
		for (String slave : currentNode.connectToList){
			removeMasterConnection(currentNode.mac, slave);
		}
		mastersList.remove(currentNode.mac);
		table.remove(currentNode);
		currentMasterCount--;
	}
	
	public int GetCurrentMasterCount(){
		return mastersList.size();
	}

	public void addMissingMasters(Dataset cluster, int desiredNoOfMasters) {
		//Check how many masters need to be still added and add them
		int mastersMissing = desiredNoOfMasters - GetCurrentMasterCount();
		
		for (Instance node : cluster){
			if (!table.containsKey(node.classValue())){
				//the node is new in the network, add it as a master
			}
		}
		
	}
	
	public String firstMasterInRing(){
		return mastersList.get(0);
	}
	public String lastMasterInRing(){
		return mastersList.get(mastersList.size()-1);
	}
	
	
}
