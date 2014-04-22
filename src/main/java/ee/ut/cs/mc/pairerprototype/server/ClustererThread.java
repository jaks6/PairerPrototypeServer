package ee.ut.cs.mc.pairerprototype.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.fastdtw.FastDTW;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("unchecked")
public class ClustererThread extends Thread {
	Logger log = Logger.getLogger("ClustererThreads");
	BlockingQueue<JSONObject> queue = null;
	HashMap<String, JSONObject> deviceDetailsMap;
	static JSONParser parser;

	public ClustererThread(BlockingQueue<JSONObject> queue) {
		super();
		this.queue = queue;
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
			for (Dataset ds : clusters) {
				for (Instance ins : ds){
					System.out.print(ins.classValue() + "," + deviceDetailsMap.get(ins.classValue())+ "; ");
				}
				System.out.println();
			}
		}
		log.info("Stopping clusterer thread");
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
		return new DenseInstance(target, (String)json.get("device"));
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
