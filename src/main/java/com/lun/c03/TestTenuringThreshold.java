package com.lun.c03;

public class TestTenuringThreshold {

	private static final int _1MB = 1024 * 1024;

	/**
	 * VM参数：-verbose:gc -Xms20M -Xmx20M -Xmn10M -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1
	 * -XX:+PrintTenuringDistribution
	 */
	@SuppressWarnings("unused")
	public static void testTenuringThreshold() {
		byte[] allocation1, allocation2, allocation3;
		allocation1 = new byte[_1MB / 4];  // 什么时候进入老年代决定于XX:MaxTenuringThreshold设置
		allocation2 = new byte[4 * _1MB];
		allocation3 = new byte[4 * _1MB];
		allocation3 = null;
		allocation3 = new byte[4 * _1MB];
	}

	
	public static void main(String[] args) {
		testTenuringThreshold();
	}

}

/* -XX:MaxTenuringThreshold=1
 
  
[GC (Allocation Failure) [DefNew
Desired survivor size 524288 bytes, new threshold 1 (max 1)
- age   1:     911032 bytes,     911032 total
: 5503K->889K(9216K), 0.0127713 secs] 5503K->4985K(19456K), 0.0128544 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
[GC (Allocation Failure) [DefNew
Desired survivor size 524288 bytes, new threshold 1 (max 1)
: 4985K->0K(9216K), 0.0025695 secs] 9081K->4984K(19456K), 0.0026409 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
Heap
 def new generation   total 9216K, used 4178K [0x00000000fec00000, 0x00000000ff600000, 0x00000000ff600000)
  eden space 8192K,  51% used [0x00000000fec00000, 0x00000000ff014930, 0x00000000ff400000)
  from space 1024K,   0% used [0x00000000ff400000, 0x00000000ff400000, 0x00000000ff500000)
  to   space 1024K,   0% used [0x00000000ff500000, 0x00000000ff500000, 0x00000000ff600000)
 tenured generation   total 10240K, used 4984K [0x00000000ff600000, 0x0000000100000000, 0x0000000100000000)
   the space 10240K,  48% used [0x00000000ff600000, 0x00000000ffade188, 0x00000000ffade200, 0x0000000100000000)
 Metaspace       used 2791K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 298K, capacity 386K, committed 512K, reserved 1048576K
*/


/* -XX:MaxTenuringThreshold=15 //这与书上的不同

[GC (Allocation Failure) [DefNew
Desired survivor size 524288 bytes, new threshold 1 (max 15)
- age   1:     911032 bytes,     911032 total
: 5503K->889K(9216K), 0.0067026 secs] 5503K->4985K(19456K), 0.0067924 secs] [Times: user=0.00 sys=0.00, real=0.01 secs] 
[GC (Allocation Failure) [DefNew
Desired survivor size 524288 bytes, new threshold 15 (max 15)
: 4985K->0K(9216K), 0.0032538 secs] 9081K->4984K(19456K), 0.0032959 secs] [Times: user=0.00 sys=0.00, real=0.00 secs] 
Heap
 def new generation   total 9216K, used 4178K [0x00000000fec00000, 0x00000000ff600000, 0x00000000ff600000)
  eden space 8192K,  51% used [0x00000000fec00000, 0x00000000ff014930, 0x00000000ff400000)
  from space 1024K,   0% used [0x00000000ff400000, 0x00000000ff400000, 0x00000000ff500000)
  to   space 1024K,   0% used [0x00000000ff500000, 0x00000000ff500000, 0x00000000ff600000)
 tenured generation   total 10240K, used 4984K [0x00000000ff600000, 0x0000000100000000, 0x0000000100000000)
   the space 10240K,  48% used [0x00000000ff600000, 0x00000000ffade188, 0x00000000ffade200, 0x0000000100000000)
 Metaspace       used 2791K, capacity 4486K, committed 4864K, reserved 1056768K
  class space    used 298K, capacity 386K, committed 512K, reserved 1048576K

*/
