package ee.ut.cs.mc.pairerprototype.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

public class WorkThread extends Thread {
	ScheduledThreadPoolExecutor scheduledExecutor;
	BlockingQueue<JSONObject> queue = null;
	ConcurrentHashMap<String, JSONObject> instructionsMap;
	static Logger log = Logger.getLogger("WorkThread");

	public WorkThread(BlockingQueue<JSONObject> queue, ConcurrentHashMap<String, JSONObject> instructionsMap) {
		scheduledExecutor = (ScheduledThreadPoolExecutor)
				Executors.newScheduledThreadPool(3);
		this.queue = queue;
		this.instructionsMap = instructionsMap;
	}

	@Override
	public void run() {
		log.info("WorkThread running.");
		waitTilTimeSynced();
		long initialDelay = SntpClient.calculateInitialDelay();
		scheduledExecutor.scheduleAtFixedRate(new ClustererThread(queue, instructionsMap), initialDelay, 10000, TimeUnit.MILLISECONDS);
		
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
