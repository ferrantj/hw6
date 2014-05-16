import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

class SerialFirewall {
	public static void main(String[] args) {
		final int numAddressesLog=Integer.parseInt(args[0]);
	    final int numTrainsLog=Integer.parseInt(args[1]);
	    final double meanTrainSize=Double.parseDouble(args[2]);
	    final double meanTrainsPerComm=Double.parseDouble(args[3]);
	    final int meanWindow=Integer.parseInt(args[4]);
	    final int meanCommsPerAddress=Integer.parseInt(args[5]);
	    final int meanWork=Integer.parseInt(args[6]);
	    final double configFraction=Double.parseDouble(args[7]);
	    final double pngFraction=Double.parseDouble(args[8]);
	    final double acceptingFraction=Double.parseDouble(args[9]);
	    final int numMilliseconds = Integer.parseInt(args[10]);
	    int totalPackets = 0;
	    
	    //initiating source, address ranges, fingerprint, and hashmap for histogram
	    RangeLists ranges = new SerialRangeLists();
		PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog,
				meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress,
				meanWork, configFraction, pngFraction, acceptingFraction);
		HashMap<Long,Integer> checksums=new HashMap<Long,Integer>();
		Fingerprint fingerprint=new Fingerprint();
		//preconfiguring address ranges
		for(int i=0;i<Math.pow(Math.pow(2, numAddressesLog), 1.5);i++){
			Packet packet=source.getConfigPacket();
			if(packet.config.acceptingRange){
				ranges.add(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
			else{
				ranges.subtract(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
		}
		long start = System.currentTimeMillis();
		while(System.currentTimeMillis()<start+numMilliseconds){
			Packet packet=source.getPacket();
			switch(packet.type){
			case ConfigPacket:
				if(packet.config.acceptingRange){
					ranges.add(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
				}
				else{
					ranges.subtract(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
				}
				break;
			case DataPacket:
				if(ranges.check(packet.header.source,packet.header.dest)){
					long key = fingerprint.getFingerprint(packet.body.iterations, packet.body.seed);
					if(!checksums.containsKey(key)){
						checksums.put(key, 0);
					}
					checksums.put(key, checksums.get(key)+1);
					totalPackets+=1;
				}
				break;
			}
		}
		long end = System.currentTimeMillis();
		System.out.println( totalPackets/(end-start));
	}
}
class STMFirewall {
	public static void main(String[] args){
		final int numAddressesLog=Integer.parseInt(args[0]);
	    final int numTrainsLog=Integer.parseInt(args[1]);
	    final double meanTrainSize=Double.parseDouble(args[2]);
	    final double meanTrainsPerComm=Double.parseDouble(args[3]);
	    final int meanWindow=Integer.parseInt(args[4]);
	    final int meanCommsPerAddress=Integer.parseInt(args[5]);
	    final int meanWork=Integer.parseInt(args[6]);
	    final double configFraction=Double.parseDouble(args[7]);
	    final double pngFraction=Double.parseDouble(args[8]);
	    final double acceptingFraction=Double.parseDouble(args[9]);
	    final int numMilliseconds = Integer.parseInt(args[10]);
	    final int n = Integer.parseInt(args[11]);
	    
	    //initiating source, address ranges, fingerprint, and hashmap for histogram
	    RangeLists ranges = new STMRangeLists();
		PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog,
				meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress,
				meanWork, configFraction, pngFraction, acceptingFraction);
		ConcurrentHashMap<Long,Integer> checksums=new ConcurrentHashMap<Long,Integer>(); 
		PacketQueue[] queues = new PacketQueue[n];
	    for(int i=0; i<n; i++){
	    	queues[i]=new PacketQueue(8);
	    }
		//preconfiguring address ranges
		for(int i=0;i<Math.pow(Math.pow(2, numAddressesLog), 1.5);i++){
			Packet packet=source.getConfigPacket();
			if(packet.config.acceptingRange){
				ranges.add(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
			else{
				ranges.subtract(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
		}
		//control signals
		PaddedPrimitiveNonVolatile<Boolean> finished = new PaddedPrimitiveNonVolatile<Boolean>(false);
	    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
	    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		
		//dispatcher and workers
	    Dispatcher dispatcher = new Dispatcher(queues, source, finished);
	    Thread dispatcherThread = new Thread(dispatcher);
	    
	    Thread[] workers = new Thread[n];
	    STMPacketWorker[] workersdata = new STMPacketWorker[n];
	    for(int i=0; i<n; i++){
	    	workersdata[i]=new STMPacketWorker(done, queues,checksums,i,ranges);
	    	workers[i]= new Thread(workersdata[i]);
	    }
	    //start workers
	    for(Thread worker:workers){
	    	worker.start();
	    }
	    
		long start = System.currentTimeMillis();
		
		//start dispatcher
		dispatcherThread.start();
		try {
		      Thread.sleep(numMilliseconds);
		    } catch (InterruptedException ignore) {;}
		finished.value=true;
	    memFence.value=true;
	    //wait for dispatcher to finish
	    try {
			dispatcherThread.join();
		} catch (InterruptedException e) {
		}
	    //wait for workers to finish
	    done.value=true;
	    for(int i=0; i<workers.length; i++){
	    	try {
				workers[i].join();
			} catch (InterruptedException e) {
			}
	    }
	    
		long end = System.currentTimeMillis();
		System.out.println( dispatcher.totalPackets/(end-start));	
	}
}
class ParellelFirewall{
	public static void main(String[] args){
		if (args.length==2){
			System.out.println(args[0]+" "+args[1]);
			if (args[0].equals("1")){
				args=new String[]{"11","12","5","1","3","3","3822","0.24","0.04","0.96","2000",args[1]};
			}
			else if(args[0].equals("2")){
				args=new String[]{"12","10","1","3","3","1","2644","0.11","0.09","0.92","2000",args[1]};
			}
			else if(args[0].equals("3")){
				args=new String[]{"12","10","4","3","6","2","1304","0.10","0.03","0.90","2000",args[1]};
			}
			else if(args[0].equals("4")){
				args=new String[]{"14","10","5","5","6","2","315","0.08","0.05","0.90","2000",args[1]};
			}
			else if(args[0].equals("5")){
				args=new String[]{"15","14","9","16","7","10","4007","0.02","0.10","0.84","2000",args[1]};
			}
			else if(args[0].equals("6")){
				args=new String[]{"15","14","9","10","9","9","7125","0.01","0.20","0.77","2000",args[1]};
			}
			else if(args[0].equals("7")){
				args=new String[]{"15","15","10","13","8","10","5328","0.04","0.18","0.80","2000",args[1]};
			}
			else if(args[0].equals("8")){
				args=new String[]{"16","14","15","12","9","5","8840","0.04","0.19","0.76","2000",args[1]};
			}
		}
		final int numAddressesLog=Integer.parseInt(args[0]);
	    final int numTrainsLog=Integer.parseInt(args[1]);
	    final double meanTrainSize=Double.parseDouble(args[2]);
	    final double meanTrainsPerComm=Double.parseDouble(args[3]);
	    final int meanWindow=Integer.parseInt(args[4]);
	    final int meanCommsPerAddress=Integer.parseInt(args[5]);
	    final int meanWork=Integer.parseInt(args[6]);
	    final double configFraction=Double.parseDouble(args[7]);
	    final double pngFraction=Double.parseDouble(args[8]);
	    final double acceptingFraction=Double.parseDouble(args[9]);
	    final int numMilliseconds = Integer.parseInt(args[10]);
	    final int n = Integer.parseInt(args[11]);
	    
	    //initiating source, address ranges, fingerprint, and hashmap for histogram
	    RangeLists ranges = new ParallelRangeLists();
		PacketGenerator source = new PacketGenerator(numAddressesLog, numTrainsLog,
				meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress,
				meanWork, configFraction, pngFraction, acceptingFraction);
		ConcurrentHashMap<Long,Integer> checksums=new ConcurrentHashMap<Long,Integer>(); 
		PacketQueue[] queues = new PacketQueue[n+1];
	    for(int i=0; i<n+1; i++){
	    	queues[i]=new PacketQueue(8);
	    }
		//preconfiguring address ranges
		for(int i=0;i<Math.pow(Math.pow(2, numAddressesLog), 1.5);i++){
			Packet packet=source.getConfigPacket();
			if(packet.config.acceptingRange){
				ranges.add(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
			else{
				ranges.subtract(packet.config.address,packet.config.addressBegin,packet.config.addressEnd,packet.config.personaNonGrata);
			}
		}
		//control signals
		PaddedPrimitiveNonVolatile<Boolean> finished = new PaddedPrimitiveNonVolatile<Boolean>(false);
	    PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
	    PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		
		//dispatcher and workers
	    ParallelDispatcher dispatcher = new ParallelDispatcher(queues, source, finished);
	    Thread dispatcherThread = new Thread(dispatcher);
	    
	    Thread[] workers = new Thread[n+1];
	    PacketWorker[] workersdata = new PacketWorker[n+1];
	    for(int i=0; i<n; i++){
	    	workersdata[i]=new ParallelPacketWorker(done, queues,checksums,i,ranges);
	    	workers[i]= new Thread(workersdata[i]);
	    }
	    workersdata[n]=new ParallelConfigPacketWorker(done, queues,checksums,n,ranges);
	    workers[n]=new Thread(workersdata[n]);
	    //start workers
	    for(Thread worker:workers){
	    	worker.start();
	    }
	    
		long start = System.currentTimeMillis();
		
		//start dispatcher
		dispatcherThread.start();
		try {
		      Thread.sleep(numMilliseconds);
		    } catch (InterruptedException ignore) {;}
		finished.value=true;
	    memFence.value=true;
	    //wait for dispatcher to finish
	    try {
			dispatcherThread.join();
		} catch (InterruptedException e) {
		}
	    //wait for workers to finish
	    done.value=true;
	    for(int i=0; i<workers.length; i++){
	    	try {
				workers[i].join();
			} catch (InterruptedException e) {
			}
	    }
	    
		long end = System.currentTimeMillis();
		System.out.println( dispatcher.totalPackets/(end-start));
	}
}