
package protocol;

import java.io.Serializable;

public class WrittenHttpResponse implements Serializable {
	private static final long serialVersionUID = -6690449888838725533L;
	private byte[] data;
	private int socketHash;
	private long serviceTime;
	
	public WrittenHttpResponse(byte[] data, int socketHash, long serviceTime) {
		this.data = data;
		this.socketHash = socketHash;
		this.serviceTime = serviceTime;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getSocketHash() {
		return socketHash;
	}

	public void setSocketHash(int socketHash) {
		this.socketHash = socketHash;
	}

	public long getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(long serviceTime) {
		this.serviceTime = serviceTime;
	}
}
