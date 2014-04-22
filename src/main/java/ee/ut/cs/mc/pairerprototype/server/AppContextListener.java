package ee.ut.cs.mc.pairerprototype.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

@WebListener
public class AppContextListener implements ServletContextListener {

	final static Logger log = Logger.getLogger("ContextListener");
	final static String NTP_SERVER = "ntp.estpak.ee";
	private static final int TIMEOUT_PERIOD = 3000;
	private static final int REQUEST_INTERVAL_LENGTH = 2000;
	private static final int NO_OF_REQUESTS = 1;

	public static Long timediff = (long) 0;
	WorkThread thread;

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Context Destroyed");
		if (thread != null) {
			thread.terminate();
			try {
				thread.join(); // wait for thread to die.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			log.info("Thread successfully stopped.");
		}

	}

	@Override
	public void contextInitialized(ServletContextEvent e) {
		log.info("Context Creation");
		final BlockingQueue<JSONObject> dataQueue = new LinkedBlockingQueue<JSONObject>();

		ServletContext ctx = e.getServletContext();
		ctx.setAttribute("dataQueue", dataQueue);
		getDifferenceFromServerTime();

		thread = new WorkThread(dataQueue);
		thread.start();

	}

	private void getDifferenceFromServerTime() {
		Thread t = new Thread(){
			
			@Override
			public void run() {
				List<Long> timeDiffs = new ArrayList<Long>();
				int i = 0;
				SntpClient client = new SntpClient();
				while (i <= NO_OF_REQUESTS-1) {
					if (client.requestTime(NTP_SERVER, TIMEOUT_PERIOD)) {
						long now = client.getNtpTime()
								- client.getNtpTimeReference();
						timeDiffs.add(now);
						i++;
						try {
							Thread.sleep(REQUEST_INTERVAL_LENGTH);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				timediff = (Collections.min(timeDiffs));
				
				return ;
			}
			
		};
		t.start();
	}
}