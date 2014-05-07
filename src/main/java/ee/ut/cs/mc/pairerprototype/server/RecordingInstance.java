package ee.ut.cs.mc.pairerprototype.server;

import net.sf.javaml.core.DenseInstance;

public class RecordingInstance extends DenseInstance{
	private static final long serialVersionUID = 1597052719921836676L;
	String deviceNickName = null;
	public String getDeviceNickName() {
		return deviceNickName;
	}

	public void setDeviceNickName(String deviceNickName) {
		this.deviceNickName = deviceNickName;
	}


	public RecordingInstance(double[] att, Object classValue) {
		super(att, classValue);
	}
}
