package com.lun.c13;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Atomic 变量自增运算测试
 * 
 */
public class AtomicTest {
	
	public static AtomicInteger race = new AtomicInteger(0);
	
	public static void increase() {
		race.incrementAndGet();
	}
	
	private static final int THREADS_COUNT = 20;
	
	public static void main(String[] args) {
		int a = 1 + 2;
		
		Thread[] threads = new Thread[THREADS_COUNT];
		for (int i = 0; i < THREADS_COUNT; i ++) {
			threads[i] = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 10000; i++) {
						increase();
					}
				}
			});
			threads[i].start();
		}
		
		// 等待所有累加线程都结束
		while (Thread.activeCount() > 1) 
			Thread.yield();
		
		System.out.println(race);
	}
}
