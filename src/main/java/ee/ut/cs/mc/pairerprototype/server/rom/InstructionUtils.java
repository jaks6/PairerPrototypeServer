package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.concurrent.ConcurrentHashMap;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ee.ut.cs.mc.pairerprototype.server.InstructionJSON;
import ee.ut.cs.mc.pairerprototype.server.clustering.RecordingInstance;

public class InstructionUtils {

	public static void processInstructions(MastersRing ring, ConcurrentHashMap<String, JSONObject> instructionsMap, Dataset ds){
		Logger log = Logger.getLogger(InstructionUtils.class);
		String groupID = ring.getId();
		JSONArray groupMembers = getGroupNicknames(ds);

		for (String master : ring){
			//Create instructions for the master itself
			JSONArray acceptFromList = new JSONArray();
			JSONArray connectToList = new JSONArray();

			acceptFromList.addAll(ring.getSlaves(master));
			acceptFromList.add(ring.getLeftNeighbour(master));
			connectToList.add(ring.getRightNeighbour(master));

			instructionsMap.put(master, new InstructionJSON(acceptFromList, connectToList,groupID, groupMembers));
			log.info("Creating instructions for " + master + " = " + instructionsMap.get(master));
			//Create instructions for each slave of that master
			for(String slave : ring.getSlaves(master)){
				
				instructionsMap.put(slave, new InstructionJSON(master, groupID, groupMembers ));
				log.info("Creating instructions for " + slave + " = " + instructionsMap.get(slave));
			}
		}
	}
	
	
	private static JSONArray getGroupNicknames(Dataset ds){
		JSONArray nicknames = new JSONArray();
		for(Instance instance: ds){
			nicknames.add(((RecordingInstance)instance).getDeviceNickName());
		}
		return nicknames;
	}

}
