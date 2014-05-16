
public class ParallelDispatcher implements Runnable {
	private PacketQueue[] queues;
	private PacketGenerator source;
	private PaddedPrimitiveNonVolatile<Boolean> finished;
	public int totalPackets;

	public ParallelDispatcher(PacketQueue[] queues, PacketGenerator source,
			PaddedPrimitiveNonVolatile<Boolean> finished) {
		this.queues = queues;
		this.source = source;
		this.finished = finished;
		this.totalPackets = 0;
	}

	public void run() {
		Packet p;
		while (!finished.value) {
			for (int i = 0; i < queues.length - 1; i++) {
				p = source.getPacket();
				switch (p.type) {
				case ConfigPacket:
					while (p != null) {
						try {
							queues[queues.length - 1].enq(p);
							totalPackets += 1;
							p = null;
							break;
						} catch (FullException e) {
							
						}
					}
					break;
				case DataPacket:
					while (p != null) {
						for (int j = 0; j < queues.length - 1; j++) {
							try {
								queues[(j + i) % (queues.length - 1)].enq(p);
								totalPackets += 1;
								p = null;
								break;
							} catch (FullException e) {
							}
						}
					}
					break;

				}
			}
		}
	}
}
