package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.HashMap;
/*** 
 * A Table for describing the network links between the nodes in a single ring of masters network.
 * Each row corresponds to a single node.
 * @author jaks
 *
 */
public class ConnectionsTable {

	String groupId;
	private HashMap<String, TableEntry> table = new HashMap<String, TableEntry>();
	
	public void addNode(String name, TableEntry node){
		table.put(name, node);
	}
	
	/** Removes the node from the table, clearing all links tied to it */
	public void removeNode(String nodeName){
		TableEntry nodeToRemove = table.get(nodeName);
		
		//Ensure other nodes wont listen for connections from this node
		for (String slave: nodeToRemove.connectToList){
			removeMaster(nodeName, slave);
		}
		
		//Ensure other nodes wont connect to this node
		for (String master: nodeToRemove.acceptFromList){
			removeSlave(nodeName, master);
		}
		
		table.remove(nodeToRemove);
	}
	
	private void addSlave(String slaveName, String masterName){
		table.get(masterName).acceptFromList.add(slaveName);
	}
	private void removeSlave(String slaveName, String masterName){
		table.get(masterName).acceptFromList.remove(slaveName);
	}
	private void addMaster(String masterName, String slaveName){
		table.get(slaveName).connectToList.add(masterName);
	}
	private void removeMaster(String masterName, String slaveName){
		table.get(slaveName).connectToList.remove(masterName);
	}
	
}
