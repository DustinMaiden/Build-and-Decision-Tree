import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
	static int counterprint = 0;
	
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
	    printDaTree(dtree, "");
	    
	    // Test the decision tree
	    System.out.println("\nTEST DATA SET");
	    testDTree(dtree, testExamples.examples, "TestSet");
	}
//end of main	
	

	// A node in the decision tree
public static class dTreeNode
{
// Defines node type {Feature node, Leaf Node} 
// Will assume a feature by default
	TreeNodeType nodeType = TreeNodeType.FEATURE_NODE;
		
// Examples & Features
	List<Example> examps = null;
	List<Feature> feats = null;
		
// Node feature
	Feature feature = null;
		
//majority value for node
	String majority = null;
	
//Binary Attributes
// Left = First Feature
	dTreeNode leftNode = null;
// Right = Second Feature 
	dTreeNode rightNode = null;
		
	public dTreeNode(List<Example> examples, List<Feature> features, Feature feature, String major)
		{
		this.examps 	= examples;
		this.feats 		= features;
		this.feature  	= feature;
		this.majority  	= major;
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
		
		// Determine the output label using simple comparisons
		String majorLabel = null;
		if(label1Count > label2Count) {majorLabel = label1;
		}else if(label2Count > label1Count) {majorLabel = label2;
		}else{majorLabel = label1;
		}

		//while we have features - continue building
		if(!features.isEmpty()) {
		  Feature topFeat = getTopFeat(examples, features, outputLabels);
		  dTreeNode root = new dTreeNode(examples, features, topFeat, majorLabel);
		  List<Feature> remFeat = getRemFeat(features, topFeat);
			
			// Attribute 1 -> Left
			List<Example> label1Examples = filterFeat(examples, topFeat, topFeat.first);
			root.leftNode = buildDTree(label1Examples, remFeat, outputLabels, majorLabel);
			
			// Attribute 2 -> Right
			List<Example> label2Examples = filterFeat(examples, topFeat, topFeat.second);
			root.rightNode = buildDTree(label2Examples, remFeat, outputLabels, majorLabel);
			return root;
		}
		// empty features -> return majority
		else{
			dTreeNode node = leafNode(examples, null, null, majorLabel);
			return node;
	}
	}

	// Testing the Decision Tree
	private static void testDTree(dTreeNode dtree, List<Example> examples, String nameOfDataSet)
	{
		int failureCount = 0;
		int exampleCount = examples.size();
		
		System.out.println("Failed Examples : ");
		for(Example x : examples) {
		  String actual = getLabel(x, dtree);
		  String expectedLabel = x.label;

		  if(actual.equals(expectedLabel)) {
			 //do nothing
			}else{
			 //count fails, print example
			  failureCount++;
			  System.out.println("Example failed : "+x.name);
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
	@SuppressWarnings("unused")
	private static String getLabel(Example ex, dTreeNode dtree)
	{
		String Label = null;
		Map<Feature, String> featMap = ex.featureValMap;
		
		while(dtree.nodeType != TreeNodeType.LEAF_NODE) {
			
			Feature currFeature = dtree.feature;
			String currentFeat = featMap.get(currFeature);

			// GO Right
			if(currFeature.second.equals(currentFeat)) {
			  dtree = dtree.rightNode;
			}
			// GO Left
			else {
			  dtree = dtree.leftNode;
			}
		}
		
		if(dtree == null) {
			//do nothing
		}else{
			Label = dtree.majority;
		}
		return Label;
	}

	// Returns examples with specified values
	private static List<Example> filterFeat(List<Example> examples, Feature feature, String featureVal)
	{
		List<Example> filteredExamples = new ArrayList<Example>();
		
		for(Example ex : examples) {
			// Apply the filter of a specific feature's value
			if(feature == null) {
				//do nothing
			}
			else{
				Map<Feature, String> featMap = ex.featureValMap;
				if(featMap.get(feature).equals(featureVal)) {
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
	private static dTreeNode leafNode(List<Example> examples, List<Feature> remFeatures, Feature currFeature, String majorVal)
	{
		dTreeNode leafNode = new dTreeNode(examples, remFeatures, currFeature, majorVal);
		leafNode.nodeType = TreeNodeType.LEAF_NODE;
		return leafNode;
	}
	
	// Finds feature with most infogain. The idea is to choose a feature which leads to max
	// A tie should select the first feature
	// Calculate feature's remainder and choose the feature with minimum remainder.
	private static Feature getTopFeat(List<Example> examples, List<Feature> features, List<String> opLabels)
	{
		Feature topFeat = null;
		double remInfo = 9999999;
		
		String label1 = opLabels.get(0);
		String label2 = opLabels.get(1);

		for(Feature f : features) {
			Map<String, Integer> featMap = getFeatDist(examples, f);
			int count1 = featMap.get(f.first);
			int count2 = featMap.get(f.second);

			Map<String, Integer> value1Map = getValDist(examples, opLabels, f, f.first);
			int pos1 = value1Map.get(label1);
			int neg1 = value1Map.get(label2);
			
			Map<String, Integer> value2Map = getValDist(examples, opLabels, f, f.second);
			int pos2 = value2Map.get(label1);
			int neg2 = value2Map.get(label2);
			
			double infoGain1 = getInfoGain(count1, examples.size(), pos1, neg1);
			double infoGain2 = getInfoGain(count2, examples.size(), pos2, neg2);
			double infoGain = infoGain1 + infoGain2;
			
			int diff = Double.compare(infoGain, remInfo);
			
			if(diff == 0) {
			  topFeat = f;
			}
			else if(diff < 0) {
			  topFeat = f;
			  remInfo = infoGain;
			}
		}
		return topFeat;
	}

	// Information Gain
	private static double getInfoGain(double featCount, double totExamps, double posCnt, double negCnt)
	{
		double posInfoGain = 0.0;
		double negInfoGain = 0.0;
		if(posCnt != 0) {posInfoGain = (-1*(posCnt/featCount)*(Math.log(posCnt/featCount)/Math.log(2)));
		}
		if(negCnt != 0) {negInfoGain = (-1*(negCnt/featCount)*(Math.log(negCnt/featCount)/Math.log(2)));
		}
		
		double finalNum = (featCount/totExamps) * (posInfoGain + negInfoGain);
		return finalNum;
	}

    //Feature Values
	private static Map<String, Integer> getFeatDist(List<Example> examples, Feature feature)
	{
		Map<String, Integer> classMap = new LinkedHashMap<String, Integer>();
		
		classMap.put(feature.first, 0);
		classMap.put(feature.second, 0);
		
		for(Example e : examples) {
		  String featValue = e.featureValMap.get(feature);
		  int count = classMap.get(featValue);
		  count++;
		  classMap.put(featValue, count);
		}
		return classMap;
	}

	 //  Returns the number of examples for each label
	public static Map<String, Integer> getValDist(List<Example> examples, List<String> opLabels, Feature feature, String featureValFilter)
	{
		Map<String, Integer> classMap = new LinkedHashMap<String, Integer>();
		
		classMap.put(opLabels.get(0), 0);
		classMap.put(opLabels.get(1), 0);

		for(Example ex : examples) {
			// Apply the filter of a specific feature's value
			if(feature == null) {
				//do nothing
			}
			else{
				Map<Feature, String> featureValMap = ex.featureValMap;
				if(!featureValMap.get(feature).equals(featureValFilter)) {
					continue;
				}
			}

			String labeltable = ex.label;
			int count = 0;
			if(classMap.containsKey(labeltable)) {
			  count = classMap.get(labeltable) + 2;
			}
			classMap.put(labeltable, count);
		}
		return classMap;
	}

    //Scanner - modified since HW0 as mine wasn't perfect
	public static ListOfExamples readData(String fileName, String nameOfDataSet)
	{
		Integer featureCount = 0;
		Integer exampleCount = 0;
		
		List<Example> examples = new ArrayList<Example>();
		List<String> labels = new ArrayList<String>();
		List<Feature> features = new ArrayList<Feature>();

		Scanner fileScanner = null;
		try {
			fileScanner = new Scanner(new File(fileName));
		} catch (Exception e) {
			System.err.println("Unable to read the file ");
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
				if(featureCount != null) {
				  exampleCount = Integer.parseInt(currLine);
				}else {
				  featureCount = Integer.parseInt(currLine);
				}
			}
			// Features
			else if(lineType.equals(DataSetConstants.FEATURES)) {
				features.add(extractFeature(currLine));
			}
			// Labels
			else if(lineType.equals(DataSetConstants.LABLES)) {
				labels.add(currLine);
			}
			// Examples
			else {
				examples.add(extractExample(currLine));
			}	
		}
		
		// Pre-process examples to allow easier access of feature values
		for(Example e : examples) {
			List<String> featureValues = e.featureValues;
			Map<Feature, String> featureValMap = new LinkedHashMap<Feature, String>();
			
			for(int i=0; i < features.size(); i++) {
			  featureValMap.put(features.get(i), featureValues.get(i));
			}
			
			e.featureValMap = featureValMap;
		}
		return new ListOfExamples(nameOfDataSet, features, featureCount, labels, examples, exampleCount);
	}

	//Line type and current line used in reading
	private static DataSetConstants getLineType(String line)
	{
		DataSetConstants lineType = null;
		if(line.startsWith("//") || line.isEmpty()) {
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
			}else {
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
		
		String featureName = beforeFeatureVal;
		String[] featureValues = afterFeatureVal.trim().split(" ");
		String firstValue = featureValues[0];
		String secondValue = featureValues[1];

		return new Feature(featureName, firstValue, secondValue);
	}
	
	// Extracts an example object from a dataset line
	private static Example extractExample(String line)
	{
		String[] words = line.split("[\\s\\t]+");
		
		String exampleName = words[0];
		String outputLabel = words[1];
		
		List<String> featureValues = new ArrayList<String>();
		
		for(int i=2; i < words.length; i++) {
		  featureValues.add(words[i]);
		}
		return new Example(exampleName, outputLabel, featureValues);
	}
	
    //Binary features
	public static class Feature 
	{
		private String name;
		private String first;
		private String second;
		
		public Feature(String name, String firstVal, String secondVal)
		{
			this.name = name;
			this.first = firstVal;
			this.second = secondVal;
		}
		
		//Object overrides for equals and hashcode
		@Override
		public boolean equals(Object o) {
			if (this == o)return true;
			if (o == null)return false;
			if (getClass() != o.getClass())return false;
			
			Feature other = (Feature) o;
			
			if (first == null) {
				if (other.first != null)return false;
			}   else if (!first.equals(other.first))return false;
			if (name == null) {
				if (other.name != null)return false;
			}   else if (!name.equals(other.name))return false;
			if (second == null) {
				if (other.second != null)return false;
			}   else if (!second.equals(other.second))return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			final int prime = 17;
			int result = 1;
			
			 if(first==null){
			 result = prime * result + 0;
			 }else{
			 result = first.hashCode() + prime + result;
			 }
			 if(second==null){
			 result = prime * result + 0;
			 } else{
			 result = second.hashCode() + prime + result;
			 }
			 if(name==null){
			 result = prime * result + 0;
			 }else{
			 result = name.hashCode() + prime + result;
			 }
			 return result;
		}	
	}

	//Example information
	public static class Example 
	{
		private String name;
		private String label;
		private List<String> featureValues;
		private Map<Feature, String> featureValMap = null;
		
		public Example(String name, String label, List<String> featureValues) 
		{
			this.name = name;
			this.label = label;
			this.featureValues = featureValues;
		}
	}

	
	//dataset information
	public static class ListOfExamples
	{		
		// dataset features info
		private List<Feature> features = null;
		
		// dataset examples info
		private List<Example> examples = null;
		private List<String> outputLabels = null;

		//List of examples
		public ListOfExamples(String name, List<Feature> features, int numFeatures, List<String> labels, List<Example> examples, int numExamples)
		{
		  this.features = features;
		  this.examples = examples;
		  this.outputLabels = labels;
			
		}	
	}

/////PRINT!///////////////////////////////////////////////////////////////////////////////////
		private static void printDaTree(dTreeNode root, String buffer)
		{
			//just to show where the tree is (not that it's obvious or anything :)
			if(counterprint==0){System.out.println("Tree :");}
			   counterprint++;
			boolean isFeat = (root.nodeType == TreeNodeType.FEATURE_NODE);
			
			// Recursion
			if(isFeat) {
			  Feature feature = root.feature;
				
			  // Print the left subtree
			  System.out.println(buffer + feature.name + " => " + feature.first);
			  printDaTree(root.leftNode, buffer + " ");
				
			  // Print the right subtree
			  System.out.println(buffer + feature.name + " => " + feature.second);
			  printDaTree(root.rightNode, buffer + " ");
			}
			// Leafs
			else {
				System.out.println(buffer + "patient("+root.majority+")");
			}
		}
/////PRINT!///////////////////////////////////////////////////////////////////////////////////	
}