package ee.ut.cs.mc.pairerprototype.server;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ee.ut.cs.mc.pairerprototype.server.clustering.RecordingInstance;
import ee.ut.cs.mc.pairerprototype.server.rom.InstructionUtils;
import ee.ut.cs.mc.pairerprototype.server.rom.MastersRing;


public class GroupsManager2 {
	private static final int _NETWORK_AGE_THRESHOLD = 3;
	Logger log = Logger.getLogger("GroupsManager");
	private static GroupsManager2 instance = null;
	
	public HashMap<String, MastersRing> networksMap = new HashMap<>();
	
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	InstructionUtils ct = new InstructionUtils();
	public static HashMap<String, String> connectToMap = new HashMap<String, String>();
	
	
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
		ageNetworksMap();
		
		
		//Go through each cluster
		for (Dataset cluster : clusters){
			//Find highest occurring groupId in cluster
			String groupId = findMostCommonGroup(cluster);
			JSONArray groupmembers = getGroupNicknames(cluster);
			
			String newGroupId;
			if (!networksMap.containsKey(groupId)){
				newGroupId = createNetwork(cluster);
			} else {
				newGroupId = updateNetwork(cluster, groupId);
			}
			//Set groupmembers for ring to include them in instructions.
			networksMap.get(newGroupId).setGroupMembers(groupmembers);
			InstructionUtils.processInstructions(networksMap.get(newGroupId),instructionsMap);
		}
		cleanNetworksMap();
	}

	
	private static JSONArray getGroupNicknames(Dataset ds){
		JSONArray nicknames = new JSONArray();
		for(Instance instance: ds){
			nicknames.add(((RecordingInstance)instance).getDeviceNickName());
		}
		return nicknames;
	}
	/** Goes through all networks in the map and ages them.	 */
	private void ageNetworksMap() {
		for (MastersRing ring : networksMap.values()){
			ring.ageByOne();
		}
	}
	
	/** Goes through all networks in the map and removes the networks above the age threshold them.	 */
	private void cleanNetworksMap() {
		Iterator<MastersRing> iterator = networksMap.values().iterator();
		while(iterator.hasNext()){
			MastersRing ring = iterator.next();
			if (ring.getAge() > _NETWORK_AGE_THRESHOLD){
				iterator.remove();
			}
		}
	}


	private String createNetwork(Dataset cluster) {
		String groupId = UUID.randomUUID().toString();

		MastersRing ring = new MastersRing(groupId);
		int desiredNoOfMasters = noOfMasters(cluster.size());
		
		for (int i =0; i < desiredNoOfMasters; i++){
			Instance node = cluster.remove(0);
			ring.addMaster(node, i);
		}
		
		for (Instance slave : cluster){
			ring.addSlave(slave);
		}
		networksMap.put(groupId, ring);
		return groupId;
	}
	
	/*** This method creates instructions, manages the links table,e tc 
	 * @throws UnexpectedException */
	private String updateNetwork(Dataset cluster, String groupId) throws UnexpectedException {
		MastersRing ring = networksMap.remove(groupId);
		ring.resetAge();
		groupId = UUID.randomUUID().toString();

		//Check how many original nodes exist,verify them 
		int oldMastersLost = ring.verifyNodes(cluster);
		cleanCluster(cluster, ring);
		
		//!TODO
//		ring.resizeRingOfMasters();
		
		//Use up any free new nodes
		ring.addNewNodes(cluster);
		
		//Add missing masters using old slaves
		ring.addMissingMasters(cluster);
		
		//Clean up
		ring.clearDeprecations();
		networksMap.put(groupId, ring);
		return groupId;
	}


	/** Removes all nodes from the cluster which already are in the network (verified)*/
	private void cleanCluster(Dataset cluster, MastersRing ring) {
			Iterator<Instance> clusterIterator = cluster.iterator();
			ArrayList<String> allSlaves = ring.getAllSlaves();
			while ( clusterIterator.hasNext()){
				String nodeMac = clusterIterator.next().classValue().toString();
				if (ring.contains(nodeMac) || 
						allSlaves.contains(nodeMac)){
					clusterIterator.remove();
					
				}
			}
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
	
	/** Finds the appropriate number of masters for a Ring Of Masters network, given the
	 * total number of members.
	 * @param groupMemberCount
	 * @return
	 */
	private int noOfMasters(int groupMemberCount){
		if (groupMemberCount < 2) return groupMemberCount;
		return (int) Math.ceil(groupMemberCount / 3.0);
	}
	/** Finds the appropriate number of masters for a Ring Of Masters network, given the
	 * total number of members. Note that the number of masters is always even here.
	 * @param groupMemberCount
	 * @return
	 */
	private int evenNoOfMasters(int groupMemberCount){
		if (groupMemberCount < 2) return groupMemberCount;
		int alpha = (int) Math.ceil(groupMemberCount / 3.0);
		if (alpha % 2 == 0){
			return  alpha;
		}
		else {
			return alpha + 1;
		}
	}
	
	public void clearInstructionsMap(){
		log.info("Instructionsmap size before clearing="+instructionsMap.size());
		instructionsMap.clear();
		log.info("Instructionsmap size after clearing="+instructionsMap.size());
	}
}
