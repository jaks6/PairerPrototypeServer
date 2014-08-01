package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;

/*** A single row of the table which describes how nodes of the ring of masters
 * network connect to each other.
 * @author jaks
 *
 */
public class TableEntry {

	public String mac;
	public ArrayList<String> connectToList;
	public ArrayList<String> acceptFromList;
	
	
	public TableEntry(String mac, ArrayList<String> connectToList,
			ArrayList<String> acceptFromList) {
		super();
		this.mac = mac;
		this.connectToList = connectToList;
		this.acceptFromList = acceptFromList;
	}
}
