import java.util.Vector;
import java.io.*;


// the Reader class contains data structures and methods
// for opening, reading, and closing a file for input
class Reader
{
    BufferedReader stream;
    String buffer;

    public Reader()
    {
	buffer = "";
    }

    public boolean open(String filename)
    {
	try { 
	    stream = new BufferedReader(new FileReader(filename));
	} catch (IOException e) { 
	    return false;  
	}
	return true;
    }

    public boolean ready()
    {
	try {
	    stream.ready();
	} catch (IOException e) {
	    return false;
	}
	try {
	    buffer = stream.readLine();
	} catch (IOException e) {
	    buffer = "";
	    return false;
	}
	if(buffer == null)
	    return false;
	return true;	
    }

    public String getLine()
    {
	return buffer;
    }

    public void closeReader()
    {
	try {
	    stream.close();
	} catch (IOException e) {}
    }


} // end of class Reader

// the Node class contains data fields for node data
// and methods for setting and retreiving the data
class Node
{
    private String nodeID;
    private String nodeType;
    private String typeID;
    private String name;
    private String successors;
    private String sourceLine;
    private String bytecodeOffset;
    private String nodeAttributes;
    private String edgeAttributes;


    public Node()
    {
	nodeID = "undefined";
	nodeType = "undefined";
	typeID = "undefined";
	name = "undefined";
	successors = "";
	sourceLine = "undefined";
	bytecodeOffset = "undefined";
	nodeAttributes = "";
	edgeAttributes = "";
    }

    public void SetNodeID(String val)
    {
	nodeID = val;
    }

    public void SetNodeType(String val)
    {
	nodeType = val;
    }

    public void SetTypeID(String val)
    {
	typeID = val;
    }

    public void SetName(String val)
    {
	name = val;
    }

    public void AddSuccessor(String val)
    {
	if(successors.length() == 0)
	    successors = val;
	else
	    successors = successors + "; " + val;
    }

    public void SetSourceLine(String val)
    {
	sourceLine = val;
    }

    public void SetBytecodeOffset(String val)
    {
	bytecodeOffset = val;
    }

    public void AddNodeAttribute(String val)
    {
	if(nodeAttributes.length() == 0)
	    nodeAttributes = val;
	else
	    nodeAttributes = nodeAttributes + "<br>" + val;
    }

    public void AddEdgeAttribute(String val)
    {
	if(edgeAttributes.length() == 0)
	    edgeAttributes = val;
	else
	    edgeAttributes = edgeAttributes + val;
    }

    public String GetNodeID()
    {
	return nodeID;
    }

    public String GetNodeType()
    {
	return nodeType;
    }

    public String GetTypeID()
    {
	return typeID;
    }

    public String GetName()
    {
	return name;
    }

    public String GetSuccessors()
    {
	return successors;
    }

    public String GetSourceLine()
    {
	return sourceLine;
    }

    public String GetBytecodeOffset()
    {
	return bytecodeOffset;
    }

    public String GetNodeAttributes()
    {
	return nodeAttributes;
    }

    public String GetEdgeAttributes()
    {
	return edgeAttributes;
    }
} // end of class Node


// this class contains data fields for graph data
// and methods for setting and retreiving the data
class Graph
{
    private String name;
    private String type;
    private Vector nodes;
    private int index;

    public Graph()
    {
	name = "undefined";
	type = "undefined";
	nodes = new Vector();
	index = 0;
    }

    public void SetName(String val)
    {
	name = val;
    }

    public void SetType(String val)
    {
	type = val;
    }

    public void AddNode(Node newNode)
    {
	nodes.addElement(newNode);
    }

    public void AddEdge(String source, String sink, String attributes)
    {
	Node n;
	for(int i = 0; i < nodes.size(); i++)
	    {
		n = (Node)nodes.elementAt(i);
		if(n.GetNodeID().equalsIgnoreCase(source))
		    {
			n.AddSuccessor(sink);
			n.AddEdgeAttribute(attributes);
		    }
	    }
    }

    public String GetName()
    {
	return name;
    }

    public String GetType()
    {
	return type;
    }

    public boolean MoreNodes()
    {
	return (index < nodes.size());
    }

    public Node GetNode()
    {
	// note that, though we have to cast the object in the Vector to type
	// Node, there should never be any other type of object in this Vector
	return (Node)nodes.elementAt(index++);
    }
} // end of class Graph




public class jabaGraph
{

    public static void WriteUsage()
    {
	System.out.println("Usage: [-s|-d] inputFile [outputFile]");
    }

    public static int DetermineType(String buffer)
    {
	if(buffer.substring(buffer.indexOf('<') + 1,
			    buffer.indexOf('>')).equalsIgnoreCase("cfgs"))
	    return 1;
	if(buffer.substring(buffer.indexOf('<') + 1,
			    buffer.indexOf('>')).equalsIgnoreCase("icfg"))
	    return 2;
	if(buffer.substring(buffer.indexOf('<') + 1, buffer.indexOf('>')).
	   equalsIgnoreCase("classhierarchygraph"))
	    return 3;
	return 0;
    }

    public static void ProcCFGs(Reader input, Vector graphs)
    {
	ProcICFGnCHG(input, graphs, true, 1);

	// for each possible CFG, read ahead 2 lines... if <cfg> then
	// get the new CFG, else we already got the last one
	input.ready();
	input.ready();

	while(input.getLine().indexOf("<cfg>") != -1)
	    {
		ProcICFGnCHG(input, graphs, true, 1);
		input.ready();
		input.ready();
	    }
    }

    public static void ProcICFGnCHG(Reader input, Vector graphs, 
				    boolean isICFG, int type)
    {
	Graph g = new Graph();
	Node n;

	if(type == 1)
	    g.SetType("CFG");
	else if(type == 2)
	    g.SetType("ICFG");
	else
	    g.SetType("CHG");

	// advance to the first line with <node>
	while(input.getLine().indexOf("<node>") == -1)
	    input.ready();

	n = ProcessNode(input.getLine(), isICFG);
	g.AddNode(n);

	while((input.ready()) && (input.getLine().indexOf("<node>") != -1))
	    {
	      	n = ProcessNode(input.getLine(), isICFG);
		g.AddNode(n);
	    }

	graphs.addElement(g);

	// advance to the first line with <edge>
	while(input.getLine().indexOf("<edge>") == -1)
	    input.ready();

	g.AddEdge(getSource(input.getLine()),
		  getSink(input.getLine()) +
		  getLabel(input.getLine()), 
		  getEdgeAttribute(input.getLine(), getSink(input.getLine())));

	while((input.ready()) && (input.getLine().indexOf("<edge>") != -1))
	g.AddEdge(getSource(input.getLine()),
		  getSink(input.getLine()) +
		  getLabel(input.getLine()), 
		  getEdgeAttribute(input.getLine(), getSink(input.getLine())));
    }

    public static Node ProcessNode(String buffer, boolean allFields)
    {
	Node newNode = new Node();
	int front, back;
	
	// get the nodeID
	front = buffer.indexOf("<nodenumber>") + 12;
	back = buffer.indexOf("</nodenumber>");
	newNode.SetNodeID(buffer.substring(front, back));

	// get the nodeType
	front = buffer.indexOf("<type>") + 6;
	back = buffer.indexOf("</type>");
	newNode.SetNodeType(buffer.substring(front, back));

	if(allFields)
	    {
 	       	// get the sourceLine
		front = buffer.indexOf("<sourceline>") + 12;
		back = buffer.indexOf("</sourceline>");
		newNode.SetSourceLine(buffer.substring(front, back));

 	       	// get the bytecodeOffset
		front = buffer.indexOf("<bytecodeoffset>") + 16;
		back = buffer.indexOf("</bytecodeoffset>");
		newNode.SetBytecodeOffset(buffer.substring(front, back));

		// get the attributes
		front = 0;
		while(buffer.indexOf("<attribute>", front) != -1)
		    {
			front = buffer.indexOf("<attribute>", front) + 11;
			back = buffer.indexOf("</attribute>", front);
			newNode.AddNodeAttribute(getNodeAttribute(buffer.substring(front, back)));
			front = back;
		    }

	    }
	else
	    {
 	       	// get the typeID
		front = buffer.indexOf("<typeid>") + 8;
		back = buffer.indexOf("</typeid>");
		newNode.SetTypeID(buffer.substring(front, back));

 	       	// get the bytecodeOffset
		front = buffer.indexOf("<name>") + 6;
		back = buffer.indexOf("</name>");
		newNode.SetName(buffer.substring(front, back));
	    }

	return newNode;
    }

    public static String getSource(String buffer)
    {
	int front, back;
	front = buffer.indexOf("<source>") + 8;
	back = buffer.indexOf("</source>");
	return buffer.substring(front, back);
    }

    public static String getSink(String buffer)
    {
	int front, back;
	front = buffer.indexOf("<sink>") + 6;
	back = buffer.indexOf("</sink>");
	return buffer.substring(front, back);
    }

    public static String getLabel(String buffer)
    {
	int front, back;
	String label;
	front = buffer.indexOf("label>") + 6;
	back = buffer.indexOf("</", front);
	label = buffer.substring(front, back);

	if(label.equalsIgnoreCase("null"))
	    return "";
	else
	    return " (" + label + ")";
    }

    public static String getNodeAttribute(String buffer)
    {
	int front, back;

	if(buffer.indexOf("<callablemethod>") != -1)
	    {
		front = 37;
		back = buffer.indexOf("</", front);
		return ("Callable Method: " + buffer.substring(front, back));
	    }
	if(buffer.indexOf("<newinstance><type>") != -1)
	    {
		front = 19;
		back = buffer.indexOf("</", front);
		return ("New Instance Type: " + buffer.substring(front, back));
	    }
	if(buffer.indexOf("<exceptionattribute><class>") != -1)
	    {
		front = 27;
		back = buffer.indexOf("</", front);
		return ("Exception Class: " + buffer.substring(front, back));
	    }
	if(buffer.indexOf("<returnsitenodenumber>") != -1)
	    {
		front = 43;
		back = buffer.indexOf("</", front);
		return ("Return Site NodeID: " +
			buffer.substring(front, back));
	    }
	return "";
    }

    public static String getEdgeAttribute(String buffer, String NodeID)
    {
	int front, back;
	String attributes = "";

	// get the attributes
	if(buffer.indexOf("<attribute>") != -1)
	    {
		if(buffer.indexOf("<callnodenumber>") != -1)
		    {
			front = buffer.indexOf("<callnodenumber>") + 16;
			back = buffer.indexOf("</", front);
			attributes = attributes +
			    "&nbsp;&nbsp;&nbsp;- Call NodeID: " +
			    buffer.substring(front, back) + "<br>";
		    }
		if(buffer.indexOf("<traversaltype>") != -1)
		    {
			front = buffer.indexOf("<traversaltype>") + 15;
			back = buffer.indexOf("</", front);
			attributes = attributes +
			    "&nbsp;&nbsp;&nbsp;- Traversal Type: " +
			    buffer.substring(front, back) + "<br>";
		    }
	    }

	if(attributes.length() > 0)
	    attributes = "On Edge to Node " + NodeID + ":<br>" + attributes;
	
	return attributes;
    }

    public static void main(String args[])
    {
	boolean simpleDisplay = true;
	String inputFilename, outputFilename;
	Reader input;
	String buffer = "";
	int type;
	Vector graphs = new Vector();

	// parse the args
	if(args.length == 1)
	    {
		inputFilename = args[0];
		outputFilename = args[0] + ".html";
	    }
	else if(args.length == 2)
	    {
		if(args[0].equalsIgnoreCase("-s"))
		    {
			inputFilename = args[1];
			outputFilename = args[1] + ".html";
		    }
		else if(args[0].equalsIgnoreCase("-d"))
		    {
			simpleDisplay = false;
			inputFilename = args[1];
			outputFilename = args[1] + ".html";
		    }
		else
		    {
			inputFilename = args[0];
			outputFilename = args[1];
		    }
	    }
	else if(args.length == 3)
	    {
		if(args[0].equalsIgnoreCase("-d"))
		    simpleDisplay = false;

		inputFilename = args[1];
		outputFilename = args[2];
	    }
	else
	    {
		WriteUsage();
		System.exit(0);
		inputFilename = outputFilename = "";
	    }

	input = new Reader();
	if(!input.open(inputFilename))
	    {
		System.out.println("Invalid input filename: " + inputFilename);
		WriteUsage();
		System.exit(0);
	    }
	
       	if(input.ready())
	    {
		type = DetermineType(input.getLine());
	    }
	else
	    type = 0;

	switch(type)
	    {
	    case 1:
		System.out.print("Processing CFGs...");
		ProcCFGs(input, graphs);
		break;
	    case 2:
		System.out.print("Processing ICFG...");
		ProcICFGnCHG(input, graphs, true, 2);
		break;
	    case 3:
		System.out.print("Processing CHG...");
		ProcICFGnCHG(input, graphs, false, 3);
		break;
	    default:
		System.out.println("Unable to process file " + inputFilename);
	    }
	
	System.out.println("complete");


/* start */

	Writer output = new Writer();
	output.runWriter( graphs, outputFilename, simpleDisplay );

/* end */

	
    }
    
}
