package ee.ut.cs.mc.pairerprototype.server;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import ee.ut.cs.mc.pairerprototype.server.clustering.RecordingInstance;
import ee.ut.cs.mc.pairerprototype.server.rom.ConnectionsTable;
import ee.ut.cs.mc.pairerprototype.server.rom.TableEntry;


public class GroupsManager2 {
	Logger log = Logger.getLogger("GroupsManager");
	private static GroupsManager2 instance = null;
	
	public HashMap<String, ConnectionsTable> networksMap = new HashMap<>();
	
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static HashMap<String, String> connectToMap = new HashMap<String, String>();
	
	
	public void setInstructionMap(
			ConcurrentHashMap<String, JSONObject> instructionsMap) {
		this.instructionsMap = instructionsMap;
	}

	
	protected GroupsManager2( ) {
	}
	
	public static GroupsManager2 getInstance(){
		if (instance == null){
			instance = new GroupsManager2();
		}
		return instance;
	}

	public void processClusters(Dataset[] clusters) throws UnexpectedException {
		//Go through each cluster
		for (Dataset cluster : clusters){
			//Find highest occurring groupId in cluster
			String groupId = findMostCommonGroup(cluster);
			
			updateNetwork(cluster, groupId);
		}
//		instructionsMap.clear();
	}
	/*** This method creates instructions, manages the links table,e tc 
	 * @throws UnexpectedException */
	private void updateNetwork(Dataset cluster, String groupId) throws UnexpectedException {
		 
		int desiredNoOfMasters = numberOfMasters(cluster.size());
		ConnectionsTable networkTable = networksMap.get(groupId);
		
//		if (desiredNoOfMasters < cluster.size() ) desiredNoOfMasters = networkTable.GetCurrentMasterCount();
		
		//Check how many original nodes exist,verify them 
		int oldMastersLost = networkTable.verifyNodes(cluster);
		
		networkTable.cleanCluster(cluster);
		networkTable.getMastersList().resizeRingOfMasters(desiredNoOfMasters);
		
		//Use up any free new nodes
		networkTable.addNewNodes(cluster);
		
		//Add missing masters using old slaves
		networkTable.addMissingMasters(cluster, desiredNoOfMasters);
	}




	/*** 
	Finds the most frequently occurring groupId in the cluster
	@return the group id, or id "0" if none
	*/
	private String findMostCommonGroup(Dataset cluster) {
		//Keep track of each groupId's count in this map
		HashMap<String, Integer> groupScoresMap = new HashMap<>();
		
		String curGroupId = null;
		for (Instance deviceInstance: cluster){
			curGroupId = ((RecordingInstance)deviceInstance).lastGroupId; 
			
			//Increment current group id's score if it exists, otherwise assign score 1 to it.
			try {
				Integer curValue = groupScoresMap.get(curGroupId);
				groupScoresMap.put(curGroupId, curValue + 1);
			} catch (NullPointerException e) {
				groupScoresMap.put(curGroupId, 1);
			}
		}
		
		//Determine which was the largest
		Entry<String, Integer> largestScoringGroupId = null;
		for (Entry<String, Integer> entry : groupScoresMap.entrySet()){
			if (largestScoringGroupId==null || entry.getValue() > largestScoringGroupId.getValue()){
				largestScoringGroupId = entry;
			}
		}
		
		return largestScoringGroupId.getKey();
	}
	
	@SuppressWarnings("unchecked")
	private void createInstructionJSONsForCluster(Dataset[] clusters) {
		for (Entry<String, String> e : connectToMap.entrySet()){
			String instructionReceiver = e.getKey();
			String instructionReceiverHost = e.getValue();
			JSONObject instructions = instructionsMap.get(instructionReceiver);
			if (instructions == null) instructions = new JSONObject();
			JSONObject hostInstructions;
			instructions.put("connectto", instructionReceiverHost);
			
			//Make sure this devices host has the instruction to listen for connections:
			hostInstructions = instructionsMap.get(instructionReceiverHost);
			if (hostInstructions == null) hostInstructions = new JSONObject();
			hostInstructions.put("listento", true);
			
			for (Dataset ds : clusters){
				if (ds.classes().contains(instructionReceiver)){
					instructions.put("group", getGroupNicknames(ds));
					hostInstructions.put("group", getGroupNicknames(ds));
				}
			}
			log.info("**Storing instructions for="+ instructionReceiver+ ", instructions are=" + instructions.toString());
			log.info("**Storing instructions for="+ instructionReceiverHost+ ", instructions are=" + hostInstructions.toString());
			instructionsMap.put(instructionReceiverHost, hostInstructions);
			instructionsMap.put(instructionReceiver, instructions);
		}
	}
	
	
	/** Finds the appropriate number of masters for a Ring Of Masters network, given the
	 * total number of members. Note that the number of masters is always even here.
	 * @param groupMemberCount
	 * @return
	 */
	private int numberOfMasters(int groupMemberCount){
		if (groupMemberCount < 2) return groupMemberCount;
		int alpha = (int) Math.ceil(groupMemberCount / 3.0);
		if (alpha % 2 == 0){
			return  alpha;
		}
		else {
			return alpha + 1;
		}
	}


	private void createInitialInstructions (Dataset cluster, Instance deviceInstance){
		String clientMAC = (String) deviceInstance.classValue();
		
		//remove old instructions of the device were creating instructions for
		connectToMap.remove(clientMAC);
		for (Instance potentialHost: cluster){
			String potentialHostMAC = (String) potentialHost.classValue();
			//Find a device which would be a host for this device
			if (!clientMAC.equals(potentialHostMAC) &&!connectToMap.containsValue(potentialHostMAC)){
				//make the argument device connect to the potential host
				connectToMap.put(clientMAC, potentialHostMAC);
			}
		}
	}
	
	
	private JSONArray getGroupNicknames(Dataset ds){
		JSONArray nicknames = new JSONArray();
		for(Instance instance: ds){
			nicknames.put(((RecordingInstance)instance).getDeviceNickName());
		}
		return nicknames;
		
	}
	
	/** Compares given device to all of the devices in the given cluster,
	 * seeing if there are any client / server relations between any pair
	 * @param deviceInstance
	 * @param cluster
	 * @return
	 */
	private boolean checkIfWasInSameNetwork(Instance deviceInstance, Dataset cluster) {
		String deviceMAC = (String) deviceInstance.classValue();
		for (Instance device: cluster){
			String comparedDeviceMAC = (String ) device.classValue();
			if (comparedDeviceMAC.equals(deviceMAC)) continue;
			boolean deviceIsClient = comparedDeviceMAC.equals(connectToMap.get(deviceMAC));
			if (deviceIsClient ) return true;
			
			boolean deviceIsServer = deviceMAC.equals(connectToMap.get(comparedDeviceMAC));
			if (deviceIsServer ) return true;
			
			log.info("I found that "+ deviceMAC +" and " + comparedDeviceMAC + "have no relations");
		}
		return false;
	}
}
