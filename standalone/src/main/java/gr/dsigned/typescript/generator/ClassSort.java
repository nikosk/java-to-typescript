package gr.dsigned.typescript.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * gr.dsigned.typescript.generator
 * User: nk
 * Date: 2016-10-04 09:56
 */
public class ClassSort {


	public static List<Class<?>> sort(List<Class<?>> classes) {
		final ArrayList<Class<?>> sorted = new ArrayList<>();
		for (Class<?> cls : classes) {
			final int i = indexOfLastSuperClass(cls, sorted);
			sorted.add(i + 1, cls);
		}
		return sorted;
	}

	private static int indexOfLastSuperClass(Class<?> clz, List<Class<?>> classes) {
		int index = -1;
		for (int i = 0; i < classes.size(); i++) {
			if (classes.get(i).isAssignableFrom(clz)) {
				index = i;
			}
		}
		return index;
	}

}
