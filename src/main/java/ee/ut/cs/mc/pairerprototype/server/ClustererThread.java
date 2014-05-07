package ee.ut.cs.mc.pairerprototype.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
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
//	HashMap<String, JSONObject> deviceDetailsMap;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static JSONParser parser = new JSONParser();;

	public ClustererThread(BlockingQueue<JSONObject> queue, ConcurrentHashMap<String, JSONObject> instructionsMap) {
		super();
		this.queue = queue;
		this.instructionsMap = instructionsMap;
//		deviceDetailsMap = new HashMap<String, JSONObject>();
	}

	@Override
	public void run() {
		//Print synced time
		log.info("Running new clusterer thread		" +
				SntpClient.parseMsTimeToHHMMSS(
						TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS) + 
						AppContextListener.timediff));
		
		//!TODO : add filter for too old timestamps 
		DefaultDataset dataset = new DefaultDataset();
		fillDataSetFromQueue(dataset);
		if(!dataset.isEmpty()){
			log.info("Clustering dataset of"+ dataset.size() + " elements");
			log.info(dataset.toString());
			
			Dataset[] clusters = runClusterer(dataset);
			System.out.println("Results no of clusters=" + clusters.length);
			
			GroupsManager.getInstance().processClusters(clusters);
			//Provide instructions for each device (and cluster)
			for (int i=0; i<clusters.length; i++) {
				log.info("SHOWING CLASSES=" + clusters[i].classes());
				for (int j=0; j <clusters[i].size(); j++){
				}
			}
		}
		log.info("Stopping clusterer thread");
	}

	private void fillDataSetFromQueue(DefaultDataset dataset) {
		while(! queue.isEmpty()){
			JSONObject json = queue.poll();
			if (json != null) {

				RecordingInstance instance = createNormalizedInstance(json);
				dataset.add(instance);
			} else {

			}
		}
	}

	/** Produces a DenseInstance subclass RecordingInstance from the json in which the sample values are normalized.
	 * NB - this method removes the "sequence" object from the argument json.
	 * The classvalue of the instance which is used to later identify the device once clustered,
	 *   will be the MAC address of the device
	 * @param dataset
	 * @param json
	 */
	private RecordingInstance createNormalizedInstance(JSONObject json) {
		List<Number> sequence = new ArrayList<Number>(
				(JSONArray) json.remove("sequence"));
		double[] target = new double[sequence.size()];
		
		for (int i = 0; i < target.length; i++) {
			target[i] = sequence.get(i).doubleValue();
		}
		double[] normalizedSeq = energy(target);
		RecordingInstance instance = new RecordingInstance(target, (String)json.get("mac"));
		instance.setDeviceNickName((String)json.get("nickname"));
		return instance;
	}

	private void normalizeSeq(double[] seq) {
		double mean = new Mean().evaluate(seq);
		for (int i=0; i<seq.length; i++){
			seq[i]= seq[i]/mean;
		}
	}
	
	private double[] energy(double[] seq){
		final int windowLength = 5;
		double[] hammondVals = {0.08, 0.54, 1, 0.54, 0.08};
		int samples = seq.length;
		double[] result = new double[samples];
		double sum;
		for (int i = 0; i<samples-windowLength; i++){
			sum = 0;
			for (int w=0; w< windowLength; w++){
				sum += Math.sqrt((seq[i+w]*seq[i+w])* hammondVals[w]);
			}
			result[i] = sum / windowLength;
		}
		return result;
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
