package com.lun.c10;

import java.util.Arrays;
import java.util.List;

public class AutoboxingAndForeach {

	public static void main(String[] args) {
		
		List<Integer> list = Arrays.asList(1, 2, 3, 4);
		// 如果在 JDK 1.8 中，还有另外一颗语法糖
		// 能让上面这句代码进一步简写成 List<Integer> list = [1, 2, 3, 4];
		int sum = 0;
		for (int i : list) {
			sum += i;
		}
		
		System.out.println(sum);
		
	}

}
