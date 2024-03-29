import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public interface RangeList {
	public void add(int addressBegin, int addressEnd, boolean personaNonGrata);
	public void subtract(int addressBegin, int addressEnd, boolean personaNonGrata);
	public boolean PNG();
	public boolean contains(int source);
	public void print();
}
class SerialRangeList implements RangeList{
	private TreeMap<Integer,Integer> ranges = new TreeMap<Integer,Integer>();
	private boolean PNG;

	public SerialRangeList(int addressBegin, int addressEnd, boolean personaNonGrata,boolean acceptingRange) {
		//assuming addresses are fine if not stated otherwise
		PNG=personaNonGrata;
		if(acceptingRange){
			ranges.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
		else{
			ranges.put(Integer.MIN_VALUE, addressBegin);
			ranges.put(addressEnd, Integer.MAX_VALUE);
			
		}
	}

	public void add(int addressBegin, int addressEnd, boolean personaNonGrata) {
		PNG=personaNonGrata;
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()<addressBegin){
			entry=ranges.higherEntry(entry.getValue());
			ranges.put(addressBegin, addressEnd);
		}
		else if(entry.getValue()<=addressEnd){
			ranges.put(entry.getKey(), addressEnd);
			entry=ranges.higherEntry(entry.getValue());
		}
		else{
			entry=ranges.higherEntry(entry.getValue());
		}
		//clean up extra ranges
		while(entry!=null&&entry.getValue()<addressEnd){
			ranges.remove(entry.getKey());
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry!=null&&ranges.floorEntry(addressBegin).getValue()>=entry.getKey()){
			ranges.remove(entry.getKey());
			ranges.put(ranges.floorEntry(addressBegin).getKey(), entry.getValue());
		}
	}

	public void subtract(int addressBegin, int addressEnd, boolean personaNonGrata) {
		PNG=personaNonGrata;
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()>addressEnd){// if placing range inside of an old range 
			ranges.put(addressEnd, entry.getValue());
			if(entry.getKey()==addressBegin){
				ranges.remove(entry.getKey());
			}
			else{
				ranges.put(entry.getKey(),addressBegin);
			}
		}
		else{
			entry=ranges.higherEntry(entry.getKey());
			while(entry.getValue()<addressEnd){
				ranges.remove(entry.getKey());
				entry=ranges.higherEntry(entry.getKey());
			}
			if(entry.getKey()<addressEnd){
				ranges.put(addressEnd, entry.getValue());
				ranges.remove(entry.getKey());
			}
		}
	}

	public boolean PNG() {
		return PNG;
	}

	public boolean contains(int source) {
		Entry<Integer, Integer> entry = ranges.firstEntry();
		//System.out.println(source);
		while(entry.getValue()<source){
			//System.out.println("entry "+Integer.toString(entry.getKey())+" "+Integer.toString(entry.getValue()));
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry.getKey()<source){
			return true;
		}
		return false;
	}
	public void print() {
		System.out.println("ranges");
		for(int k:ranges.keySet()){
			System.out.println(Integer.toString(k)+" - "+Integer.toString(ranges.get(k)));
		}
		
	}

}
class ParallelRangeList implements RangeList{
	private TreeMap<Integer,Integer> ranges = new TreeMap<Integer,Integer>();
	private volatile boolean PNG;
	private ReadWriteLock lock;

	public ParallelRangeList(int addressBegin, int addressEnd, boolean personaNonGrata,boolean acceptingRange) {
		//assuming addresses are fine if not stated otherwise
		PNG=personaNonGrata;
		if(acceptingRange){
			ranges.put(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
		else{
			ranges.put(Integer.MIN_VALUE, addressBegin);
			ranges.put(addressEnd, Integer.MAX_VALUE);
			
		}
		lock = new ReentrantReadWriteLock();
	}

	public void add(int addressBegin, int addressEnd, boolean personaNonGrata) {
		lock.writeLock().lock();
		PNG=personaNonGrata;
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()<addressBegin){
			entry=ranges.higherEntry(entry.getValue());
			ranges.put(addressBegin, addressEnd);
		}
		else if(entry.getValue()<=addressEnd){
			ranges.put(entry.getKey(), addressEnd);
			entry=ranges.higherEntry(entry.getValue());
		}
		else{
			entry=ranges.higherEntry(entry.getValue());
		}
		//clean up extra ranges
		while(entry!=null&&entry.getValue()<addressEnd){
			ranges.remove(entry.getKey());
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry!=null&&ranges.floorEntry(addressBegin).getValue()>=entry.getKey()){
			ranges.remove(entry.getKey());
			ranges.put(ranges.floorEntry(addressBegin).getKey(), entry.getValue());
		}
		lock.writeLock().unlock();
	}

	public void subtract(int addressBegin, int addressEnd, boolean personaNonGrata) {
		lock.writeLock().lock();
		PNG=personaNonGrata;
		Entry<Integer, Integer> entry = ranges.floorEntry(addressBegin);
		if(entry.getValue()>addressEnd){// if placing range inside of an old range 
			ranges.put(addressEnd, entry.getValue());
			if(entry.getKey()==addressBegin){
				ranges.remove(entry.getKey());
			}
			else{
				ranges.put(entry.getKey(),addressBegin);
			}
		}
		else{
			entry=ranges.higherEntry(entry.getKey());
			while(entry.getValue()<addressEnd){
				ranges.remove(entry.getKey());
				entry=ranges.higherEntry(entry.getKey());
			}
			if(entry.getKey()<addressEnd){
				ranges.put(addressEnd, entry.getValue());
				ranges.remove(entry.getKey());
			}
		}
		lock.writeLock().unlock();
	}

	public boolean PNG() {
		return PNG;
	}

	public boolean contains(int source) {
		lock.readLock().lock();
		Entry<Integer, Integer> entry = ranges.firstEntry();
		//System.out.println(source);
		while(entry.getValue()<source){
			//System.out.println("entry "+Integer.toString(entry.getKey())+" "+Integer.toString(entry.getValue()));
			entry=ranges.higherEntry(entry.getKey());
		}
		if(entry.getKey()<source){
			lock.readLock().unlock();
			return true;
		}
		lock.readLock().unlock();
		return false;
	}
	public void print() {
		System.out.println("ranges");
		for(int k:ranges.keySet()){
			System.out.println(Integer.toString(k)+" - "+Integer.toString(ranges.get(k)));
		}
		
	}

}