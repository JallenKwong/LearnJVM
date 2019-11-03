package com.lun.c02;

import java.util.ArrayList;
import java.util.List;

/**
 * VM Args：-Xms20m -Xmx20m -XX:+HeapDumpOnOutOfMemoryError
 * @author zzm
 */
public class HeapOOM {
	
	static class OOMObject {
	}

	public static void main(String[] args) {
		List<OOMObject> list = new ArrayList<OOMObject>();

		while (true) {
			list.add(new OOMObject());
		}
	}
	
}

/*result:

[GC (Allocation Failure) [PSYoungGen: 7793K->1001K(9216K)] 7793K->5334K(19456K), 0.1073292 secs] [Times: user=0.09 sys=0.00, real=0.11 secs] 
[GC (Allocation Failure) [PSYoungGen: 9193K->1024K(9216K)] 13526K->11194K(19456K), 0.0330039 secs] [Times: user=0.03 sys=0.00, real=0.03 secs] 
[Full GC (Ergonomics) [PSYoungGen: 1024K->1023K(9216K)] [ParOldGen: 10170K->10140K(10240K)] 11194K->11163K(19456K), [Metaspace: 2790K->2790K(1056768K)], 0.4100364 secs] [Times: user=0.44 sys=0.00, real=0.41 secs] 
[Full GC (Ergonomics) [PSYoungGen: 8551K->8424K(9216K)] [ParOldGen: 10140K->8028K(10240K)] 18692K->16452K(19456K), [Metaspace: 2790K->2790K(1056768K)], 0.3584301 secs] [Times: user=0.39 sys=0.00, real=0.36 secs] 
[Full GC (Allocation Failure) [PSYoungGen: 8424K->8423K(9216K)] [ParOldGen: 8028K->8016K(10240K)] 16452K->16440K(19456K), [Metaspace: 2790K->2790K(1056768K)], 0.2883919 secs] [Times: user=0.30 sys=0.02, real=0.29 secs] 
java.lang.OutOfMemoryError: Java heap space
Dumping heap to java_pid5784.hprof ...
Heap dump file created [28135555 bytes in 0.213 secs]
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
	at java.util.Arrays.copyOf(Arrays.java:3210)
	at java.util.Arrays.copyOf(Arrays.java:3181)
	at java.util.ArrayList.grow(ArrayList.java:265)
	at java.util.ArrayList.ensureExplicitCapacity(ArrayList.java:239)
	at java.util.ArrayList.ensureCapacityInternal(ArrayList.java:231)
	at java.util.ArrayList.add(ArrayList.java:462)
	at com.lun.c02.HeapOOM.main(HeapOOM.java:19)
Heap
 PSYoungGen      total 9216K, used 8703K [0x00000000ff600000, 0x0000000100000000, 0x0000000100000000)
  eden space 8192K, 100% used [0x00000000ff600000,0x00000000ffe00000,0x00000000ffe00000)
  from space 1024K, 49% used [0x00000000fff00000,0x00000000fff7fd40,0x0000000100000000)
  to   space 1024K, 0% used [0x00000000ffe00000,0x00000000ffe00000,0x00000000fff00000)
 ParOldGen       total 10240K, used 8016K [0x00000000fec00000, 0x00000000ff600000, 0x00000000ff600000)
  object space 10240K, 78% used [0x00000000fec00000,0x00000000ff3d4398,0x00000000ff600000)
 Metaspace       used 2822K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 302K, capacity 386K, committed 512K, reserved 1048576K

*/

