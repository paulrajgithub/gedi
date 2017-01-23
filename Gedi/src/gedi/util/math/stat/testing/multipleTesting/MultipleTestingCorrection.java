package gedi.util.math.stat.testing.multipleTesting;


public class MultipleTestingCorrection {

	private double[] corrected;
	private int index = 0; 
	private MultipleTestingCorrectionMethod correction;
	private double insignificantThreshold = 0.05;
	
	
	public MultipleTestingCorrection(MultipleTestingCorrectionMethod correction) {
		this.correction = correction;
	}

	public MultipleTestingCorrection(MultipleTestingCorrectionMethod correction, double insignificantThreshold) {
		this.correction = correction;
		this.insignificantThreshold = insignificantThreshold;
	}

	public int addPValue(double pval) {
		if(corrected!=null)
			throw new RuntimeException("Has already been corrected!");
		
		if (pval>insignificantThreshold) {
			correction.addInsignificant();
			return -1;
		}
		else {
			correction.addPvalue(pval, new Callback(index++));
			return index-1;
		}
	}
	
	public double[] getCorrectedPvalues() {
		correct();
		return corrected;
	}
	
	public int getNumSignificant(double alpha) {
		correct();
		int n=0;
		for (int i=0; i<corrected.length; i++)
			if (corrected[i]<alpha)
				n++;
		return n;
	}


	private void correct() {
		if (corrected==null) {
			corrected = new double[index];
			correction.correct();
		}
	}


	private class Callback implements CorrectPvalueCallback {
		
		private int index;
		
		public Callback(int index) {
			this.index = index;
		}

		@Override
		public void setCorrectedPvalue(double c) {
			corrected[index]=c;
		}
		
	}
	
}