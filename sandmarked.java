import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

////////////////////////////////////////////////////////////////////////////
//
//Code for HW1, Problem 2
//Inducing Decision Trees
//CS540 (Shavlik) - Epic
//Dustin Maiden : NedID : dmaiden
//
////////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////////
public class BuildAndTestDecisionTree {
	// Decision Tree Printing 
	//private static String printString = null;
	
	// Decision Tree Nodes
	public enum TreeNodeType {
		LEAF_NODE,
		FEATURE_NODE	
	}
	
	// Dataset constant pieces for scanning and reading
	public enum DataSetConstants {		
		FEATURES,LABLES,EXAMPLES, //important dataset pieces
		SKIP,COUNT			      //useful for scanning 
	}
////////////////////////////////////////////////////////////////////////////


	public static void main(String[] args) {
	    if (args.length != 2) {
	      System.err.println("You must call BuildAndTestDecisionTree as follows:\n\n" + 
				             "java BuildAndTestDecisionTree <trainsetFilename> <testsetFilename>\n");
	      System.exit(1);
	    }    

	    String trainset = args[0];
	    String testset  = args[1];

	    // Read in the examples from the files
	    ListOfExamples trainExamples = readData(trainset, "TrainSet");
	    ListOfExamples testExamples = readData(testset, "TestSet");

	    List<String> labels = trainExamples.outputLabels;    // labels from trainset
	    // List<String> labelstest = testExamples.outputLabels; // labels from testset
	    
	    // Build the decision tree using trainset 
	    // note that we pass in the first to be default
	    dTreeNode dtree = buildDTree(trainExamples.examples, trainExamples.features, labels, labels.get(0));
	    
	    // Print the decision tree
	    printDecisionTree(dtree, "");
	    
	    // Test the decision tree
	    System.out.println("\nTEST DATA SET");
	    testDecisionTree(dtree, testExamples.examples, "TestSet");
	}
//end of main	
	

	// A node in the decision tree
	public static class dTreeNode
	{
		// Examples & Features
		List<Example> examples = null;
		List<Feature> features = null;
		
		// Node feature
		Feature feature = null;
		
		//majority value for node
		String majority = null;

		// Defines node type {Feature node, Leaf Node} 
		// Will assume a feature by default
		TreeNodeType nodeType = TreeNodeType.FEATURE_NODE;
		
		//Binary Attributes
		// Left = First Feature
		dTreeNode leftNode = null;
		// Right = Second Feature 
		dTreeNode rightNode = null;
		
		public dTreeNode(List<Example> examples, List<Feature> features, Feature feature, String major)
		{
			this.examples 		= examples;
			this.features 		= features;
			this.feature  		= feature;
			this.majority  		= major;
		}
	}

	//Build D-TREE (trainset)->ID3 algorithm.
	public static dTreeNode buildDTree(List<Example> examples, List<Feature> features, List<String> outputLabels, String major)
	{
		String label1 = outputLabels.get(0);
		String label2 = outputLabels.get(1);

		// Without examples -> Leaf Node
		if(examples == null) {
			dTreeNode node = leafNode(examples, features, null, major);
			return node;
		}
		
		// If examples have same label, return that label
		Map<String, Integer> labelMap = getValDist(examples, outputLabels, null, null);
		int label1Count = labelMap.get(label1);
		int label2Count = labelMap.get(label2);
		
		if(label1Count == 0) {
			dTreeNode node = leafNode(examples, features, null, label2);
			return node;
		}
		if(label2Count == 0) {
			dTreeNode node = leafNode(examples, features, null, label1);
			return node;
		}
		
		// Determine the output label
		String majorLabel = null;
		if(label1Count > label2Count) {
			majorLabel = label1;
		}else if(label2Count > label1Count) {
			majorLabel = label2;
		}else{
			majorLabel = label1;
		}

		// If features is empty, the return the majority value of examples
		if(features.isEmpty()) {
			dTreeNode node = leafNode(examples, null, null, majorLabel);
			return node;
		}

		Feature topFeat = getTopFeat(examples, features, outputLabels);
		List<Feature> remFeat = getRemFeat(features, topFeat);
		dTreeNode root = new dTreeNode(examples, features, topFeat, majorLabel);

		// Attribute 1 -> Left
		List<Example> label1Examples = filterFeat(examples, topFeat, topFeat.firstValue);
		root.leftNode = buildDTree(label1Examples, remFeat, outputLabels, majorLabel);
		
		// Attribute 2 -> Right
		List<Example> label2Examples = filterFeat(examples, topFeat, topFeat.secondValue);
		root.rightNode = buildDTree(label2Examples, remFeat, outputLabels, majorLabel);
		
		return root;
	}

	//PRINT!
	private static void printDecisionTree(dTreeNode root, String buffer)
	{
		boolean isFeat = (root.nodeType == TreeNodeType.FEATURE_NODE);
		
		// Recursion
		if(isFeat) {
			Feature feature = root.feature;
			
			// Print the left subtree
			System.out.println(buffer + feature.name + " => " + feature.firstValue);
			printDecisionTree(root.leftNode, buffer + " ");
			
			// Print the right subtree
			System.out.println(buffer + feature.name + " => " + feature.secondValue);
			printDecisionTree(root.rightNode, buffer + " ");
		}
		// Leafs
		else {
			System.out.println(buffer + "patient("+root.majority+")");
		}
	}

	// Testing the Decision Tree
	private static void testDecisionTree(dTreeNode dtree, List<Example> examples, String nameOfDataSet)
	{
		int failureCount = 0;
		int exampleCount = examples.size();
		List<Example> failed = new ArrayList<Example>();
		
		System.out.println("Failed Examples : ");
		for(Example x : examples) {
			String expectedLabel = x.outputLabel;
			String actual = getLabel(x, dtree);
			if(actual.equals(expectedLabel)) {
				//do nothing
			}else{
				//count fails, add to list to print
				++failureCount;
				System.out.println(x.name);
				failed.add(x);
			}
		}
		
		//Calculation and accuracy/fraction output
		int numCorrect = exampleCount - failureCount;
		double accuracy = ((double)(numCorrect)*100)/exampleCount;
		System.out.println();
		System.out.println("Accuracy : " + accuracy + "%");
		System.out.println("Fraction :(" +  numCorrect + "/" + exampleCount + ")");
	}

	//Generates Output Label
	private static String getLabel(Example ex, dTreeNode dtree)
	{
		String Label = null;
		Map<Feature, String> featMap = ex.featureValMap;
		while(dtree != null && dtree.nodeType != TreeNodeType.LEAF_NODE) {
			Feature currFeature = dtree.feature;
			String currentFeat = featMap.get(currFeature);
			//printString = printString + "#" + currFeature.name;

			// first feature -> Left
			if(currFeature.firstValue.equals(currentFeat)) {
				dtree = dtree.leftNode;
			}
			// second feature -> Right
			else {
				dtree = dtree.rightNode;
			}
		}
		
		if(dtree != null) {
			Label = dtree.majority;
			//printString = "";
		}

		return Label;
	}

	// Returns examples with specified values
	private static List<Example> filterFeat(List<Example> examples, Feature feature, String featureVal)
	{
		List<Example> filteredExamples = new ArrayList<Example>();
		for(Example ex : examples) {
			// Apply the filter of a specific feature's value
			if(feature != null) {
				Map<Feature, String> featureValMap = ex.featureValMap;
				if(featureValMap.get(feature).equals(featureVal)) {
					filteredExamples.add(ex);
				}
			}
		}

		return filteredExamples;
	}

	// Returns new feature list containing all features minus best feature
	private static List<Feature> getRemFeat(List<Feature> features, Feature bestFeature)
	{
		List<Feature> remFeats = new ArrayList<Feature>();
		for(Feature f : features) {
			if(f.equals(bestFeature)) {
				//do nothing
			}
			else{
				remFeats.add(f);
			}
		}
		return remFeats;
	}

	// Creates a leaf node for decision tree
	private static dTreeNode leafNode(
			List<Example> examples, List<Feature> remFeatures, Feature currFeature, String majorVal)
	{
		dTreeNode leafNode = new dTreeNode(examples, remFeatures, currFeature, majorVal);
		leafNode.nodeType = TreeNodeType.LEAF_NODE;
		return leafNode;
	}
	
	//
	// Finds feature with most infogain. The idea is to choose a feature which leads to max
	// A tie should select the first feature
	// Calculate feature's remainder and choose the feature with minimum remainder.
	private static Feature getTopFeat(List<Example> examples, List<Feature> features, List<String> opLabels)
	{
		Feature topFeat = null;
		double remInfo = 9999999;
		
		String label1 = opLabels.get(0);
		String label2 = opLabels.get(1);

		int totalExamples = examples.size();
		for(Feature f : features) {
			Map<String, Integer> featMap = getFeatDist(examples, f);
			int count1 = featMap.get(f.firstValue);
			int count2 = featMap.get(f.secondValue);

			Map<String, Integer> value1Map = getValDist(examples, opLabels, f, f.firstValue);
			int pos1 = value1Map.get(label1);
			int neg1 = value1Map.get(label2);
			
			Map<String, Integer> value2Map = getValDist(examples, opLabels, f, f.secondValue);
			int pos2 = value2Map.get(label1);
			int neg2 = value2Map.get(label2);
			
			double infoGain1 = getInfoGain(count1, totalExamples, pos1, neg1);
			double infoGain2 = getInfoGain(count2, totalExamples, pos2, neg2);
			
			double infoGain = infoGain1 + infoGain2;
			
			int diff = Double.compare(infoGain, remInfo);
			if(diff < 0) {
				remInfo = infoGain;
				topFeat = f;
			}
			else if(diff == 0) {
				// choose earliest for ties
				if(f.name.compareTo(topFeat.name) < 0) {
					topFeat = f;
				}
			}
		}

		return topFeat;
	}

	// Information Gain
	private static double getInfoGain(double featCount, double totExamps, double posCnt, double negCnt)
	{
		//if(featureValCnt == 0.0 || (posCnt + negCnt) == 0.0) {
		//	return 0.0;
		//}

		double posInfoGain = 0.0;
		double negInfoGain = 0.0;
		if(posCnt != 0) {
			posInfoGain = (-1*(posCnt/featCount)*(Math.log(posCnt/featCount)/Math.log(2)));
		}
		if(negCnt != 0) {
			negInfoGain = (-1*(negCnt/featCount)*(Math.log(negCnt/featCount)/Math.log(2)));
		}
		double finalNum = (featCount/totExamps)*(posInfoGain + negInfoGain);
		return finalNum;
	}

    //Feature Values
	private static Map<String, Integer> getFeatDist(List<Example> examples, Feature feature)
	{
		Map<String, Integer> classifierMap = new HashMap<String, Integer>();
		
		classifierMap.put(feature.firstValue, 0);
		classifierMap.put(feature.secondValue, 0);
		
		for(Example e : examples) {
			String featureVal = e.featureValMap.get(feature);
			int count = classifierMap.get(featureVal) + 1;
			classifierMap.put(featureVal, count);
		}

		return classifierMap;
	}

	 //  Returns the number of examples for each label

	public static Map<String, Integer> getValDist(
			List<Example> examples, List<String> opLabels, Feature feature, String featureValFilter)
	{
		Map<String, Integer> classifierMap = new HashMap<String, Integer>();
		classifierMap.put(opLabels.get(0), 0);
		classifierMap.put(opLabels.get(1), 0);

		for(Example ex : examples) {
			// Apply the filter of a specific feature's value
			if(feature != null) {
				Map<Feature, String> featureValMap = ex.featureValMap;
				if(!featureValMap.get(feature).equals(featureValFilter)) {
					continue;
				}
			}

			String opLabel = ex.outputLabel;
			int count = 1;
			if(classifierMap.containsKey(opLabel)) {
				count = classifierMap.get(opLabel) + 1;
			}
			classifierMap.put(opLabel, count);
		}

		return classifierMap;
	}

    //Scanner - modified since HW0 as mine wasn't perfect
	public static ListOfExamples readData(String fileName, String nameOfDataSet)
	{
		List<Feature> features = new ArrayList<Feature>();
		Integer featureCount = null;
		
		List<Example> examples = new ArrayList<Example>();
		Integer exampleCount = null;

		List<String> opLabels = new ArrayList<String>();
		
		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(new File(fileName));
		} catch (Exception e) {
			System.err.println("Unable to read the file " + fileName + ". Exception : " + e.getMessage());
			System.exit(1);
		}
		while (fileScanner.hasNext()) {
			String currLine = fileScanner.nextLine().trim();
			DataSetConstants lineType = getLineType(currLine);
			
			// Skip for blank or empty lines
			if(lineType.equals(DataSetConstants.SKIP)) {
				continue;
			}
			
			// Populate feature count and example count values
			if(lineType.equals(DataSetConstants.COUNT)) {
				if(featureCount == null) {
					featureCount = Integer.parseInt(currLine);
				}
				else {
					exampleCount = Integer.parseInt(currLine);
				}
			}
			// Features
			else if(lineType.equals(DataSetConstants.FEATURES)) {
				features.add(extractFeature(currLine));
			}
			// Labels
			else if(lineType.equals(DataSetConstants.LABLES)) {
				opLabels.add(currLine);
			}
			// Examples
			else {
				examples.add(extractExample(currLine));
			}
			
		}
		
		// Pre-process examples to allow easier access of feature values
		for(Example e : examples) {
			List<String> featureValues = e.featureValues;
			Map<Feature, String> featureValMap = new HashMap<Feature, String>();
			for(int i=0; i < features.size(); i++) {
				featureValMap.put(features.get(i), featureValues.get(i));
			}
			
			e.featureValMap = featureValMap;
		}
		
		return new ListOfExamples(nameOfDataSet, features, featureCount, opLabels, examples, exampleCount);
	}

	//Line type and current line used in reading
	private static DataSetConstants getLineType(String line)
	{
		DataSetConstants lineType = null;
		if(line.isEmpty() || line.startsWith("//")) {
			lineType = DataSetConstants.SKIP;
		}
		else if(line.contains("-")) {
			lineType = DataSetConstants.FEATURES;
		}
		else if(line.matches("[0-9]+")) {
			lineType = DataSetConstants.COUNT;
		}
		else {
			String[] words = line.split("[\\s\\t]+");
			if(words.length == 1) {
				lineType = DataSetConstants.LABLES;
			}
			else {
				lineType = DataSetConstants.EXAMPLES;	
			}
		}
		
		return lineType;
	}

	// Extracts a feature object from dataset line
	private static Feature extractFeature(String line)
	{
		int lastDashIndex = line.lastIndexOf("-");
		String beforeFeatureVal = line.substring(0, lastDashIndex);
		String afterFeatureVal = line.substring(lastDashIndex+1);
		
		String featureName = beforeFeatureVal.trim();
		String[] featureValues = afterFeatureVal.trim().split(" ");
		String firstValue = featureValues[0].trim();
		String secondValue = featureValues[1].trim();

		return new Feature(featureName, firstValue, secondValue);
	}

	// Extracts an example object from a dataset line
	private static Example extractExample(String line)
	{
		String[] words = line.split("[\\s\\t]+");
		
		String exampleName = words[0].trim();
		String outputLabel = words[1].trim();
		
		List<String> featureValues = new ArrayList<String>();
		for(int i=2; i < words.length; i++) {
			featureValues.add(words[i].trim());
		}
		
		return new Example(exampleName, outputLabel, featureValues);
	}

    //Binary features
	public static class Feature 
	{
		private String name;
		private String firstValue;
		private String secondValue;
		
		public Feature(String name, String firstVal, String secondVal)
		{
			this.name = name;
			this.firstValue = firstVal;
			this.secondValue = secondVal;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((firstValue == null) ? 0 : firstValue.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result
					+ ((secondValue == null) ? 0 : secondValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Feature other = (Feature) obj;
			if (firstValue == null) {
				if (other.firstValue != null)
					return false;
			} else if (!firstValue.equals(other.firstValue))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (secondValue == null) {
				if (other.secondValue != null)
					return false;
			} else if (!secondValue.equals(other.secondValue))
				return false;
			return true;
		}
	}

	//Example information
	public static class Example 
	{
		private String name;
		private String outputLabel;
		private List<String> featureValues;
		private Map<Feature, String> featureValMap = null;
		
		public Example(String name, String outputLabel, List<String> featureValues) 
		{
			this.name = name;
			this.outputLabel = outputLabel;
			this.featureValues = featureValues;
		}
	}

	//dataset information
	public static class ListOfExamples
	{
		//private String nameOfDataset = "";
		
		// dataset features info
		//private int numFeatures = -1;
		private List<Feature> features = null;
		
		// dataset examples info
		//private int numExamples = -1;
		private List<Example> examples = null;
		
		private List<String> outputLabels = null;

		public ListOfExamples(String name, List<Feature> features, int numFeatures, List<String> opLabels, 
				List<Example> examples, int numExamples)
		{
			//this.nameOfDataset = name;
			this.features = features;
			//this.numFeatures = numFeatures;
			this.outputLabels = opLabels;
			this.examples = examples;
			//this.numExamples = numExamples;
		}	
	}

}