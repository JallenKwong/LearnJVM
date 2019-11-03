package com.lun.c02;

import java.util.ArrayList;
import java.util.List;

/**
 * VM Args：-XX:PermSize=10M -XX:MaxPermSize=10M
 * JDK1.8 : -XX:MetaspaceSize=10M -XX:MaxMetaspaceSize=10M
 * @author zzm
 */
public class RuntimeConstantPoolOOM {
	public static void main(String[] args) {
		// 使用List保持着常量池引用，避免Full GC回收常量池行为
		List<String> list = new ArrayList<String>();
		
		// 10MB的PermSize在integer范围内足够产生OOM了
		int i = 0; 
		while (true) {
			list.add(String.valueOf(i++).intern());
		}
	}
}

/*
Error occurred during initialization of VM
OutOfMemoryError: Metaspace
*/


/* 设置类-Xms20M -Xmx20M
 Exception in thread "main" java.lang.OutOfMemoryError: GC overhead limit exceeded
	at java.lang.Long.toString(Long.java:397)
	at java.lang.String.valueOf(String.java:3113)
	at com.lun.c02.RuntimeConstantPoolOOM.main(RuntimeConstantPoolOOM.java:20)
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option PermSize=1k; support was removed in 8.0
Java HotSpot(TM) 64-Bit Server VM warning: ignoring option MaxPermSize=1k; support was removed in 8.0

*/



