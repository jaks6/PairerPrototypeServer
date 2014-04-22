package ee.ut.cs.mc.pairerprototype.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.distance.fastdtw.FastDTW;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@SuppressWarnings("unchecked")
public class ClustererThread extends Thread {
	Logger log = Logger.getLogger("ClustererThreads");
	BlockingQueue<JSONObject> queue = null;
	HashMap<String, JSONObject> deviceDetailsMap;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static JSONParser parser;

	public ClustererThread(BlockingQueue<JSONObject> queue, ConcurrentHashMap<String, JSONObject> instructionsMap) {
		super();
		this.queue = queue;
		this.instructionsMap = instructionsMap;
		parser =new JSONParser();
		deviceDetailsMap = new HashMap<String, JSONObject>();
	}

	@Override
	public void run() {
		//Print synced time
		log.info("Running new clusterer thread		" +
				SntpClient.parseMsTimeToHHMMSS(
						TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) + 
						AppContextListener.timediff));
		
		
		DefaultDataset dataset = new DefaultDataset();
		fillDataSetFromQueue(dataset);
		if(!dataset.isEmpty()){
			log.info("Clustering dataset of"+ dataset.size() + " elements");
			log.info(dataset.toString());
			Dataset[] clusters = runClusterer(dataset);
			System.out.println("Results no of clusters=" + clusters.length);
			
			//Provide instructions for each device (and cluster)
			for (int i=0; i<clusters.length; i++) {
				log.info("SHOWING CLASSES=" + clusters[i].classes());
				for (int j=0; j <clusters[i].size(); j++){
					createInstructions(j, clusters[i]);
					//!todo write instructions in an update style
				}
			}
		}
		log.info("Stopping clusterer thread");
	}

	private void createInstructions(int instanceIndex, Dataset ds) {
		String connectToMac = null;
		boolean beHost = false;
		
		String deviceMac = (String) ds.get(instanceIndex).classValue();
		/** This device will have no neighbor to be a host for, if
			the device is first in the chain of devices. */
		beHost = ( instanceIndex != 0);
		
		if (instanceIndex < ds.size()-1 ){
			connectToMac = (String) ds.get(instanceIndex+1).classValue();
		}
		JSONObject instructions = new JSONObject();
		if (connectToMac!= null) instructions.put("connectto", connectToMac);
		instructions.put("listen", beHost);
		instructions.put("group", ds.classes());
		log.info("**Storing instructions for="+ deviceMac+ " i="+instanceIndex+", instructions are=" + instructions.toString());
		instructionsMap.put(deviceMac, instructions);
	}

	private void fillDataSetFromQueue(DefaultDataset dataset) {
		while(! queue.isEmpty()){
			JSONObject json = queue.poll();
			if (json != null) {
				log.info("Taking : " + json.toJSONString());
				long timestamp = (Long) json.get("timestamp");
				log.info("--TIMESTAMP--:" + SntpClient.parseMsTimeToHHMMSS(timestamp));

				DenseInstance instance = createNormalizedInstance(json);
				deviceDetailsMap.put((String)json.get("device"), json);
				dataset.add(instance);
			} else {

			}
		}
	}

	/** Produces a DenseInstance from the json in which the sample values are normalized.
	 * NB - this method removes the "sequence" object from the argument json.
	 * The classvalue of the instance which is used to later identify the device once clustered,
	 *   will be the MAC address of the device
	 * @param dataset
	 * @param json
	 */
	private DenseInstance createNormalizedInstance(JSONObject json) {
		List<Number> sequence = new ArrayList<Number>(
				(JSONArray) json.remove("sequence"));
		double[] target = new double[sequence.size()];
		
		for (int i = 0; i < target.length; i++) {
			target[i] = sequence.get(i).doubleValue();
		}
		normalizeSeq(target);
		
		return new DenseInstance(target, (String)json.get("mac"));
	}

	private void normalizeSeq(double[] seq) {
		double mean = new Mean().evaluate(seq);
		for (int i=0; i<seq.length; i++){
			seq[i]= seq[i]/mean;
		}
	}

	private Dataset[] runClusterer(Dataset dataset) {
		FastDTW fastdtw = new FastDTW(5);

		int minPts = 2;
		double epsilon = 15.5;
		DensityBasedSpatialClustering dbsc = new DensityBasedSpatialClustering(
				epsilon, minPts, fastdtw);
		return dbsc.cluster(dataset);

	}

}
