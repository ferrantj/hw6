
public class Dispatcher implements Runnable{
	private PacketQueue[] queues;
	private PacketGenerator source;
	private PaddedPrimitiveNonVolatile<Boolean> finished;
	public  int totalPackets;
	public Dispatcher(PacketQueue[] queues,PacketGenerator source,PaddedPrimitiveNonVolatile<Boolean> finished){
		this.queues=queues;
		this.source=source;
		this.finished=finished;
		this.totalPackets = 0;
	}
	public void run(){
		Packet p;
		while(!finished.value){
			for (int i=0;i<queues.length;i++){
				p=source.getPacket();
				while(p!=null){
					for (int j=0;j<queues.length;j++){
						try {
							queues[(j+i)%queues.length].enq(p);
							totalPackets+=1;
							p=null;
							break;
						} catch (FullException e) {
						}
					}
				}
			}
		}
	}
}