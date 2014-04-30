import java.lang.reflect.Array;

// modified from textbook
public class PacketQueue {
	volatile int head = 0, tail =0;
	Packet[] items;
	final public int capacity;
	@SuppressWarnings("unchecked")
	public PacketQueue(int capacity){
		this.capacity=capacity;
		items = (Packet[])Array.newInstance(new Packet(null).getClass(),capacity);
		head = 0; tail = 0;
	}
	public void enq(Packet x) throws FullException {
		if (tail-head == items.length)
			throw new FullException();
		items[tail% items.length]=x;
		tail++;
	}
	public Packet deq() throws EmptyException {
		if (tail - head == 0)
			throw new EmptyException();
		Packet x = items[head % items.length];
		head++;
		return x;
	}
}
