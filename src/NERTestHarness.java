public class NERTestHarness extends NameTester {
	
	static void processStanfordNer(String filename) throws Exception {
		StanfordNerAnalyzer sna = new StanfordNerAnalyzer(filename);
		NERAnalysis nerAnalysis = sna.processInputs();
		NERAnalysis.printTestResults(nerAnalysis);
	}
	
	public static void main(String[] args) {
		String filename;
		if (args.length > 0) {
			filename = args[0];
		} else {
			filename = "MyTestFile.txt";
		}

		try {
			processStanfordNer(filename);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
