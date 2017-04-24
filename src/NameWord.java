
public class NameWord implements Comparable<NameWord>{

	public NameWord(String word) {
		super();
		this.word = word;
	}

	public String word;
	public int occurance=0;
	@Override
	public int compareTo(NameWord o) {
		// TODO Auto-generated method stub
		
		int result = o.occurance - this.occurance;
		
		if (result == 0) {
			result = this.word.compareTo(o.word);
		}
		
		return result;
	}
	
	public String toString() {
		return word + "(" + occurance + ")";
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getOccurance() {
		return occurance;
	}

	public void setOccurance(int occurance) {
		this.occurance = occurance;
	}
	
	public void incrementCount() {
		this.setOccurance(this.getOccurance() + 1);
	}

}
