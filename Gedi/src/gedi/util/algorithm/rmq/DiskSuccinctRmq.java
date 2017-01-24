/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.util.algorithm.rmq;

import java.io.IOException;

import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.PageFileWriter;

public class DiskSuccinctRmq {

	
	private PageFileView file;
	
	// size of array a
	int n;
	// microblock size
	int s;
	// block size
	int sprime;
	// superblock size
	int sprimeprime;
	// depth of table M:
	int M_depth;
	// depth of table M':
	int Mprime_depth;

	
	// array: any type
	long ap;
	// table M for the out-of-block queries (contains indices of block-minima)
	long Mp; // byte[M_depth][nb]
	// table M' for superblock-queries (contains indices of block-minima)
	long Mprimep;//int[Mprime_depth][nsb]
	// type of blocks
	long typep;//short[nmb]
	// precomputed in-block queries
	long Precp;//byte[Catalan[s][s]][s]


	
	// number of blocks (always n/sprime)
	int nb;
	// number of superblocks (always n/sprimeprime)
	int nsb;
	// number of microblocks (always n/s)
	int nmb;

	// because M just stores offsets (rel. to start of block), this method
	// re-calculates the true index:
	private final int m(int k, int block) throws IOException { return getM(k,block)+(block*sprime); }

	// return microblock-number of entry i:
	private final int microblock(int i) { return i/s; }

	// return block-number of entry i:
	private final  int block(int i) { return i/sprime; }

	// return superblock-number of entry i:
	private final  int superblock(int i) { return i/sprimeprime; }

	// precomputed Catalan triangle (17 is enough for 64bit computing):
	static final int[][] Catalan ={
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16},
		{0,0,2,5,9,14,20,27,35,44,54,65,77,90,104,119,135},
		{0,0,0,5,14,28,48,75,110,154,208,273,350,440,544,663,798},
		{0,0,0,0,14,42,90,165,275,429,637,910,1260,1700,2244,2907,3705},
		{0,0,0,0,0,42,132,297,572,1001,1638,2548,3808,5508,7752,10659,14364},
		{0,0,0,0,0,0,132,429,1001,2002,3640,6188,9996,15504,23256,33915,48279},
		{0,0,0,0,0,0,0,429,1430,3432,7072,13260,23256,38760,62016,95931,144210},
		{0,0,0,0,0,0,0,0,1430,4862,11934,25194,48450,87210,149226,245157,389367},
		{0,0,0,0,0,0,0,0,0,4862,16796,41990,90440,177650,326876,572033,961400},
		{0,0,0,0,0,0,0,0,0,0,16796,58786,149226,326876,653752,1225785,2187185},
		{0,0,0,0,0,0,0,0,0,0,0,58786,208012,534888,1188640,2414425,4601610},
		{0,0,0,0,0,0,0,0,0,0,0,0,208012,742900,1931540,4345965,8947575},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,742900,2674440,7020405,15967980},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,2674440,9694845,25662825},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9694845,35357670},
		{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,35357670}
	};;

	// minus infinity (change for 64bit version)
	static final int minus_infinity = Integer.MAX_VALUE;

 	// stuff for clearing the least significant x bits (change for 64-bit computing)
	static final byte[] HighestBitsSet = {~0, ~1, ~3, ~7, ~15, ~31, ~63, ~127};

	// Least Significant Bits for 8-bit-numbers:
	static final byte[] LSBTable256 = {
		0,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		7,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
		4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0
	};

	private final int log2fast(int v) {
		int c = 0;          // c will be lg(v)
		int tt,t;
		if ((tt = v >> 16)!=0)
			c = (t = v >> 24)!=0 ? 24 + LogTable256[t] : 16 + LogTable256[tt & 0xFF];
		else 
			c = (t = v >> 8)!=0 ? 8 + LogTable256[t] : LogTable256[v];
		return c;
	}
	
	// the following stuff is for fast base 2 logarithms:
	// (currently only implemented for 32 bit numbers)
	static final byte[] LogTable256 = {
		0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
		4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
		5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
		5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
		7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
	};;
    private final byte clearbits(byte n, int x) {
		return (byte) (n & HighestBitsSet[x]);
	}

	private boolean ARRAY_VERY_SMALL;
    
    
    public static void create(PageFileWriter writer, int[] a) throws IOException {
    	
    	int n = a.length;
    	int s = 1 << 3;	         // microblock-size
    	int sprime = 1 << 4;         // block-size
    	int sprimeprime = 1 << 8;	 // superblock-size
    	
    	int nmb = (n-1)/s+1; // number of microblocks
    	int nb = (n-1)/sprime+1;       // number of blocks
    	int nsb = (n-1)/sprimeprime+1; // number of superblocks
    	
    	
    	int M_depth = (int) Math.floor(Math.log(((double) sprimeprime / (double) sprime))/Math.log(2));
    	int Mprime_depth = (int) Math.floor(Math.log(nsb)/Math.log(2)) + 1;
    	
    	
    	// write header: MAGIX(RmaxQ),n, s,sprime,sprimeprime,M_depth,Mprime_depth
    	writer.putAsciiChars("RmaxQ");
    	writer.putInt(n);
    	writer.putInt(s);
    	writer.putInt(sprime);
    	writer.putInt(sprimeprime);
    	writer.putInt(M_depth);
    	writer.putInt(Mprime_depth);
    	
    	
    	// spill out the array
		for (int i:a)
			writer.putInt(i);
		
    	
    	// The following is necessary because we've fixed s, s' and s'' according to the computer's
    	// word size and NOT according to the input size. This may cause the (super-)block-size
    	// to be too big, or, in other words, the array too small. If this code is compiled on
    	// a 32-bit computer, this happens iff n < 113. For such small instances it isn't 
    	// advisable anyway to use this data structure, because simpler methods are faster and 
    	// less space consuming.
    	if (nb<sprimeprime/(2*sprime)) {
    		// naive scanning is used, so no extra space needed
    		return; 
    	}

    	// space for out-of-block- and out-of-superblock-queries:
    	byte[][] M = new byte[M_depth][];
    	M[0] = new byte[nb];
    	int[][] Mprime = new int[Mprime_depth][];
    	Mprime[0] = new int[nsb];
    	
    	// Type-calculation for the microblocks and pre-computation of in-microblock-queries:
    	short[] type = new short[nmb];
    	byte[][] Prec = new byte[Catalan[s][s]][];
    	for (int i = 0; i < Catalan[s][s]; i++) {
    		Prec[i] = new byte[s];
    		Prec[i][0] = 1; // init with impossible value
    	}

    	int[] rp = new int[s+1];   // rp: rightmost path in Cart. tree
    	int z = 0;            // index in array a
    	int start;            // start of current block
    	int end;              // end of current block
    	int q;                // position in Catalan triangle
    	int p;                // --------- " ----------------
    	rp[0] = minus_infinity; // stopper (minus infinity)

    	// prec[i]: the jth bit is 1 iff j is 1. pos. to the left of i where a[j] < a[i] 
    	int[] gstack = new int[s];
    	int gstacksize;
    	int g; // first position to the left of i where a[g[i]] < a[i]

    	for (int i = 0; i < nmb; i++) { // step through microblocks
    		start = z;            // init start
    		end = start + s;      // end of block (not inclusive!)
    		if (end > n) end = n; // last block could be smaller than s!

    		// compute block type as in Fischer/Heun CPM'06:
    		q = s;        // init q
    		p = s-1;      // init p
    		type[i] = 0;  // init type (will be increased!)
    		rp[1] = a[z]; // init rightmost path

    		while (++z < end) {   // step through current block:
    			p--;
    			while (rp[q-p-1] < a[z]) {
    				type[i] += Catalan[p][q]; // update type
    				q--;
    			}
    			rp[q-p] = a[z]; // add last element to rightmost path
    		}

    		// precompute in-block-queries for this microblock (if necessary)
    		// as in Alstrup et al. SPAA'02:
    		if (Prec[type[i]][0] == 1) {
    			Prec[type[i]][0] = 0;
    			gstacksize = 0;
    			for (int j = start; j < end; j++) {
    				while(gstacksize > 0 && (a[j] > a[gstack[gstacksize-1]])) {
    					gstacksize--;
    				}
    				if(gstacksize > 0) {
    					g = gstack[gstacksize-1];
    					Prec[type[i]][j-start] = (byte) (Prec[type[i]][g-start] | (1 << (g % s)));
    				}
    				else Prec[type[i]][j-start] = 0;
    				gstack[gstacksize++] = j;
    			}
    		}
    	}

    	

    	// fill 0'th rows of M and Mprime:
    	z = 0; // minimum in current block
    	q = 0; // pos. of min in current superblock
    	g = 0; // number of current superblock
    	for (int i = 0; i < nb; i++) { // step through blocks
    		start = z;              // init start
    		p = start;              // init minimum
    		end = start + sprime;   // end of block (not inclusive!)
    		if (end > n) end = n;   // last block could be smaller than sprime!
    		if (a[z] > a[q]) q = z; // update minimum in superblock

    		while (++z < end) { // step through current block:
    			if (a[z] > a[p]) p = z; // update minimum in block
    			if (a[z] > a[q]) q = z; // update minimum in superblock
    		}
    		M[0][i] = (byte) (p-start);                     // store index of block-minimum (offset!)
    		if (z % sprimeprime == 0 || z == n) {  // reached end of superblock?
    			Mprime[0][g++] = q;               // store index of superblock-minimum
    			q = z;
    		}
    	}

    	// fill M:
    	int dist = 1; // always 2^(j-1)
    	for (int j = 1; j < M_depth; j++) {
    		M[j] = new byte[nb];
    		for (int i = 0; i < nb - dist; i++) { // be careful: loop may go too far
    			int m1 = M[j-1][i]+(i*sprime);
    			int m2 = M[j-1][i+dist]+(i+dist)*sprime;
    			
    			M[j][i] = (byte) (a[m1] >= a[m2] ?
    				M[j-1][i] : M[j-1][i+dist] + (dist*sprime)); // add 'skipped' elements in a
    		}
    		for (int i = nb - dist; i < nb; i++) M[j][i] = M[j-1][i]; // fill overhang
    		dist *= 2;
    	}
    	
    	// fill M':
    	dist = 1; // always 2^(j-1)
    	for (int j = 1; j < Mprime_depth; j++) {
    		Mprime[j] = new int[nsb];
    		for (int i = 0; i < nsb - dist; i++) {
    			Mprime[j][i] = a[Mprime[j-1][i]] >= a[Mprime[j-1][i+dist]] ?
    				Mprime[j-1][i] : Mprime[j-1][i+dist];
    		}
    		for (int i = nsb - dist; i < nsb; i++) Mprime[j][i] = Mprime[j-1][i]; // overhang
    		dist *= 2;
    	}
    	
    	
    	// write everything
    	// first M
    	for (int i=0; i<M_depth; i++)
    		for (int j=0; j<nb; j++)
   				writer.put(M[i][j]);
    	// now Mprime
    	for (int i=0; i<Mprime_depth; i++)
    		for (int j=0; j<nsb; j++)
    			writer.putInt(Mprime[i][j]);
    	// type
    	for (int i=0; i<nmb; i++)
    		writer.putShort(type[i]);
    	// finally, Prec
    	for (int i=0; i<Catalan[s][s]; i++)
    		for (int j=0; j<s; j++)
    			writer.putByte(Prec[i][j]);
	}
    
    public DiskSuccinctRmq(String file) throws IOException {
		this(new PageFileView(new PageFile(file)));
	}
    public DiskSuccinctRmq(PageFileView file) throws IOException {
  		this.file = file;
  		
  		file.position(0);
  		if (!file.getAsciiChars(5).equals("RmaxQ")) throw new IOException("Illegal file!");
  		
  		this.n = file.getInt();
  		this.s = file.getInt();
  		this.sprime = file.getInt();
  		this.sprimeprime = file.getInt();
  		this.M_depth = file.getInt();;
  		this.Mprime_depth = file.getInt();
  		
  	  	nmb = (n-1)/s+1; // number of microblocks
    	nb = (n-1)/sprime+1;       // number of blocks
    	nsb = (n-1)/sprimeprime+1; // number of superblocks
 
  		this.ap = file.position();
  		this.Mp = this.ap+n*Integer.BYTES;
  		this.Mprimep = this.Mp + Byte.BYTES*M_depth*nb;
  		this.typep = this.Mprimep + Integer.BYTES*Mprime_depth*nsb;
  		this.Precp = this.typep + Short.BYTES*nmb;

  		if (nb<sprimeprime/(2*sprime)) {
    		ARRAY_VERY_SMALL = true;
    		return; 
    	}

  		
  	}
      
    private final int geta(int i) throws IOException {
    	return file.getInt(ap+Integer.BYTES*i);
    }
    private final byte getM(int i, int j) throws IOException {
    	return file.get(Mp+Byte.BYTES*(i*nb+j));
    }
    private final int getMprime(int i, int j) throws IOException {
    	return file.getInt(Mprimep+Integer.BYTES*(i*nsb+j));
    }
    private final short gettype(int i) throws IOException {
    	return file.getShort(typep+Short.BYTES*i);
    }
    private final byte getPrec(int i, int j) throws IOException {
    	return file.get(Precp+Byte.BYTES*(i*s+j));
    }

	public int query(int start, int stop) throws IOException {
		if (start==stop) 
			return start;
		int i=start;
		int j = stop;
		int mb_i = microblock(i);     // i's microblock
		int mb_j = microblock(j);     // j's microblock
		int min, min_tmp;             // min: to be returned
		int s_mi = mb_i * s;          // start of i's microblock
		int i_pos = i - s_mi;         // pos. of i in its microblock

		if (ARRAY_VERY_SMALL) { // scan naively
		  min = i;
		  for (int x = i+1; x <= j; x++) if (geta(x) > geta(min)) min = x;
		}
		else if (mb_i == mb_j) { // only one in-microblock-query
			min_tmp = clearbits(getPrec(gettype(mb_i),j-s_mi), i_pos);
			min = min_tmp == 0 ? j : s_mi + LSBTable256[min_tmp];
		}
		else { 
			int b_i = block(i);      // i's block
			int b_j = block(j);      // j's block
			int s_mj = mb_j * s;     // start of j's microblock
			int j_pos = j - s_mj;    // position of j in its microblock
			min_tmp = clearbits(getPrec(gettype(mb_i),s-1), i_pos);
			min = min_tmp == 0 ? s_mi + s - 1 : s_mi + LSBTable256[min_tmp]; // left in-microblock-query

			if (mb_j > mb_i + 1) { // otherwise only 2 in-microblock-queries
				int s_bi = b_i * sprime;      // start of i's block
				int s_bj = b_j * sprime;      // start of j's block
				if (s_bi+s > i) { // do another microblock-query to compensate for missing block-layer
					mb_i++;   // go one microblock to the right
					min_tmp = getPrec(gettype(mb_i),s-1) == 0 ?
						s_bi + sprime - 1 : s_mi + s + LSBTable256[getPrec(gettype(mb_i),s-1)];
					if (geta(min_tmp) > geta(min)) min = min_tmp;
				}

				if (b_j > b_i + 1) { // otherwise no out-of-block-queries
					int k, t, b;  // temporary variables
					b_i++; // block where out-of-block-query starts
					if (s_bj - s_bi - sprime <= sprimeprime) { // just one out-of-block-query
						k = log2fast(b_j - b_i - 1);
						t = 1 << k; // 2^k
						i = m(k, b_i); b = m(k, b_j-t); // i can be overwritten!
						min_tmp = geta(i) >= geta(b) ? i : b;
						if (geta(min_tmp) > geta(min)) min = min_tmp;
					}
					else { // here we have two out-of-block-queries:
	 					int sb_i = superblock(i); // i's superblock
	 					int sb_j = superblock(j); // j's superblock

	 					b = block((sb_i+1)*sprimeprime); // end of left out-of-block-query
	 					k = log2fast(b - b_i);
	 					t = 1 << k; // 2^k
	 					i = m(k, b_i); i_pos = m(k, b+1-t); // i & i_pos can be overwritten!
	 					min_tmp = geta(i) >= geta(i_pos) ? i : i_pos;
						if (geta(min_tmp) > geta(min)) min = min_tmp;

						if (sb_j > sb_i + 1) { // the superblock-query
							k = log2fast(sb_j - sb_i - 2);
							t = 1 << k;
							i = getMprime(k,sb_i+1); i_pos = getMprime(k,sb_j-t);
							min_tmp = geta(i) >= geta(i_pos) ? i : i_pos;
							if (geta(min_tmp) > geta(min)) min = min_tmp;
						}

						b = block(sb_j*sprimeprime); // start of right out-of-block-query
						k = log2fast(b_j - b);
						t = 1 << k; // 2^k
						b--; // going one block to the left doesn't harm and saves some tests
						i = m(k, b); i_pos = m(k, b_j-t);
						min_tmp = geta(i) >= geta(i_pos) ? i : i_pos;
						if (geta(min_tmp) > geta(min)) min = min_tmp;
					}
				}

				if (j >= s_bj+s) { // another microblock-query to compensate for missing block-layer
					min_tmp = getPrec(gettype(mb_j-1),s-1) == 0 ?
						s_mj - 1 : s_bj + LSBTable256[getPrec(gettype(mb_j-1),s-1)];
					if (geta(min_tmp) > geta(min)) min = min_tmp;
				}
			}

			min_tmp = getPrec(gettype(mb_j),j_pos) == 0 ?
				j : s_mj + LSBTable256[getPrec(gettype(mb_j),j_pos)];     // right in-microblock-query
			if (geta(min_tmp) > geta(min)) min = min_tmp;

		}

		return min;
	}

	public int getValue(int index) throws IOException {
		return geta(index);
	}
	
	
	
}
