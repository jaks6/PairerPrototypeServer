package ee.ut.cs.mc.pairerprototype.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import ee.ut.cs.mc.pairerprototype.server.clustering.ClustererThread;

public class WorkThread extends Thread {
	private static final int CLUSTERING_INTERVAL = 10000;
	ScheduledThreadPoolExecutor scheduledExecutor;
	BlockingQueue<JSONObject> dataQueue = null;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static Logger log = Logger.getLogger("WorkThread");

	public WorkThread(BlockingQueue<JSONObject> dataQueue, ConcurrentHashMap<String, JSONObject> instructionsMap) {
		scheduledExecutor = (ScheduledThreadPoolExecutor)
				Executors.newScheduledThreadPool(3);
		this.dataQueue = dataQueue;
		this.instructionsMap = instructionsMap;
	}

	@Override
	public void run() {
		log.info("WorkThread running.");
		waitTilTimeSynced();
		long initialDelay = SntpClient.calculateInitialDelay();
		
		scheduledExecutor.scheduleAtFixedRate(
				new ClustererThread(dataQueue, instructionsMap),
				initialDelay,
				CLUSTERING_INTERVAL,
				TimeUnit.MILLISECONDS);
		
	}

	private void waitTilTimeSynced() {
		while(AppContextListener.timediff == 0){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.info("Waiting for time syncing to finish");
		}
		log.info("timeDiff ="+ AppContextListener.timediff);
	}
}
