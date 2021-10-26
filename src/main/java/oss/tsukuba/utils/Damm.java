package oss.tsukuba.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

public class Damm {

	private final int KEY_SIZE = 31;
	
	private final int BASE = 32;
	
	private final char[] BASE32 = "2345679abcdefghijkmnopqrstuvwxyz".toCharArray();
	
	private SecureRandom sr;
	
	private char[][] damm32Matrix = {
	        { 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 3, 1, 7, 5, 11, 9, 15, 13, 19, 17, 23, 21, 27, 25, 31, 29},
	        { 2, 0, 6, 4, 10, 8, 14, 12, 18, 16, 22, 20, 26, 24, 30, 28, 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31},
	        { 4, 6, 0, 2, 12, 14, 8, 10, 20, 22, 16, 18, 28, 30, 24, 26, 7, 5, 3, 1, 15, 13, 11, 9, 23, 21, 19, 17, 31, 29, 27, 25},
	        { 6, 4, 2, 0, 14, 12, 10, 8, 22, 20, 18, 16, 30, 28, 26, 24, 5, 7, 1, 3, 13, 15, 9, 11, 21, 23, 17, 19, 29, 31, 25, 27},
	        { 8, 10, 12, 14, 0, 2, 4, 6, 24, 26, 28, 30, 16, 18, 20, 22, 11, 9, 15, 13, 3, 1, 7, 5, 27, 25, 31, 29, 19, 17, 23, 21},
	        {10, 8, 14, 12, 2, 0, 6, 4, 26, 24, 30, 28, 18, 16, 22, 20, 9, 11, 13, 15, 1, 3, 5, 7, 25, 27, 29, 31, 17, 19, 21, 23},
	        {12, 14, 8, 10, 4, 6, 0, 2, 28, 30, 24, 26, 20, 22, 16, 18, 15, 13, 11, 9, 7, 5, 3, 1, 31, 29, 27, 25, 23, 21, 19, 17},
	        {14, 12, 10, 8, 6, 4, 2, 0, 30, 28, 26, 24, 22, 20, 18, 16, 13, 15, 9, 11, 5, 7, 1, 3, 29, 31, 25, 27, 21, 23, 17, 19},
	        {16, 18, 20, 22, 24, 26, 28, 30, 0, 2, 4, 6, 8, 10, 12, 14, 19, 17, 23, 21, 27, 25, 31, 29, 3, 1, 7, 5, 11, 9, 15, 13},
	        {18, 16, 22, 20, 26, 24, 30, 28, 2, 0, 6, 4, 10, 8, 14, 12, 17, 19, 21, 23, 25, 27, 29, 31, 1, 3, 5, 7, 9, 11, 13, 15},
	        {20, 22, 16, 18, 28, 30, 24, 26, 4, 6, 0, 2, 12, 14, 8, 10, 23, 21, 19, 17, 31, 29, 27, 25, 7, 5, 3, 1, 15, 13, 11, 9},
	        {22, 20, 18, 16, 30, 28, 26, 24, 6, 4, 2, 0, 14, 12, 10, 8, 21, 23, 17, 19, 29, 31, 25, 27, 5, 7, 1, 3, 13, 15, 9, 11},
	        {24, 26, 28, 30, 16, 18, 20, 22, 8, 10, 12, 14, 0, 2, 4, 6, 27, 25, 31, 29, 19, 17, 23, 21, 11, 9, 15, 13, 3, 1, 7, 5},
	        {26, 24, 30, 28, 18, 16, 22, 20, 10, 8, 14, 12, 2, 0, 6, 4, 25, 27, 29, 31, 17, 19, 21, 23, 9, 11, 13, 15, 1, 3, 5, 7},
	        {28, 30, 24, 26, 20, 22, 16, 18, 12, 14, 8, 10, 4, 6, 0, 2, 31, 29, 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1},
	        {30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0, 29, 31, 25, 27, 21, 23, 17, 19, 13, 15, 9, 11, 5, 7, 1, 3},
	        { 3, 1, 7, 5, 11, 9, 15, 13, 19, 17, 23, 21, 27, 25, 31, 29, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30},
	        { 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 2, 0, 6, 4, 10, 8, 14, 12, 18, 16, 22, 20, 26, 24, 30, 28},
	        { 7, 5, 3, 1, 15, 13, 11, 9, 23, 21, 19, 17, 31, 29, 27, 25, 4, 6, 0, 2, 12, 14, 8, 10, 20, 22, 16, 18, 28, 30, 24, 26},
	        { 5, 7, 1, 3, 13, 15, 9, 11, 21, 23, 17, 19, 29, 31, 25, 27, 6, 4, 2, 0, 14, 12, 10, 8, 22, 20, 18, 16, 30, 28, 26, 24},
	        {11, 9, 15, 13, 3, 1, 7, 5, 27, 25, 31, 29, 19, 17, 23, 21, 8, 10, 12, 14, 0, 2, 4, 6, 24, 26, 28, 30, 16, 18, 20, 22},
	        { 9, 11, 13, 15, 1, 3, 5, 7, 25, 27, 29, 31, 17, 19, 21, 23, 10, 8, 14, 12, 2, 0, 6, 4, 26, 24, 30, 28, 18, 16, 22, 20},
	        {15, 13, 11, 9, 7, 5, 3, 1, 31, 29, 27, 25, 23, 21, 19, 17, 12, 14, 8, 10, 4, 6, 0, 2, 28, 30, 24, 26, 20, 22, 16, 18},
	        {13, 15, 9, 11, 5, 7, 1, 3, 29, 31, 25, 27, 21, 23, 17, 19, 14, 12, 10, 8, 6, 4, 2, 0, 30, 28, 26, 24, 22, 20, 18, 16},
	        {19, 17, 23, 21, 27, 25, 31, 29, 3, 1, 7, 5, 11, 9, 15, 13, 16, 18, 20, 22, 24, 26, 28, 30, 0, 2, 4, 6, 8, 10, 12, 14},
	        {17, 19, 21, 23, 25, 27, 29, 31, 1, 3, 5, 7, 9, 11, 13, 15, 18, 16, 22, 20, 26, 24, 30, 28, 2, 0, 6, 4, 10, 8, 14, 12},
	        {23, 21, 19, 17, 31, 29, 27, 25, 7, 5, 3, 1, 15, 13, 11, 9, 20, 22, 16, 18, 28, 30, 24, 26, 4, 6, 0, 2, 12, 14, 8, 10},
	        {21, 23, 17, 19, 29, 31, 25, 27, 5, 7, 1, 3, 13, 15, 9, 11, 22, 20, 18, 16, 30, 28, 26, 24, 6, 4, 2, 0, 14, 12, 10, 8},
	        {27, 25, 31, 29, 19, 17, 23, 21, 11, 9, 15, 13, 3, 1, 7, 5, 24, 26, 28, 30, 16, 18, 20, 22, 8, 10, 12, 14, 0, 2, 4, 6},
	        {25, 27, 29, 31, 17, 19, 21, 23, 9, 11, 13, 15, 1, 3, 5, 7, 26, 24, 30, 28, 18, 16, 22, 20, 10, 8, 14, 12, 2, 0, 6, 4},
	        {31, 29, 27, 25, 23, 21, 19, 17, 15, 13, 11, 9, 7, 5, 3, 1, 28, 30, 24, 26, 20, 22, 16, 18, 12, 14, 8, 10, 4, 6, 0, 2},
	        {29, 31, 25, 27, 21, 23, 17, 19, 13, 15, 9, 11, 5, 7, 1, 3, 30, 28, 26, 24, 22, 20, 18, 16, 14, 12, 10, 8, 6, 4, 2, 0}
	    };
	
	public Damm() {
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			LogUtils.error(e.toString(), e);
		}
	}
	
	public String getPassphrase() {
		StringBuffer sb = new StringBuffer();

		while (sb.length() < KEY_SIZE) {
			int pos = sr.nextInt(BASE);
			sb.append(BASE32[pos]);
		}
		
		return sb.toString();
	}
	
	private int base32Index(char p) {
		int i;
	    
		for (i = 0; i < BASE; i++) {
			if (p == BASE32[i]) {
				break;
	        }
	    }
	    
		return i;
	}
	
	public char damm32Encode(char[] code) {
		int i, interim = 0;

		for (char p: code) {
			i = base32Index(p);
	        
			if (i == BASE) {
				return '-';
	        }
	        
			interim = damm32Matrix[interim][i];
	        
	    }
	    
		return BASE32[interim];
	}
	
	public boolean damm32Check(char[] code) {
		return damm32Encode(code) == BASE32[0];
	}
}
