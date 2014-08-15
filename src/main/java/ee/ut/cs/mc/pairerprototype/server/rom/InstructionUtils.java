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
	static Logger log = Logger.getLogger(InstructionUtils.class);

	@SuppressWarnings("unchecked")
	public static void processInstructions(MastersRing ring, ConcurrentHashMap<String, JSONObject> instructionsMap){
		
		String groupID = ring.getId();

		for (String master : ring){
			//Create instructions for the master itself
			JSONArray acceptFromList = new JSONArray();
			String connectTo;

			acceptFromList.addAll(ring.getSlaves(master));
			acceptFromList.add(ring.getLeftNeighbour(master));
			connectTo = (ring.getRightNeighbour(master));

			instructionsMap.put(master, new InstructionJSON(acceptFromList, connectTo,groupID, ring.groupmembers));
			log.info("Creating instructions for " + master + " = " + instructionsMap.get(master));
			
			//Create instructions for each slave of that master
			for(String slave : ring.getSlaves(master)){
				instructionsMap.put(slave, new InstructionJSON(master, groupID, ring.groupmembers ));
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
