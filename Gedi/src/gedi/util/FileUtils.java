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

package gedi.util;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.orm.Orm;
import gedi.util.orm.OrmSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.MappedByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import sun.nio.ch.DirectBuffer;
import cern.colt.bitvector.BitVector;


public class FileUtils {

	
	/**
	 * Finds files starting from root using filter and puts them into list.
	 * @param root the root to start
	 * @param filter the file filter
	 * @param list the result list
	 */
	public static void findFiles(File root, FileFilter filter, List<File> list, boolean recursive) {
		for (File f : root.listFiles())
			if (filter.accept(f))
				list.add(f);
			else if (recursive && f.isDirectory())
				findFiles(f, filter, list,recursive);
	}
	
	public static void writeAllText(String text, File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		fw.write(text);
		fw.flush();
		fw.close();
	}
	
	public static void writeAllLines(String[] lines, File file) throws IOException {
		writeAllText(StringUtils.concat("\n", lines), file);
	}
	
	public static <T> void writeAllLines(Iterable<T> lines, File file) throws IOException {
		writeAllText(StringUtils.concat("\n", lines), file);
	}

	public static void deleteRecursive(File file) {
		if (file.isFile())
			file.delete();
		else {
			for (File sub : file.listFiles()) 
				deleteRecursive(sub);
			file.delete();
		}
		
	}
	
	public static void deleteContentsRecursive(File file) {
		for (File sub : file.listFiles()) 
			deleteRecursive(sub);
	}

	public static String readAllText(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		while((line=br.readLine())!=null)
			sb.append(line+"\n");
		br.close();
		return sb.toString();
	}
	
	public static String[] readAllLines(InputStream stream) throws IOException {
		List<String> re = new LinkedList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String line;
		while((line=br.readLine())!=null)
			re.add(line);
		br.close();
		return re.toArray(new String[re.size()]);
	}

	
	public static String[] readAllLines(File file) throws IOException {
		return readAllLines(new FileInputStream(file));
	}

	public static String getExtension(File f) {
		String path= f.getAbsolutePath();
		return getExtension(path);
	}
	
	public static String getFullNameWithoutExtension(File f) {
		String path= f.getAbsolutePath();
		return getFullNameWithoutExtension(path);
	}
	
	public static String getNameWithoutExtension(File f) {
		String path= f.getAbsolutePath();
		return getNameWithoutExtension(path);
	}
	
	public static String getExtension(Path f) {
		String path= f.toString();
		return getExtension(path);
	}
	
	public static String getFullNameWithoutExtension(Path f) {
		String path= f.toString();
		return getFullNameWithoutExtension(path);
	}
	
	
	public static String getExtension(String path) {
		int dotIndex = path.lastIndexOf('.');
		int slashIndex = path.lastIndexOf(File.pathSeparatorChar);
		if (dotIndex>slashIndex)
			return path.substring(dotIndex+1);
		else
			return "";
	}
	
	public static String getFullNameWithoutExtension(String path) {
		int dotIndex = path.lastIndexOf('.');
		int slashIndex = path.lastIndexOf(File.separatorChar);
		if (dotIndex>slashIndex)
			return path.substring(0,dotIndex);
		else
			return path;
	}

	public static String getNameWithoutExtension(String path) {
		int dotIndex = path.lastIndexOf('.');
		int slashIndex = path.lastIndexOf(File.separatorChar);
		if (dotIndex>slashIndex)
			return path.substring(slashIndex+1,dotIndex);
		else
			return path.substring(slashIndex+1);
	}


	public static void writeStringArray(BinaryWriter f, String[] a) throws IOException {
		f.putInt(a.length);
		for (int i=0; i<a.length; i++) 
			f.putString(a[i]);
	}
	
	public static String[] readStringArray(BinaryReader f) throws IOException {
		String[] re = new String[f.getInt()];
		for (int i=0; i<re.length; i++) 
			re[i] = f.getString();
		return re;
	}
	
	public static void writeReferenceSequence(BinaryWriter out,
			ReferenceSequence reference) throws IOException {
		out.putString(reference.getName()+reference.getStrand().toString());
	}
	public static void writeGenomicRegion(BinaryWriter out,
			GenomicRegion region) throws IOException {
		int parts = region.getNumParts();
		out.putCInt(parts);
		for (int i=0; i<parts; i++) {
			out.putCInt(i==0?region.getStart(i):(region.getStart(i)-region.getEnd(i-1)));
			out.putCInt(region.getLength(i));
		}
	}

	
	public static Chromosome readReferenceSequence(BinaryReader in) throws IOException {
		return Chromosome.obtain(in.getString());
	}
	public static ArrayGenomicRegion readGenomicRegion(BinaryReader in) throws IOException {
		int parts = in.getCInt();
		int[] re = new int[parts*2];
		for (int i=0; i<re.length; i++)
			re[i] = in.getCInt();
		ArrayUtils.cumSumInPlace(re, 1);
		return new ArrayGenomicRegion(re);
	}

	
	public static void writeIntArray(BinaryWriter f, int[] a) throws IOException {
		f.putInt(a.length);
		for (int i=0; i<a.length; i++) 
			f.putInt(a[i]);
	}
	
	public static void writeNumber(BinaryWriter out, Number d) throws IOException {
		if (d instanceof Byte)
			out.putByte(d.intValue());
		else if (d instanceof Short)
			out.putShort(d.shortValue());
		else if (d instanceof Integer)
			out.putInt(d.intValue());
		else if (d instanceof Long)
			out.putLong(d.longValue());
		else if (d instanceof Float)
			out.putFloat(d.floatValue());
		else
			out.putDouble(d.doubleValue());
	}
	
	
	public static void writeShortArray(BinaryWriter f, short[] a) throws IOException {
		f.putInt(a.length);
		for (int i=0; i<a.length; i++) 
			f.putShort(a[i]);
	}
	
	public static void writeDoubleArray(BinaryWriter f, double[] a) throws IOException {
		f.putInt(a.length);
		for (int i=0; i<a.length; i++) 
			f.putDouble(a[i]);
	}
	
	public static void writeDoubleArrayList(BinaryWriter f, DoubleArrayList a) throws IOException {
		f.putInt(a.size());
		for (int i=0; i<a.size(); i++) 
			f.putDouble(a.getDouble(i));
	}
	
	public static void writeIntArrayList(BinaryWriter f, IntArrayList a) throws IOException {
		f.putInt(a.size());
		for (int i=0; i<a.size(); i++) 
			f.putInt(a.getInt(i));
	}
	
	public static IntArrayList readIntArrayList(BinaryReader f) throws IOException {
		int size = f.getInt();
		IntArrayList re = new IntArrayList(size);
		for (int i=0; i<size; i++) 
			re.add(f.getInt());
		return re;
	}
	
	public static DoubleArrayList readDoubleArrayList(BinaryReader f) throws IOException {
		int size = f.getInt();
		DoubleArrayList re = new DoubleArrayList(size);
		for (int i=0; i<size; i++) 
			re.add(f.getDouble());
		return re;
	}
	
	public static int[] readIntArray(BinaryReader f) throws IOException {
		int[] re = new int[f.getInt()];
		for (int i=0; i<re.length; i++) 
			re[i] = f.getInt();
		return re;
	}
	
	public static short[] readShortArray(BinaryReader f) throws IOException {
		short[] re = new short[f.getInt()];
		for (int i=0; i<re.length; i++) 
			re[i] = f.getShort();
		return re;
	}

	public static double[] readDoubleArray(BinaryReader f) throws IOException {
		double[] re = new double[f.getInt()];
		for (int i=0; i<re.length; i++) 
			re[i] = f.getDouble();
		return re;
	}

	public static void write(BinaryWriter out, Object n) throws IOException {
		if (n instanceof CharSequence) {
			out.putString("A");
			out.putString(n.toString());
		}
		else if (n instanceof Number) {
			if (n instanceof Byte) {
				out.putString("B");
				out.put((Byte)n);
			}
			else if (n instanceof Short) {
				out.putString("S");
				out.putShort((Short)n);
			}
			else if (n instanceof Integer) {
				out.putString("I");
				out.putInt((Integer)n);
			}
			else if (n instanceof Long) {
				out.putString("L");
				out.putLong((Long)n);
			}
			else if (n instanceof Float) {
				out.putString("F");
				out.putFloat((Float)n);
			}
			else if (n instanceof Double) {
				out.putString("D");
				out.putDouble((Double)n);
			}
			else
				throw new RuntimeException("Could not encode number "+n.getClass().getName());
			
		}
		else if (n instanceof BinarySerializable) {
			out.putString(n.getClass().getName());
			((BinarySerializable)n).serialize(out);
		} else
			throw new RuntimeException("Only BinarySerializables can be serialized right now!");
	}
	
	
	public static <T> T read(BinaryReader in) throws IOException, ClassNotFoundException {
		String clsName = in.getString();
		
		if (clsName.length()==1) {
			switch (clsName) {
			case "A":
				return (T) in.getString();
			case "B":
				return (T)new Byte(in.get());
			case "S":
				return (T)new Short(in.getShort());
			case "I":
				return (T)new Integer(in.getInt());
			case "L":
				return (T)new Long(in.getLong());
			case "F":
				return (T)new Float(in.getFloat());
			case "D":
				return (T)new Double(in.getDouble());
			}
		}
		
		
		Class<T> cls = (Class<T>) Class.forName(clsName); 
		
		if (BinarySerializable.class.isAssignableFrom(cls)) {
			try {
				T re = cls.newInstance();
				((BinarySerializable) re).deserialize(in);
				return re;
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IOException("Could not instantiate "+cls.getName(),e);
			}
		} else
			throw new RuntimeException("Only BinarySerializables can be deserialized right now!");
	}
	
	public static void writeBitVector(BitVector bits,
			BinaryWriter out) throws IOException {
		out.putInt(bits.size());
		for (int b=0; b<bits.size(); b+=8)
			out.putByte((int)bits.getLongFromTo(b, Math.min(b+7,bits.size()-1)));
	}

	public static void readBitVector(BitVector bits,
			BinaryReader in) throws IOException {
		int size = in.getInt();
		bits.setSize(size);
		for (int b=0; b<bits.size(); b+=8)
			bits.putLongFromTo(in.getByte(), b, Math.min(b+7,bits.size()-1));
	}
	
	
	public static String[] find(String glob) throws IOException {
		return find(".",glob,true);
	}
	public static String[] find(String path, String glob, boolean stripPath) throws IOException {
		if (!path.endsWith("/")) path = path+"/";
		String cpath = path;
		ArrayList<String> re = new ArrayList<String>(); 
		PathMatcher matcher =FileSystems.getDefault().getPathMatcher("glob:" + path+glob);
		Files.find(Paths.get(path), 9999, (p,per)->{
			return matcher.matches(p);	
		}, FileVisitOption.FOLLOW_LINKS).forEach(
				f->{
					String file = f.toString();
					if (stripPath && file.startsWith(cpath))
						file = file.substring(cpath.length()); 
					re.add(file);
				}
				);
		return re.toArray(new String[0]);
	}

	public static <D> D deserialize(D obj, BinaryReader in) throws IOException {
		if (obj instanceof BinarySerializable) {
			((BinarySerializable) obj).deserialize(in);
			return obj;
		} else
			return Orm.fromBinaryReader(in, obj);
	}

	
	public static <D> D serialize(D obj, BinaryWriter out) throws IOException {
		if (obj instanceof BinarySerializable) {
			((BinarySerializable) obj).serialize(out);
			return obj;
		} else
			return Orm.toBinaryWriter(obj, out);
	}

	
	private static final long GC_TIMEOUT_MS = 500;
	private static LinkedBlockingQueue<WeakReference<MappedByteBuffer>> byteBufferUnmapQueue = new LinkedBlockingQueue<WeakReference<MappedByteBuffer>>(10);
	private static boolean byteBufferUnmapperStarted = false;
	private static Thread byteBufferUnmapper = new Thread() {
			public void run() {
			for (;;) {
				try {
					unmapSynchronous(byteBufferUnmapQueue.take());
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	};
	/**
	 *Make sure that there is no strong reference left to the buffer!
	 * @param ref
	 * @return
	 */
    public static boolean unmap(WeakReference<MappedByteBuffer> ref) {
    	if (!byteBufferUnmapperStarted) {
    		synchronized (byteBufferUnmapQueue) {
    			if (!byteBufferUnmapperStarted){
	    			byteBufferUnmapper.setName("File-Unmapper");
	        		byteBufferUnmapper.setDaemon(true);
	    			byteBufferUnmapper.start();
	    			byteBufferUnmapperStarted = true;
    			}
			}
    		
    	}
    	try {
			byteBufferUnmapQueue.put(ref);
		} catch (InterruptedException e) {
		}
        return true;
	}
    
    /**
	 *Make sure that there is no strong reference left to the buffer!
	 * @param ref
	 * @return
	 */
    public static boolean unmapSynchronous(WeakReference<MappedByteBuffer> ref) {
    	MappedByteBuffer buffer = ref.get();
    	if (buffer==null) return true;
        // first write all data
        buffer.force();

        // need to dispose old direct buffer, see bug
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038

        boolean useSystemGc = true;
        try {
        	((DirectBuffer)buffer).cleaner().clean();
            useSystemGc = false;
        } catch (Throwable e) {
            // useSystemGc is already true
        } finally {
        }
        if (useSystemGc) {
            buffer = null;
            long start = System.currentTimeMillis();
            while (ref.get() != null) {
                if (System.currentTimeMillis() - start > GC_TIMEOUT_MS) {
                  return false;
                }
                System.gc();
                Thread.yield();
            }
        }
        return true;
	}

	public static void applyRecursively(String path, Consumer<File> action) {
		File root = new File( path );
        File[] list = root.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) 
            	applyRecursively( f.getAbsolutePath(), action);
            else 
                action.accept(f);
        }
	}
	
	
	public static void serialize(String path, Object o) throws IOException {
		OrmSerializer ser = new OrmSerializer();
		PageFileWriter out = new PageFileWriter(path);
		ser.serializeAll(out, EI.singleton(o));
		out.close();
	}
	
	public static <T> T deserialize(String path) throws IOException {
		OrmSerializer ser = new OrmSerializer();
		PageFile in = new PageFile(path);
		T re = (T) ser.deserializeAll(in).first();
		in.close();
		return re;
	}


}
