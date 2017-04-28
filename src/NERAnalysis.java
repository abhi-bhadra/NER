import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.io.IOUtils;

public class NERAnalysis {

	public NERAnalysis() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public static String PERSON = "PERSON";
	public static String LOCATION = "LOCATION";
	public static int debugLevel = 6;
	public static int defaultLevel = 5;
	public static int alwaysPrint = 10;
	
	String nerInputFileName="";
	String nerInputText="";
	int totalWordCount=0;
	HashMap<String, HashMap<String, NameWord>> nerResponse = new HashMap<String, HashMap<String, NameWord>>();
	public String getNerInputFileName() {
		return nerInputFileName;
	}
	public String getNerInputText() {
		return nerInputText;
	}
	public int getTotalWordCount() {
		return totalWordCount;
	}
	public HashMap<String, HashMap<String, NameWord>> getNerResponse() {
		return nerResponse;
	}
	
	// print debug statements
	static void debugPrint(String aString, int aLevel) {
		if (aLevel >= debugLevel) {
			System.out.println(aString);
		}
	}
	
	public void setNerInputFileName(String nerInputFileName) throws IOException {
		this.nerInputFileName = nerInputFileName;
		setNerInputText(IOUtils.slurpFile(nerInputFileName));
	}
	
	public void setNerInputText(String nerInputText) {
		this.nerInputText = nerInputText;
	}
	
	public void setTotalWordCount(int totalWordCount) {
		this.totalWordCount = totalWordCount;
	}
	
	public void incrementWordCount() {
		totalWordCount++;
	}
	
	public boolean previouslySeenName(String aName){
		Set<String> nameSet = getNerResponseForType(PERSON).keySet();
		return nameSet.contains(aName);
	}
	
	public boolean previouslySeenLocation(String aLoc){
		Set<String> nameSet = getNerResponseForType(LOCATION).keySet();
		return nameSet.contains(aLoc);
	}
	
	public void incrementNameCount(String aName) {
		NameWord aNM = getNerResponseForType(PERSON).get(aName);
		aNM.incrementCount();
	}
	
	public void incrementLocationCount(String aLocation) {
		NameWord aNM = getNerResponseForType(LOCATION).get(aLocation);
		aNM.incrementCount();
	}
	
	public void handleName(String aName) {
		if (previouslySeenName(aName)) {
			incrementNameCount(aName);
		} else {
			NameWord nw = new NameWord(aName);
			getNerResponseForType(PERSON).put(aName, nw);
		}
	}
	
	public void handleLocation(String aLoc) {
		if (previouslySeenLocation(aLoc)) {
			incrementLocationCount(aLoc);
		} else {
			NameWord nw = new NameWord(aLoc);
			getNerResponseForType(LOCATION).put(aLoc, nw);
		}
	}
	
	public HashMap<String, NameWord> getNerResponseForType(String nerType) {
		HashMap<String, NameWord> result = getNerResponse().get(nerType);
		if (result==null) {
			result = new HashMap<String, NameWord>();
			getNerResponse().put(nerType, result);
		}
		return result;
	}
	
 public static void printTestResults(NERAnalysis nerAnalysis) {
		
		debugPrint("\n", alwaysPrint);
		debugPrint("Words Checked: " + nerAnalysis.totalWordCount, alwaysPrint);


		List<NameWord> sortedNames = new ArrayList<NameWord>(nerAnalysis.getNerResponseForType(NERAnalysis.PERSON).values());
		List<NameWord> sortedLoc = new ArrayList<NameWord>(nerAnalysis.getNerResponseForType(NERAnalysis.LOCATION).values());
		List<String> sortedList = new ArrayList<String>(nerAnalysis.getNerResponseForType(NERAnalysis.PERSON).keySet());
		List<String> sortedLocList = new ArrayList<String>(nerAnalysis.getNerResponseForType(NERAnalysis.LOCATION).keySet());
		int totalNames = 0;
		int totalLocs = 0;
		for (NameWord temp : sortedNames) {
			totalNames = totalNames + temp.occurance;
		}

		for (NameWord temp : sortedLoc) {
			totalLocs = totalLocs + temp.occurance;
		}
		
		debugPrint("Names Found: " + totalNames, alwaysPrint);
		debugPrint("Locations Found: " + totalLocs, alwaysPrint);

		Collections.sort(sortedNames);
		debugPrint("\nCounts of Unique Names found: " + sortedNames.size() + " in " + totalNames, alwaysPrint);
		debugPrint(sortedNames.toString(), alwaysPrint);

		Collections.sort(sortedList);
		debugPrint("\nList of Unique Names found: " + sortedList.size() + " in " + totalNames, alwaysPrint);
		debugPrint(sortedList.toString(), alwaysPrint);

		Collections.sort(sortedLoc);
		debugPrint("\nCounts of Unique Locations found: " + sortedLoc.size() + " in " + totalLocs, alwaysPrint);
		debugPrint(sortedLoc.toString(), alwaysPrint);

		Collections.sort(sortedLocList);
		debugPrint("\nList of Unique Locations found: " + sortedLocList.size() + " in " + totalLocs, alwaysPrint);
		debugPrint(sortedLocList.toString(), alwaysPrint);
	}
	
}
