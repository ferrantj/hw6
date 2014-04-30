import org.deuce.Atomic;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public interface RangeLists{
	public void add(int addressBegin, int addressEnd, int addressEnd2, boolean personaNonGrata);
	public void subtract(int address, int addressBegin, int addressEnd,boolean personaNonGrata);
	public boolean check(int source, int dest);
}
class SerialRangeLists implements RangeLists{
	HashMap<Integer,RangeList> ranges = new HashMap<Integer,RangeList>();

	public void add(int address, int addressBegin, int addressEnd, boolean personaNonGrata) {
		if(ranges.containsKey(address)){
			ranges.get(address).add(addressBegin,addressEnd, personaNonGrata);
		}
		else{
			ranges.put(address,new SerialRangeList(addressBegin,addressEnd,personaNonGrata,true));
		}
	}

	public void subtract(int address, int addressBegin, int addressEnd,
			boolean personaNonGrata) {
		if(ranges.containsKey(address)){
			ranges.get(address).subtract(addressBegin,addressEnd, personaNonGrata);
		}
		else{
			ranges.put(address,new SerialRangeList(addressBegin,addressEnd,personaNonGrata,false));
		}
		
	}

	public boolean check(int source, int dest) {
		boolean x=ranges.containsKey(source);
		boolean y=ranges.containsKey(dest);
		if (x&&y){
			return !ranges.get(source).PNG()&&ranges.get(dest).contains(source);
		}
		//assuming safe if not yet configured
		else{
			if(y){
				return ranges.get(dest).contains(source);
			}
			if(x){
				return !ranges.get(source).PNG();
			}
			return true;
		}
	}

}
class STMRangeLists implements RangeLists{
	HashMap<Integer,RangeList> ranges = new HashMap<Integer,RangeList>();
	
	@Atomic
	public void add(int address, int addressBegin, int addressEnd, boolean personaNonGrata) {
		if(ranges.containsKey(address)){
			ranges.get(address).add(addressBegin,addressEnd, personaNonGrata);
		}
		else{
			ranges.put(address,new SerialRangeList(addressBegin,addressEnd,personaNonGrata,true));
		}
	}
	@Atomic
	public void subtract(int address, int addressBegin, int addressEnd,
			boolean personaNonGrata) {
		if(ranges.containsKey(address)){
			ranges.get(address).subtract(addressBegin,addressEnd, personaNonGrata);
		}
		else{
			ranges.put(address,new SerialRangeList(addressBegin,addressEnd,personaNonGrata,false));
		}
		
	}
	@Atomic
	public boolean check(int source, int dest) {
		boolean x=ranges.containsKey(source);
		boolean y=ranges.containsKey(dest);
		if (x&&y){
			return !ranges.get(source).PNG()&&ranges.get(dest).contains(source);
		}
		//assuming safe if not yet configured
		else{
			if(y){
				return ranges.get(dest).contains(source);
			}
			if(x){
				return !ranges.get(source).PNG();
			}
			return true;
		}
	}
}