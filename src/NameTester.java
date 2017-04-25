
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;

public class NameTester {

	final static Pattern pattern = Pattern.compile("([A-Z][a-z][a-z]*)");
	final static String PERSON = "PERSON";
	final static String LOCATION = "LOCATION";
	public static int debugLevel = 6;
	public static int defaultLevel = 5;
	public static int alwaysPrint = 10;

	public static int wordsChecked = 0;
	public static int names = 0;
	public static int locations = 0;

	static AbstractSequenceClassifier<CoreLabel> classifier;
	static String namelist;
	static String wordlist;
	static String excludedList;
	static boolean needsInit = true;
	static HashSet<String> nameSet = new HashSet<String>();
	static HashSet<String> locationSet = new HashSet<String>();
	static HashMap<String,NameWord> nameWordMap = new HashMap<String,NameWord>();
	static HashMap<String,NameWord> locMap = new HashMap<String,NameWord>();
	static HashSet<String> wordShapeSet = new HashSet<String>();

	static void initIfRequired() throws Exception {
		if (needsInit) {
			initNameList();
			initWordList();
			initExcludedList();
			String serializedClassifier = "english.all.3class.distsim.crf.ser.gz";
			classifier = CRFClassifier.getClassifier(serializedClassifier);
			needsInit = false;
		}
	}

	static void initNameList() throws Exception {
		namelist = IOUtils.slurpFile("propernames.txt");
	}

	static void initWordList() throws Exception {
		wordlist = IOUtils.slurpFile("web2.txt");
	}
	
	static void initExcludedList() throws Exception{
		excludedList=IOUtils.slurpFile("Exclusions.txt");
	}

	// print debug statements
	static void debugPrint(String aString, int aLevel) {
		if (aLevel >= debugLevel) {
			System.out.println(aString);
		}
	}

	// public method to test
	public static NamedEntityRetrievalResponse checkStringForNameOrLocation(String str) throws Exception {
		boolean result = false;
		boolean alreadySeenName = false;
		boolean alreadySeenLoc = false;
		boolean nameResult = false;
		boolean locResult = false;
		
		initIfRequired();
		debugPrint("Input Token: " + str, defaultLevel);
		
		//Replace all non alpha chars for right now
		String cleaned = str.replaceAll("[^a-zA-Z\\d\\s:]", " ");
		cleaned = cleaned.trim();
		debugPrint("Cleaned Input Token: " + cleaned, defaultLevel);
		for (List<CoreLabel> lcl : classifier.classify(cleaned)) {
			for (CoreLabel cl : lcl) {
				
				// stop checking once something is found within token passed in
				if (!result) {

					wordsChecked = wordsChecked + 1;


					alreadySeenName = nameSet.contains(cl.toString());
					alreadySeenLoc = locationSet.contains(cl.toString());

					if (!(alreadySeenName || alreadySeenLoc)) {
						// previously unseen name or location, check for name
						// first
						nameResult = checkName(cl);
					} else {
						// previously seen name or location, just increment
						// counters
						result = true;
						// increment count
						if (alreadySeenName) {
							NameWord nw = nameWordMap.get(cl.toString());
							nw.incrementCount();
							names++;
						}
						if (alreadySeenLoc) {
							NameWord nw = locMap.get(cl.toString());
							nw.incrementCount();
							locations++;
						}

					}

					// printProbalitiesForChunk(classifier, word.toString());

					// new word was determined to be a newly seen name
					if (nameResult) {
						result = true;
						debugPrint("Cleaned Name: " + cl.toString(), defaultLevel);
						nameSet.add(cl.toString());
						NameWord nw = new NameWord(cl.toString());
						nw.incrementCount();
						nameWordMap.putIfAbsent(nw.getWord(), nw);
						names++;
					}

					// if word was not already a location, or a new name, see if
					// it is a newly found location now
					if (!(alreadySeenLoc || nameResult)) {
						locResult = checkLocation(cl);
						if (locResult) {
							result = true;
							debugPrint("Cleaned Location: " + cl.toString(), defaultLevel);
							locationSet.add(cl.toString());
							NameWord nw = new NameWord(cl.toString());
							nw.incrementCount();
							locations++;
							locMap.putIfAbsent(nw.getWord(), nw);
						}
					}

				}

			}
		}

		NamedEntityRetrievalResponse nerResp= new NamedEntityRetrievalResponse();
		if (result) {
			if (alreadySeenName || nameResult) nerResp.setResponseType(NamedEntityRetrievalResponse.getPerson());
			if (alreadySeenLoc || locResult) nerResp.setResponseType(NamedEntityRetrievalResponse.getLocation());
		}
		return nerResp;
		
		
	}

	// check if a given word is a name
	static boolean checkName(CoreLabel aNameWord) {
		String aName = aNameWord.toString();
		boolean isName = false;
		boolean isWord = true;
		boolean isInNamelist = false;

		boolean isExcluded = checkExclusions(aName);
		if (isExcluded) return false;
		
		// print what is being tested
		// System.out.println("\nName Input: " + aName);
		debugPrint("\nName Input: " + aName, defaultLevel);

		// check regular expression patters Xx*
		if (aName != null) {
			isName = checkRegex(aName);
		}

		// check if in list of names
		if (isName) {
			isInNamelist = checkNameList(aName);
			isName = isInNamelist;
			if (isName) {
				debugPrint("Found name: " + aName, defaultLevel);
			}
		}

		// check to see if it is a dictionary word if it is the proper shape
		if (isName) {
			isWord = checkDictionary(aName);
			if (isWord) {
				debugPrint("Found word: " + aName, defaultLevel);
			}
			isName = !isWord;
		}
		

		// check with NER library if the string is a name, override initial result
		if (!isName) {
			debugPrint("Checking NER: " + aName, defaultLevel);
			boolean isNER = false;
			isNER = checkNERPerson(aNameWord);
			if (isNER) {
				debugPrint("Result Person: " + aName + ": " + isNER, alwaysPrint);
			}
			isName = isNER;
		}
		
		if (isName) nameSet.add(aName.toString());

		// either the name is in the shape of a name Xx* and not a dictionary
		// word, or NER thinks it is a name
		return (isName);
	}
	
	// check if a given word is a Location
	static boolean checkLocation(CoreLabel aLocWord) {

		String aLoc = aLocWord.toString();
		boolean isLocation = false;

		boolean isExcluded = checkExclusions(aLoc);
		if (isExcluded)
			return false;

		// print what is being tested
		// System.out.println("\nName Input: " + aName);
		debugPrint("\nLocation Input: " + aLoc, defaultLevel);

		// check with NER library if the string is a location
		debugPrint("Checking Location NER: " + aLoc, defaultLevel);
		boolean isNER = false;
		isNER = checkNERLocation(aLocWord);
		if (isNER) {
			debugPrint("Result Location: " + aLoc + ": " + isNER, alwaysPrint);
			locationSet.add(aLoc);
			isLocation = isNER;
		}
		
		return isLocation;
	}
	
	

	static boolean checkRegex(String aName) {
		boolean result = false;
		if (aName != null) {
			if (pattern.matcher(aName).matches()) {
				result = true;
			}
		}
		debugPrint("Regex Matches: " + result, defaultLevel);
		return result;
	}

	static boolean checkNERPerson(CoreLabel aNameWord) {
		boolean result = false;
		result = aNameWord.get(CoreAnnotations.AnswerAnnotation.class).equals(PERSON);
		debugPrint("NER Person Matches: " + result, defaultLevel);
		return result;
	}
	
	static boolean checkNERLocation(CoreLabel aNameWord) {
		boolean result = false;
		result = aNameWord.get(CoreAnnotations.AnswerAnnotation.class).equals(LOCATION);
		debugPrint("NER Location Matches: " + result, defaultLevel);
		return result;
	}

	 static boolean checkDictionary(String aName) {
		boolean isWord = false;
		// if see same name shaped word more than once, assume it is a name
		if (wordShapeSet.contains(aName)) {
			debugPrint("Dictionary check: " + aName, defaultLevel);
			return isWord;
		}
		String lcName = aName.toLowerCase();
		if (!lcName.equals(aName)) {
			if (checkRegex(aName)) {
				wordShapeSet.add(aName);
			}
		}
		isWord = wordlist.indexOf((lcName + " ")) > -1;
		if (isWord) {
			debugPrint("Dictionary Matches: " + lcName, defaultLevel);
		}

		return isWord;
	}
	 
	 static boolean checkExclusions(String aName) {
		boolean isExcluded = false;
		// if see same name shaped word more than once, assume it is a name
//		if (wordShapeSet.contains(aName)) {
//			debugPrint("Dictionary check: " + aName, defaultLevel);
//			return isWord;
//		}
		
		isExcluded = excludedList.indexOf((aName + " ")) > -1;
		if (isExcluded) {
			debugPrint("Excluded Matches: " + aName, defaultLevel);
		}

		return isExcluded;
	}

	static boolean checkNameList(String aName) {
		boolean isName = false;
		isName = namelist.indexOf(aName + " ") > -1;
		if (isName) {
			debugPrint("ProperName Matches: " + aName, defaultLevel);
		}

		return isName;
	}

	static void printProbabilities(AbstractSequenceClassifier<CoreLabel> aClassifier, String aFilename) {
		DocumentReaderAndWriter<CoreLabel> readerAndWriter = aClassifier.makePlainTextReaderAndWriter();
		debugPrint("Per-token marginalized probabilities", defaultLevel);
		aClassifier.printProbs(aFilename, readerAndWriter);
	}

	static void printProbalitiesForChunk(AbstractSequenceClassifier<CoreLabel> aClassifier, String aChunk) {
		//DocumentReaderAndWriter<CoreLabel> readerAndWriter = aClassifier.makePlainTextReaderAndWriter();
		//String[] stringArray = { aChunk };
		// System.out.println(stringArray[0]);
		// stringArray[0] = aChunk;

		// List<List<CoreLabel>> docs = aClassifier.classifyRaw(aChunk,
		// readerAndWriter);
		// aClassifier.printProbsDocuments(docs);
	}

	// main method
	// create classifier
	// read input (if not a file then use that as the input string to check)
	// parse content into tokens
	// check each token
	public static void main(String[] args) throws Exception {

		String filename;
		if (args.length > 0) {
			filename = args[0];
		} else {
			filename = "MyTestFile.txt";
		}
		if (true) {

			initIfRequired();

			// print probabilities
			// printProbabilities(classifier, args[0]);

			// read input
			String fileContents;
			NamedEntityRetrievalResponse result;
			fileContents = IOUtils.slurpFile(filename);

			// System.out.println(fileContents);

			// tokenize and check each token
			StringTokenizer tok = new StringTokenizer(fileContents, " ");
			while (tok.hasMoreTokens()) {

				String str = (String) tok.nextElement();
				debugPrint("File Token: " + str, defaultLevel);

				// run test
				result = checkStringForNameOrLocation(str);

				// display results
				int lev = alwaysPrint;
				if (!result.isOther()) {
					debugPrint("String Input: " + str, lev);
					debugPrint("Has Entity: " + result, lev);
					debugPrint("", lev);
				}

			}
			
			// report hit rate
			debugPrint("\n", alwaysPrint);
			debugPrint("Words Checked: " + wordsChecked, alwaysPrint);
			debugPrint("Names Found: " + names, alwaysPrint);
			debugPrint("Locations Found: " + locations, alwaysPrint);

			List<NameWord> sortedNames = new ArrayList<NameWord>(nameWordMap.values());
			List<NameWord> sortedLoc = new ArrayList<NameWord>(locMap.values());
			List<String> sortedList = new ArrayList<String>(nameSet);
			List<String> sortedLocList = new ArrayList<String>(locationSet);
			int totalNames = 0;
			int totalLocs = 0;
			for (NameWord temp : sortedNames) {
				totalNames = totalNames + temp.occurance;
			}

			for (NameWord temp : sortedLoc) {
				totalLocs = totalLocs + temp.occurance;
			}

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

}
