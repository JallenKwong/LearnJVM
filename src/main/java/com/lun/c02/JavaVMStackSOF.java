package com.lun.c02;

/**
 * VM Args：-Xss128k
 * @author zzm
 */
public class JavaVMStackSOF {
	
	private int stackLength = 1;

	public void stackLeak() {
		stackLength++;
		stackLeak();
	}

	public static void main(String[] args) throws Throwable {
		JavaVMStackSOF oom = new JavaVMStackSOF();
		try {
			oom.stackLeak();
		} catch (Throwable e) {
			System.out.println("stack length:" + oom.stackLength);
			throw e;
		}
	}
}

/*
stack length:999
Exception in thread "main" java.lang.StackOverflowError
	at com.lun.c02.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:12)
	at com.lun.c02.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
	at com.lun.c02.JavaVMStackSOF.stackLeak(JavaVMStackSOF.java:13)
...
*/

