package com.lun.c13;

import java.util.Vector;

public class VectorThreadSafe {
	
	private static Vector<Integer> vector = new Vector<Integer>();

	private static volatile boolean flag = true;
	
	public static void main(String[] args) {

		while (flag) {
			for (int i = 0; i < 10; i++) {
				vector.add(i);
			}
			
			Thread removeThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for (int i = 0; i < vector.size(); i++) {
							vector.remove(i);
						}
					}catch(Exception e) {
						e.printStackTrace();
						flag = false;
					}
				}
			});
			
			Thread printThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						for (int i = 0; i < vector.size(); i++) {
							System.out.println(vector.get(i));
						}
					}catch(Exception e) {
						e.printStackTrace();
						flag = false;
					}
				}
			});
			
			removeThread.start();
			printThread.start();
			
			// 不要同时产生过多的线程，否则会导致操作系统假死
			while (Thread.activeCount() > 20);
		}
	}
}
