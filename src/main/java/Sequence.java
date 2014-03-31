import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/sequence")
public class Sequence extends HttpServlet {
	private static final long serialVersionUID = 1L;

	public Sequence() {
		//Servlet constructor
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getOutputStream().println("Hurray !! This Servlet Works");

		WorkThread thread = new WorkThread(); //this instance is created for testing purposes
		thread.testClustering();
		
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			
			//add JSON object from request to a queue, form which a WorkThread will gather it and process it
			JSONObject receivedJson = getJSONFromRequest(request);
			ServletContext ctx = getServletContext();
			final BlockingQueue<JSONObject> queue = (LinkedBlockingQueue<JSONObject>) ctx.getAttribute("queue");
			queue.add(receivedJson);

			//Write response to request
			OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
			writer.write("Server successfully received your POST");
			writer.flush();
			writer.close();


		} catch (IOException e) {
			try{
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().print(e.getMessage());
				response.getWriter().close();
			} catch (IOException ioe) {
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}  
	}

	private JSONObject getJSONFromRequest(HttpServletRequest request)
			throws ParseException, IOException {
		return (JSONObject) new JSONParser().parse(request.getReader());
	}

	

}