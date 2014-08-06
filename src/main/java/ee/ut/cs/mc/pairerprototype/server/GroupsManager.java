package ee.ut.cs.mc.pairerprototype.server;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import ee.ut.cs.mc.pairerprototype.server.clustering.RecordingInstance;


public class GroupsManager {
	Logger log = Logger.getLogger("GroupsManager");
	private static GroupsManager instance = null;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static HashMap<String, String> connectToMap = new HashMap<String, String>();
	
	
	public void setInstructionMap(
			ConcurrentHashMap<String, JSONObject> instructionsMap) {
		this.instructionsMap = instructionsMap;
	}

	
	protected GroupsManager( ) {
	}
	
	public static GroupsManager getInstance(){
		if (instance == null){
			instance = new GroupsManager();
		}
		return instance;
	}

	public void processClusters(Dataset[] clusters) {
		
		//!TODO CLEAR OLD INSTRUCTIONS
//		instructionsMap.clear();
		log.info("Going through" + clusters.length + " clusters");
		//Go through each cluster
		for (Dataset cluster : clusters){
			log.info("Going through cluster of size " + cluster.size());
			for (Instance deviceInstance: cluster){
				if (!checkIfWasInSameNetwork(deviceInstance, cluster)){
					//The device is new in the cluster, create instructions for it (see who it should connect to)
					createInitialInstructions(cluster, deviceInstance);
				}
			}
		}
		createInstructionJSONsForCluster(clusters);
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
