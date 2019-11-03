package com.lun.c08;

import static java.lang.invoke.MethodHandles.lookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class SuperSuper {

	class GrandFather {
		void thinking() {
			System.out.println("i am grandfather");
		}
	}

	class Father extends GrandFather {
		void thinking() {
			System.out.println("i am father");
		}
	}

	class Son extends Father {
		void thinking() {
			try {
				MethodType mt = MethodType.methodType(void.class);
				MethodHandle mh = lookup().findSpecial(GrandFather.class, "thinking", mt, getClass());
				mh.invoke(this);
				//输出i am father，没有达到书本结果

				//System.out.println(getClass().getSuperclass().getSuperclass());
				new GrandFather().thinking();
				
			} catch (Throwable e) {
			}
		}
	}

	public static void main(String[] args) {
		(new SuperSuper().new Son()).thinking();
	}

}
