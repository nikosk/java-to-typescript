package gr.dsigned.typescript.generator;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

/**
 * gr.dsigned.typescript.generator
 * User: nk
 * Date: 2016-10-04 09:58
 */
public class ClassSortTest {
	@Test
	public void compare() throws Exception {
		List<Class<?>> list = new ArrayList<>();
		list.add(A.class);
		list.add(C.class);
		list.add(D.class);
		list.add(B.class);
		list = ClassSort.sort(list);
		System.out.println("1) " + list.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
		assertThat(list.get(0), is(equalTo(A.class)));
		assertThat(list.get(1), is(equalTo(B.class)));
		assertThat(list.get(2), is(equalTo(D.class)));
		assertThat(list.get(3), is(equalTo(C.class)));
	}

	@Test
	public void compareR() throws Exception {
		final ArrayList<Class<?>> cls = new ArrayList<>();
		cls.add(A.class);
		cls.add(B.class);
		cls.add(C.class);
		cls.add(D.class);
		for(int i =0; i < 100; i++) {
			List<Class<?>> list = new ArrayList<>();
			Collections.shuffle(cls);
			list.add(cls.get(0));
			list.add(cls.get(1));
			list.add(cls.get(2));
			list.add(cls.get(3));
			System.out.println("1) " + list.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
			list = ClassSort.sort(list);
			System.out.println("2) " + list.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
			assertThat(list.indexOf(A.class), is(lessThan(list.indexOf(B.class))));
			assertThat(list.indexOf(A.class), is(lessThan(list.indexOf(C.class))));
			assertThat(list.indexOf(A.class), is(lessThan(list.indexOf(D.class))));

			assertThat(list.indexOf(B.class), is(lessThan(list.indexOf(C.class))));

			assertThat(list.indexOf(D.class), is(greaterThan(list.indexOf(A.class))));

		}
	}

	static class A {
	}

	static class B extends A {
	}

	public static class C extends B {
	}

	public static class D extends A {
	}

}
