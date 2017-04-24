
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

	// print debug statements
	static void debugPrint(String aString, int aLevel) {
		if (aLevel >= debugLevel) {
			System.out.println(aString);
		}
	}

	// public method to test
	public static boolean checkStringForName(String str) throws Exception {
		boolean result = false;
		initIfRequired();
		debugPrint("Input Token: " + str, defaultLevel);
		
		//String cleaned = str.replace('-', ' ');
		String cleaned = str.replaceAll("[^a-zA-Z\\d\\s:]", " ");
		cleaned = cleaned.trim();
		//System.out.println(cleaned);
		for (List<CoreLabel> lcl : classifier.classify(cleaned)) {
			for (CoreLabel cl : lcl) {

				wordsChecked = wordsChecked + 1;
				boolean alreadySeenName = false;
				boolean alreadySeenLoc = false;
				boolean subResult = false;
				
				alreadySeenName = nameSet.contains(cl.toString());
				alreadySeenLoc = locationSet.contains(cl.toString());
				
				if (!(alreadySeenName || alreadySeenLoc)) {
					// previously unseen name or location
					subResult = checkName(cl);
				} else {
					// previously seen name or location
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
				if (subResult) {
					result = true;
					debugPrint("Cleaned Name: " + cl.toString(), defaultLevel);
					nameSet.add(cl.toString());
					NameWord nw = new NameWord(cl.toString());
					nw.incrementCount();
					nameWordMap.putIfAbsent(nw.getWord(), nw);
					names++;
				}
				
				// if word was not already a location, see if it is a newly found location now
				if (!alreadySeenLoc) {
					alreadySeenLoc = locationSet.contains(cl.toString());
					if (alreadySeenLoc) {
						// result = true;
						debugPrint("Cleaned Loc: " + cl.toString(), defaultLevel);
						locationSet.add(cl.toString());
						NameWord nw = new NameWord(cl.toString());
						nw.incrementCount();
						locations++;
						locMap.putIfAbsent(nw.getWord(), nw);
					}
				}

			}

		}

		return result;
	}

	// check if a given word is a name
	static boolean checkName(CoreLabel aNameWord) {
		String aName = aNameWord.toString();
		boolean isName = false;
		boolean isWord = true;
		boolean isInNamelist = false;

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

		// check with NER library if the string is a name
		if (!isName) {
			debugPrint("Checking NER: " + aName, defaultLevel);
			boolean isNER = false;
			isNER = checkNERPerson(aNameWord);
			if (isNER) {
				debugPrint("Result Person: " + aName + ": " + isNER, alwaysPrint);
				nameSet.add(aName.toString());
			}
			isName = isNER;
		}
		
		// check with NER library if the string is a location
		if (!isName) {
			debugPrint("Checking NER: " + aName, defaultLevel);
			boolean isNER = false;
			isNER = checkNERLocation(aNameWord);
			if (isNER) {
				debugPrint("Result Location: " + aName + ": " + isNER, alwaysPrint);
				locationSet.add(aName);
			}
			//isName = isNER;
		}

		// either the name is in the shape of a name Xx* and not a dictionary
		// word, or NER thinks it is a name
		return (isName);

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
//		if (wordShapeSet.contains(aName)) {
//			debugPrint("Dictionary check: " + aName, defaultLevel);
//			return isWord;
//		}
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
			boolean result;
			fileContents = IOUtils.slurpFile(filename);

			// System.out.println(fileContents);

			// tokenize and check each token
			StringTokenizer tok = new StringTokenizer(fileContents, " ");
			while (tok.hasMoreTokens()) {

				String str = (String) tok.nextElement();
				debugPrint("File Token: " + str, defaultLevel);

				// run test
				result = checkStringForName(str);

				// display results
				int lev = defaultLevel;
				if (result) {
					//lev = alwaysPrint;
					debugPrint("String Input: " + str, lev);
					debugPrint("Has Name: " + result, lev);
					debugPrint("", lev);
					//names++;
				} else {
					// lev = alwaysPrint;
					debugPrint("String Input: " + str, lev);
					debugPrint("Has Name: " + result, lev);
					debugPrint("", lev);
				}

			}

			// report hit rate
			debugPrint("Words Checked: " + wordsChecked, alwaysPrint);
			debugPrint("Names Found: " + names, alwaysPrint);
			debugPrint("Locations Found: " + locations, alwaysPrint);

		}
		
		List<NameWord> sortedNames = new ArrayList<NameWord>(nameWordMap.values());
		List<NameWord> sortedLoc = new ArrayList<NameWord>(locMap.values());
		List<String> sortedList = new ArrayList<String>(nameSet);
		List<String> sortedLocList = new ArrayList<String>(locationSet);
		int totalNames=0;
		int totalLocs=0;
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
