package com.lun.c09;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TestLast {
	
	public static void main(String[] args) throws IOException {
		InputStream is = new FileInputStream("C:\\eclipse-workspace\\LeetCode\\target\\classes\\com\\lun\\other\\jvm\\c09\\HelloWorld.class");
		try {
			byte[] b = new byte[is.available()];
			is.read(b);
			System.out.println(JavaClassExecuter.execute(b));
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			is.close();
		}
	}
	
}
