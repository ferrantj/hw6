
public class ParallelDispatcher implements Runnable {
	private PacketQueue[] queues;
	private PacketGenerator source;
	private PaddedPrimitiveNonVolatile<Boolean> finished;
	public int totalPackets;
	private PacketQueue[] configs;

	public ParallelDispatcher(PacketQueue[] queues,PacketQueue[] configs, PacketGenerator source,
			PaddedPrimitiveNonVolatile<Boolean> finished) {
		this.configs = configs;
		this.queues = queues;
		this.source = source;
		this.finished = finished;
		this.totalPackets = 0;
	}

	public void run() {
		Packet p;
		while (!finished.value) {
			for (int i = 0; i < queues.length; i++) {
				p = source.getPacket();
				switch (p.type) {
				case ConfigPacket:
					while (p != null) {
						for (int j = 0; j < configs.length; j++) {
							try {
								configs[(j + i) % (configs.length)].enq(p);
								totalPackets += 1;
								p = null;
								break;
							} catch (FullException e) {
								System.out.println("not processsing fast enough");
							}
						}
					}
					break;
				case DataPacket:
					while (p != null) {
						for (int j = 0; j < queues.length; j++) {
							try {
								queues[(j + i) % (queues.length)].enq(p);
								totalPackets += 1;
								p = null;
								break;
							} catch (FullException e) {
								System.out.println("data is filling");
							}
						}
					}
					break;

				}
			}
		}
	}
}
