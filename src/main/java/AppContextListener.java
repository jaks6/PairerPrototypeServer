import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

@WebListener
    public class AppContextListener implements ServletContextListener{

	final static Logger log = Logger.getLogger("ContextListener");
	WorkThread thread;

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Context Destroyed");
		if (thread != null) {
			thread.terminate();
            try {
				thread.join(); //wait for thread to die.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
            log.info("Thread successfully stopped.");
        }
		
	}


	@Override
	public void contextInitialized(ServletContextEvent e) {
		final BlockingQueue<JSONObject> queue = new LinkedBlockingQueue<JSONObject>();
		log.info("Context Created");
		
		ServletContext ctx = e.getServletContext();
		ctx.setAttribute("queue", queue);
		
		thread = new WorkThread(queue);
		thread.start();
		
	}

}