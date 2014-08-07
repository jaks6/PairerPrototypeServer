package ee.ut.cs.mc.pairerprototype.server.rom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/*** A single row of the table which describes how nodes of the ring of masters
 * network connect to each other.
 * @author jaks
 *
 */
public class TableEntry {

	public String mac;
	public HashSet<String> connectToList;
	public HashSet<String> acceptFromList;
	
	
	public TableEntry(String mac, HashSet<String> connectToList,
			HashSet<String> acceptFromList) {
		super();
		this.mac = mac;
		this.connectToList = connectToList;
		this.acceptFromList = acceptFromList;
	}
	
	
	public boolean isMaster(){
		return ! acceptFromList.isEmpty();
	}
}
