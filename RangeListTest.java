
public class RangeListTest {
	public static void main(String[] args){
		RangeList list=new SerialRangeList(5,20,true,false);
		list.print();
		list.add(5, 20, true);
		list.print();
		
	}
}
