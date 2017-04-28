import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

public class StanfordNerAnalyzer {
	NERAnalysis nerAnalysis = new NERAnalysis();
	String namelist;
	String wordlist;
	String excludedList;
	String serializedClassifier;
	String inputFileName;
	AbstractSequenceClassifier<CoreLabel> classifier;
	final Pattern pattern = Pattern.compile("([A-Z][a-z][a-z]*)");
	HashSet<String> wordShapeSet = new HashSet<String>();
	final String PERSON = "PERSON";
	final String LOCATION = "LOCATION";
	public int debugLevel = 6;
	public int defaultLevel = 5;
	public int alwaysPrint = 10;

	boolean needsInit = true;

	public StanfordNerAnalyzer(NERAnalysis nerAnalysis) {
		super();
		this.nerAnalysis = nerAnalysis;
		try {
			this.initIfRequired();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public StanfordNerAnalyzer(String inputFileName) {
		super();
		this.inputFileName = inputFileName;

		try {
			this.nerAnalysis.setNerInputFileName(inputFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.initIfRequired();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public StanfordNerAnalyzer() {
		super();
		try {
			this.initIfRequired();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void initIfRequired() throws Exception {
		if (needsInit) {
			initNameList();
			initWordList();
			initExcludedList();
			serializedClassifier = "english.all.3class.distsim.crf.ser.gz";
			classifier = CRFClassifier.getClassifier(serializedClassifier);
			needsInit = false;
		}
	}

	void initNameList() throws Exception {
		namelist = IOUtils.slurpFile("propernames.txt");
	}

	void initWordList() throws Exception {
		wordlist = IOUtils.slurpFile("web2.txt");
	}

	void initExcludedList() throws Exception {
		excludedList = IOUtils.slurpFile("Exclusions.txt");
	}

	// print debug statements
	void debugPrint(String aString, int aLevel) {
		if (aLevel >= debugLevel) {
			System.out.println(aString);
		}
	}

	// public method to test
	public List<NamedEntityRetrievalResponse> checkStringForNameOrLocation(String str) throws Exception {

		List<NamedEntityRetrievalResponse> namedEntityList = new ArrayList<NamedEntityRetrievalResponse>();

		initIfRequired();
		debugPrint("Input Token: " + str, defaultLevel);

		// Replace all non alpha chars for right now
		String cleaned = str.replaceAll("[^a-zA-Z\\d\\s:]", " ");
		cleaned = cleaned.trim();
		debugPrint("Cleaned Input Token: " + cleaned, defaultLevel);
		for (List<CoreLabel> lcl : classifier.classify(cleaned)) {
			for (CoreLabel cl : lcl) {
				String newWord = cl.toString();
				boolean result = false;
				boolean alreadySeenName = false;
				boolean alreadySeenLoc = false;
				boolean nameResult = false;
				boolean locResult = false;
				nerAnalysis.incrementWordCount();

				alreadySeenName = nerAnalysis.previouslySeenName(newWord);
				alreadySeenLoc = nerAnalysis.previouslySeenLocation(newWord);

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
						nerAnalysis.incrementNameCount(newWord);
					}
					if (alreadySeenLoc) {
						nerAnalysis.incrementLocationCount(newWord);
					}

				}

				// printProbalitiesForChunk(classifier, word.toString());

				// new word was determined to be a newly seen name
				if (nameResult) {
					result = true;
					debugPrint("Cleaned Name: " + cl.toString(), defaultLevel);
					nerAnalysis.handleName(newWord);
				}

				// if word was not already a location, or a new name, see if
				// it is a newly found location now
				if (!(alreadySeenLoc || nameResult)) {
					locResult = checkLocation(cl);
					if (locResult) {
						result = true;
						debugPrint("Cleaned Location: " + newWord, defaultLevel);
						nerAnalysis.handleLocation(newWord);
					}
				}

				// collect proper results in a list

				if (result) {
					NamedEntityRetrievalResponse nerResp = new NamedEntityRetrievalResponse();
					nerResp.setEntity(cl.toString());
					if (alreadySeenName || nameResult)
						nerResp.setResponseType(NamedEntityRetrievalResponse.getPerson());
					if (alreadySeenLoc || locResult)
						nerResp.setResponseType(NamedEntityRetrievalResponse.getLocation());
					namedEntityList.add(nerResp);
				}

			}
		}

		if (namedEntityList.size() > 0)
			debugPrint("Original Input Chunk: " + str, defaultLevel);
		return namedEntityList;

	}

	// check if a given word is a name
	boolean checkName(CoreLabel aNameWord) {
		String aName = aNameWord.toString();
		boolean isName = false;
		boolean isWord = true;
		boolean isInNamelist = false;

		boolean isExcluded = checkExclusions(aName);
		if (isExcluded)
			return false;

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

		// check with NER library if the string is a name, override initial
		// result
		if (!isName) {
			debugPrint("Checking NER: " + aName, defaultLevel);
			boolean isNER = false;
			isNER = checkNERPerson(aNameWord);
			if (isNER) {
				debugPrint("Result Person: " + aName + ": " + isNER, defaultLevel);
			}

			isName = isNER;
		}

		if (isName)
			nerAnalysis.handleName(aName);

		// either the name is in the shape of a name Xx* and not a dictionary
		// word, or NER thinks it is a name
		return (isName);
	}

	// check if a given word is a Location
	boolean checkLocation(CoreLabel aLocWord) {

		String aLoc = aLocWord.toString();
		boolean isLocation = false;

		boolean isExcluded = checkExclusions(aLoc);
		if (isExcluded)
			return false;

		boolean isName = checkNameList(aLoc);
		if (isName)
			return false;

		// was this previously considered a name? if so, not a location
		isName = nerAnalysis.previouslySeenName(aLoc);// nameSet.contains(aLoc);
		if (isName)
			return false;

		// print what is being tested
		// System.out.println("\nName Input: " + aName);
		debugPrint("\nLocation Input: " + aLoc, defaultLevel);

		// check with NER library if the string is a location
		debugPrint("Checking Location NER: " + aLoc, defaultLevel);
		boolean isNER = false;
		isNER = checkNERLocation(aLocWord);
		if (isNER) {
			debugPrint("Result Location: " + aLoc + ": " + isNER, defaultLevel);
			nerAnalysis.handleLocation(aLoc);
			isLocation = isNER;
		}

		return isLocation;
	}

	boolean checkRegex(String aName) {
		boolean result = false;
		if (aName != null) {
			if (pattern.matcher(aName).matches()) {
				result = true;
			}
		}
		debugPrint("Regex Matches: " + result, defaultLevel);
		return result;
	}

	boolean checkNERPerson(CoreLabel aNameWord) {
		boolean result = false;
		result = aNameWord.get(CoreAnnotations.AnswerAnnotation.class).equals(PERSON);
		debugPrint("" + aNameWord.get(CoreAnnotations.AnswerAnnotation.class), defaultLevel);
		debugPrint("NER Person Matches: " + result, defaultLevel);
		return result;
	}

	boolean checkNERLocation(CoreLabel aNameWord) {
		boolean result = false;
		result = aNameWord.get(CoreAnnotations.AnswerAnnotation.class).equals(LOCATION);
		debugPrint("NER Location Matches: " + result, defaultLevel);
		return result;
	}

	boolean checkDictionary(String aName) {
		boolean isWord = false;
		// if see same name shaped word more than once, assume it is a name
		if (wordShapeSet.contains(aName)) {
			debugPrint("Dictionary check: " + aName, defaultLevel);
			return isWord;
		}
		String lcName = aName.toLowerCase();
		if (!lcName.equals(aName)) {
			if (this.checkRegex(aName)) {
				this.wordShapeSet.add(aName);
			}
		}
		isWord = wordlist.indexOf((lcName + " ")) > -1;
		if (isWord) {
			debugPrint("Dictionary Matches: " + lcName, defaultLevel);
		}

		return isWord;
	}

	boolean checkExclusions(String aName) {
		boolean isExcluded = false;

		isExcluded = excludedList.indexOf((aName + " ")) > -1;
		if (isExcluded) {
			debugPrint("Excluded Matches: " + aName, defaultLevel);
		}

		return isExcluded;
	}

	boolean checkNameList(String aName) {
		boolean isName = false;
		isName = namelist.indexOf(aName + " ") > -1;
		if (isName) {
			debugPrint("ProperName Matches: " + aName, defaultLevel);
		}

		return isName;
	}

	public NERAnalysis processInputs() throws Exception {

		// get the sentences to be parsed
		// parse out sentences using CoreNLP
		String fileContents = nerAnalysis.getNerInputText();
		List<List<CoreLabel>> out = classifier.classify(fileContents);
		for (List<CoreLabel> sentence : out) {
			String aSentence = sentence.toString();
			debugPrint(aSentence, defaultLevel);
			String str = aSentence;

			debugPrint("File Token: " + str, defaultLevel);

			// run test
			List<NamedEntityRetrievalResponse> namedEntityList;
			namedEntityList = checkStringForNameOrLocation(str);

			// display results
			if (namedEntityList.size() > 0) {
				debugPrint("String Input: " + str, alwaysPrint);
				debugPrint("", alwaysPrint);
			}

			for (NamedEntityRetrievalResponse resp : namedEntityList) {
				int lev = alwaysPrint;
				if (!resp.isOther()) {
					debugPrint("Has Entity: " + resp, lev);
				}
			}

			if (namedEntityList.size() > 0) {
				debugPrint("", alwaysPrint);
			}

		}

		return nerAnalysis;
	}

}
