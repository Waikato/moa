package moa.classifiers.active.budget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class is used to rank an instance.
 * 
 * @author Tuan Pham Minh (tuan.pham@ovgu.de)
 * @version $Revision: 1 $
 */
public class Ranking<T extends Comparable<T>> {
	private class IndexComparator implements Comparator<Integer>
	{
		List<T> data;
		
		public IndexComparator(List<T> data) {
			this.data = data;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			return data.get(o1).compareTo(data.get(o2));
		}
	}
	

	public int rank(List<T> data, int index)
	{
		Comparator<Integer> comparator = new IndexComparator(data);
		List<Integer> indexList = generateIndexList(data.size());
		indexList.sort(comparator);
		return indexList.indexOf(index);
	}
	
	private List<Integer> generateIndexList(int numData)
	{
		List<Integer> list = new ArrayList<>();
		for(int i = 0; i < numData; ++i)
		{
			list.add(i);
		}
		
		return list;
	}
}
