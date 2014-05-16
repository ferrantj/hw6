import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

public interface PacketWorker extends Runnable {
	public void run();
}

class STMPacketWorker implements PacketWorker {
	PaddedPrimitiveNonVolatile<Boolean> done;
	final PacketQueue[] sources;
	final ConcurrentHashMap<Long, Integer> table;
	final RangeLists ranges;
	long totalPackets = 0;
	long residue = 0;
	Fingerprint fingerprint;
	final int i;

	public STMPacketWorker(PaddedPrimitiveNonVolatile<Boolean> done,
			PacketQueue[] queues, ConcurrentHashMap<Long, Integer> table,
			int i, RangeLists ranges) {
		this.done = done;
		this.sources = queues;
		this.table = table;
		this.fingerprint = new Fingerprint();
		this.i = i;
		this.ranges = ranges;
	}

	public void run() {
		Random rand = new Random();
		Packet pkt;
		int j = rand.nextInt(sources.length);
		while (!done.value) {
			while (true) {
				try {
					pkt = sources[j].deq();
					switch (pkt.type) {
					case ConfigPacket:
						if (pkt.config.acceptingRange) {
							ranges.add(pkt.config.address,
									pkt.config.addressBegin,
									pkt.config.addressEnd,
									pkt.config.personaNonGrata);
						} else {
							ranges.subtract(pkt.config.address,
									pkt.config.addressBegin,
									pkt.config.addressEnd,
									pkt.config.personaNonGrata);
						}
						break;
					case DataPacket:
						if (ranges.check(pkt.header.source, pkt.header.dest)) {
							long key = fingerprint.getFingerprint(
									pkt.body.iterations, pkt.body.seed);
							if (!table.containsKey(key)) {
								table.put(key, 0);
							}
							table.put(key, table.get(key) + 1);
							totalPackets += 1;
						}
						break;
					}
				} catch (EmptyException e) {
					j = rand.nextInt(sources.length);
					break;
				}
			}
		}
		while (true) {
			try {
				pkt = sources[i].deq();
				switch (pkt.type) {
				case ConfigPacket:
					if (pkt.config.acceptingRange) {
						ranges.add(pkt.config.address, pkt.config.addressBegin,
								pkt.config.addressEnd,
								pkt.config.personaNonGrata);
					} else {
						ranges.subtract(pkt.config.address,
								pkt.config.addressBegin, pkt.config.addressEnd,
								pkt.config.personaNonGrata);
					}
					break;
				case DataPacket:
					if (ranges.check(pkt.header.source, pkt.header.dest)) {
						long key = fingerprint.getFingerprint(
								pkt.body.iterations, pkt.body.seed);
						if (!table.containsKey(key)) {
							table.put(key, 0);
						}
						table.put(key, table.get(key) + 1);
						totalPackets += 1;
					}
					break;
				}
			} catch (EmptyException e) {
				break;
			}
		}
	}

}

class ParallelPacketWorker implements PacketWorker {
	PaddedPrimitiveNonVolatile<Boolean> done;
	final PacketQueue[] sources;
	final ConcurrentHashMap<Long, Integer> table;
	final RangeLists ranges;
	long totalPackets = 0;
	long residue = 0;
	Fingerprint fingerprint;
	final int i;
	final Lock[] locks;

	public ParallelPacketWorker(PaddedPrimitiveNonVolatile<Boolean> done,
			PacketQueue[] queues, ConcurrentHashMap<Long, Integer> table,
			int i, RangeLists ranges,Lock[] locks) {
		this.done = done;
		this.sources = queues;
		this.table = table;
		this.fingerprint = new Fingerprint();
		this.i = i;
		this.ranges = ranges;
		this.locks = locks;
	}

	public void run() {
		Random rand = new Random();
		Packet pkt;
		int j = rand.nextInt(sources.length);
		boolean locked = false;
		while (!done.value) {
			while (true) {
				if(!locked){
					while(!locks[j].tryLock()){
						j=rand.nextInt(locks.length);
					}
					locked=true;
				}
				try {
					pkt = sources[j].deq();
					if (ranges.check(pkt.header.source, pkt.header.dest)) {
						long key = fingerprint.getFingerprint(
								pkt.body.iterations, pkt.body.seed);
						if (!table.containsKey(key)) {
							table.put(key, 0);
						}
						table.put(key, table.get(key) + 1);
						totalPackets += 1;
					}
				} catch (EmptyException e) {
					locks[j].unlock();
					locked=false;
					j = rand.nextInt(sources.length);
					break;
				}
			}
		}
		while (true) {
			try {
				locks[i].lock();
				pkt = sources[i].deq();
				if (ranges.check(pkt.header.source, pkt.header.dest)) {
					long key = fingerprint.getFingerprint(pkt.body.iterations,
							pkt.body.seed);
					if (!table.containsKey(key)) {
						table.put(key, 0);
					}
					table.put(key, table.get(key) + 1);
					totalPackets += 1;
				}
			} catch (EmptyException e) {
				locks[i].unlock();
				break;
			}
		}
	}

}

class ParallelConfigPacketWorker implements PacketWorker {
	PaddedPrimitiveNonVolatile<Boolean> done;
	final PacketQueue[] sources;
	final ConcurrentHashMap<Long, Integer> table;
	final RangeLists ranges;
	long totalPackets = 0;
	long residue = 0;
	Fingerprint fingerprint;
	final int i;
	final Lock[] locks;
	

	public ParallelConfigPacketWorker(PaddedPrimitiveNonVolatile<Boolean> done,
			PacketQueue[] queues, ConcurrentHashMap<Long, Integer> table,
			int i, RangeLists ranges,Lock[] locks) {
		this.done = done;
		this.sources = queues;
		this.table = table;
		this.fingerprint = new Fingerprint();
		this.i = i;
		this.ranges = ranges;
		this.locks = locks;
	}

	public void run() {
		
		Random rand = new Random();
		Packet pkt;
		int j = rand.nextInt(sources.length);
		boolean locked=false;
		while (!done.value) {
			while (true) {
				try {
					if(!locked){
						while(!locks[j].tryLock()){
							j=rand.nextInt(locks.length);
						}
						locked=true;
					}
					pkt = sources[j].deq();
					if (pkt.config.acceptingRange) {
						ranges.add(pkt.config.address, pkt.config.addressBegin,
								pkt.config.addressEnd,
								pkt.config.personaNonGrata);
					} else {
						ranges.subtract(pkt.config.address,
								pkt.config.addressBegin, pkt.config.addressEnd,
								pkt.config.personaNonGrata);
					}
					break;
				} catch (EmptyException e) {
					locks[j].unlock();
					locked=false;
					j = rand.nextInt(sources.length);
					break;
				}
			}
		}
		while (true) {
			try {
				locks[i].lock();
				pkt = sources[i].deq();
				if (pkt.config.acceptingRange) {
					ranges.add(pkt.config.address, pkt.config.addressBegin,
							pkt.config.addressEnd, pkt.config.personaNonGrata);
				} else {
					ranges.subtract(pkt.config.address,
							pkt.config.addressBegin, pkt.config.addressEnd,
							pkt.config.personaNonGrata);
				}
			} catch (EmptyException e) {
				locks[i].unlock();
				break;
			}
		}
	}

}