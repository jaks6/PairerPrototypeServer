package ee.ut.cs.mc.pairerprototype.server.rom;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
/*** 
 * A Table for describing the network links between the nodes in a single ring of masters network.
 * Each row corresponds to a single node.
 * @author jaks
 *
 */
public class ConnectionsTable {

	private static final int _MaxSlavesPerMaster = 2;
	String groupId;
	private ConcurrentHashMap<String, TableEntry> table = new ConcurrentHashMap<String, TableEntry>();
	int currentMaxSize = 0;
	int freeSlaveSlots = 0;

	private MastersRing mastersList = new MastersRing();

	public void addSlave(String name, TableEntry node){
		table.put(name, node);
	}
	public void addSlave(Instance node, String master){
		table.put((String) node.classValue(), new TableEntry(node));
		table.get(master).acceptFromList.add((String) node.classValue());
		table.get((String) node.classValue()).connectToList.add(master);
	}

	public void addMaster(String name, TableEntry node, int pos){
		table.put(name, node);

		String rightNeighbour = mastersList.getRightNeighbour(pos);;
		String leftNeighbour =  mastersList.getLeftNeighbour(pos);;


		if (mastersList.size() >= 1){

			//Grow the circle:
			if (rightNeighbour != null){
				//1st master must accept connections from new master, and no longer accept from previous "left" neighbour
				table.get(rightNeighbour).acceptFromList.remove(leftNeighbour);
				addConnection(rightNeighbour,name);
			}
			if (leftNeighbour != null){
				//Last master must connect to new master:
				table.get(leftNeighbour).connectToList.remove(rightNeighbour);
				addConnection(name,leftNeighbour);
			}
		}
		mastersList.add(pos, name);
	}

	public void addMaster(String name, TableEntry node){
		addMaster(name,node,-1);
	}
	public void addMaster(Instance node, int i){
		addMaster((String)node.classValue(),new TableEntry(node),-1);
	}

	public void replaceDeprecatedNode(Instance node, int i){

	}

	/** Removes the node from the table, clearing all links tied to it */
	public void removeNode(String nodeName){
		TableEntry nodeToRemove = table.get(nodeName);

		//Ensure other nodes wont listen for connections from this node
		for (String master: nodeToRemove.connectToList){
			removeConnection(master, nodeName);
		}

		//Ensure other nodes wont connect to this node
		for (String slave: nodeToRemove.acceptFromList){
			removeConnection(nodeName, slave);
		}

		table.remove(nodeName);
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
	private void removeConnection(String masterName, String slaveName){
		table.get(masterName).acceptFromList.remove(slaveName);
		table.get(slaveName).connectToList.remove(masterName);
	}


	/** Goes through all nodes in the table, checking 
	 * if they exist in the provided cluster
	 * @return */
	public int verifyNodes(Dataset cluster) {
		int mastersMissing = 0;
		Iterator<Entry<String, TableEntry>> tableIterator = table.entrySet().iterator();

		while(tableIterator.hasNext()){
			TableEntry currentNode = tableIterator.next().getValue();
			if (! cluster.classes().contains(currentNode.mac)){
				if (currentNode.isMaster()){
					mastersMissing += handleMissingMaster(cluster, currentNode);
				} else {
					table.remove(currentNode.mac);
				}
			} else {
				//node is verified, mark it down
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
		boolean masterHasNoSlaves = currentNode.acceptFromList.isEmpty();

		if (masterHasNoSlaves){
			deprecateMaster(currentNode.mac);
			return 1;
		} else {
			String replacement = pureSlaveExists(currentNode, cluster);
			if (replacement != null){
				replaceMaster(currentNode, replacement);
				return 0;
			} else {
				deprecateMaster(currentNode.mac);
				return 1;
			}
		}
	}

	public void replaceMaster(TableEntry currentNode, String replacement) {
		TableEntry replacementEntry = table.get(replacement);
		replacementEntry.connectToList = currentNode.connectToList;
		currentNode.acceptFromList.remove(replacement);
		replacementEntry.acceptFromList = currentNode.acceptFromList;

		int oldMastersRingIndex = mastersList.indexOf(currentNode.mac);
		String rightNeighbour = mastersList.getRightNeighbour(oldMastersRingIndex);
		String leftNeighbour = mastersList.getLeftNeighbour(oldMastersRingIndex);

		//change link with right neighbour
		table.get(rightNeighbour).acceptFromList.remove(currentNode.mac);
		table.get(rightNeighbour).acceptFromList.add(replacement);

		//change link with right neighbour
		table.get(leftNeighbour).connectToList.remove(currentNode.mac);
		table.get(leftNeighbour).connectToList.add(replacement);

		mastersList.set(oldMastersRingIndex, replacement);
		table.remove(currentNode.mac);
	}

	/** Determines if atleast one of the masters slaves also exists in the network, returns it if so */
	private String pureSlaveExists(TableEntry master, Dataset cluster) {
		for (String slave: master.acceptFromList){
			if (cluster.classes().contains(slave) &&  //If the slave exists in the current cluster
					! table.get(slave).isMaster()){		//and isnt already a master
				return slave;
			}
		}
		return null;
	}

	private void removeMaster(TableEntry currentNode) {
		//Remove all connections to the master
		for (String slave : currentNode.connectToList){
			removeConnection(currentNode.mac, slave);
		}

		mastersList.removeMaster(currentNode);
		table.remove(currentNode.mac);
	}

	/** Marks the given node as deprecated (needing replacement), by adding a prefix "_" to its hashmap key and 
	 * setting its TableEntrys mac-value to an empty string.
	 * @param currentNode
	 */
	private void deprecateMaster(String nodeMac) {
		mastersList.deprecate(nodeMac);

		String deprecatedMasterKey = "_"+nodeMac;
		table.put(deprecatedMasterKey, table.remove(nodeMac));
		table.get(deprecatedMasterKey).mac = "";
	}


	public int GetCurrentMasterCount(){
		return mastersList.size();
	}

	public void addMissingMasters(Dataset cluster, int desiredNoOfMasters) {
		//Check how many masters need to be still added and add them
		int mastersMissing = desiredNoOfMasters - GetCurrentMasterCount();

		for (String master : mastersList){
			if (master.isEmpty()){
				findReplacementForMaster(master);
			}
		}


	}

	private void findReplacementForMaster(String master) {
		// TODO Auto-generated method stub
	}

	public String firstMasterInRing(){
		return mastersList.get(0);
	}



	public ConcurrentHashMap<String, TableEntry> getTable(){
		return table;
	}
	public MastersRing getMastersList(){
		return mastersList;
	}

	public void addNewNodes(Dataset cluster) throws UnexpectedException {
		//See if there are masters missing.
		for (int i = 0; i <mastersList.size(); i++){
			if (mastersList.isDeprecated(i)){
				for (Instance node : cluster){
					addMaster(node, i);

				}
			}
		}

		//Add the rest as slaves
		for (Instance node: cluster){
			boolean success = false;
			for (String master : mastersList){
				if (table.get(master).acceptFromList.size() < _MaxSlavesPerMaster){
					addSlave(node, master);
					success = true;
					break;
				}
			}
			if (! success) throw new UnexpectedException(
					"DIDNT FIND MASTER FOR SLAVE NEED TO GROW");
		}
	}

	/** Removes all nodes from the cluster which already are in the network (verified)*/
	public void cleanCluster(Dataset cluster) {
		Iterator<Instance> clusterIterator = cluster.iterator();
		while ( clusterIterator.hasNext()){
			if (table.containsKey(clusterIterator.next().classValue())){
				clusterIterator.remove();
			}
		}
	}


}
