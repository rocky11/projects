
/**
 * Client Application:
 */

import java.io.*;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import net.jxta.document.Element;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.document.StructuredDocumentFactory;
import net.jxta.document.TextElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupFactory;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
//newly added
import net.jxta.util.JxtaBiDiPipe;
import net.jxta.endpoint.MessageElement;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import java.util.Date;
import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.protocol.DiscoveryResponseMsg;
import net.jxta.protocol.PeerAdvertisement;
import java.util.Vector;
import net.jxta.impl.protocol.*;
import net.jxta.impl.resolver.*;
import net.jxta.resolver.*;
import java.text.SimpleDateFormat;
import net.jxta.resolver.QueryHandler;
import net.jxta.protocol.ResolverQueryMsg;
import net.jxta.protocol.ResolverResponseMsg;


 //================================ Class: ServerList ==================

class ServerList
    {
    	private PipeAdvertisement	pAdv;
	private float			version;
	private int			numHop;
	private long			timeDiff;

	public ServerList(PipeAdvertisement p, float ver, int hopNum, long time) {
		pAdv = p;
		version = ver;
		numHop = hopNum;
		timeDiff = time;
	}

	public float getVersionNum() {
		return version;
	}

	public PipeAdvertisement getPipeAdv(){
		return pAdv;
	}

	public float getNumHop(){
		return numHop;
	}
	public float getTimeDiff(){
		return timeDiff;
	}
  };

//================================ End - Class: ServerList ==================

public class Client implements PipeMsgListener, DiscoveryListener {

    static PeerGroup netPeerGroup = null;
    static PeerGroupAdvertisement groupAdvertisement = null;
    private DiscoveryService discovery;
    private JxtaBiDiPipe pipe;
    private PipeService pipes;
    private OutputPipe myPipe; // Output pipe to connect the service
    private Message msg;
    private PipeAdvertisement pipeAdv = null;
    private final static String completeLock = "completeLock";
    private int count = 0;
    private int ServersResponded = 0;
    private final int numExpectedResponses = 1;
    private Vector ServerPipeIDList = new Vector ();
    protected SimpleDateFormat format   = new SimpleDateFormat("MM, dd, yyyy hh:mm:ss.S");
    protected StructuredTextDocument credential = (StructuredTextDocument) StructuredDocumentFactory.newStructuredDocument( new MimeMediaType("text", "xml"),"Client");
    protected String handlerName = "MicroQueryHandler";
    private long nowClientTime = 0; //time sent in resolve query
    public enum SortType { VERSION, WEIGHTED_SUM };
    private boolean Resolved = false;

    private String fileToGet = null;
    private int numServersToWaitOn = 0;
    private int milliSecServerWaitTime = 0;
    private int minVersionNumber = 0;
    private int maxHopCount = 0;
    private int maxPingTime = 0;

    public static void main(String args[]) {
        Client myapp = new Client();
        System.out.println ("Starting Client peer ....");
       	myapp.startJxta();

	// Now select the best Server(s), and connect to them.
	if ( myapp.ServerPipeIDList.size () > 0 )
	{
		myapp.selectServer ();
		myapp.establishBiDiPipe (myapp);
	}

	System.out.println ("Good Bye ....");
        System.exit(0);
    }

    private void startJxta() {
        try {
            // create, and Start the default jxta NetPeerGroup
            netPeerGroup = PeerGroupFactory.newNetPeerGroup();
        } catch (PeerGroupException e) {
            // could not instantiate the group, print the stack and exit
            System.out.println("fatal error : group creation failure");
            e.printStackTrace();
            System.exit(1);
        }

	SearchFile ( "FileMain", 8, 45*1000, 1, 5, 2000 );
    }

    // start the client
    public void SearchFile ( String fileName, int numServers, int milliSecWaitTime, int versionNumber, int hopCount, int pingTime )
    {
        // this is how to obtain the group advertisement
        groupAdvertisement = netPeerGroup.getPeerGroupAdvertisement();
        // get the discovery, and pipe service
        System.out.println("Getting DiscoveryService");
        discovery = netPeerGroup.getDiscoveryService();
        System.out.println("Getting PipeService");
        pipes = netPeerGroup.getPipeService();

	fileToGet = fileName;
	numServersToWaitOn = numServers;
	milliSecServerWaitTime = milliSecWaitTime;
	minVersionNumber = versionNumber;
	maxHopCount = hopCount;
	maxPingTime = pingTime;

	microTest();

        // Let's initialize the client
        System.out.println("Start the Client");

        // Let's try to locate the service advertisement
        // we will loop until we find it!
	String fileStr = "JXTA-" + fileName;

        System.out.println("searching for " + fileStr + " Service advertisement");
	try {
		discovery.addDiscoveryListener(this);
        	//Enumeration en = null;
		int milliSecSleepCount = 500; // Nyquist criterion: If we want to sample accurately at 1 sec., try <= 0.5 sec.

        	// remote discovery request searching for the service advertisement.
		if ( milliSecWaitTime < milliSecSleepCount )
			milliSecWaitTime = milliSecSleepCount;

		int loopCount = milliSecWaitTime/ milliSecSleepCount;
		String tag = "JXTASPEC:" + fileStr;
		System.out.println ( "Looping " + loopCount + " times, looking for tag: " + tag );
		long startTime = System.currentTimeMillis(); //current time
		long prevTime = startTime;

                	// The discovery is asynchronous as we do not know
                	// how long is going to take
                try { // sleep as much as we want. Yes we
                    	// should implement asynchronous listener pipe...
                    	for ( int i = 0; i < loopCount; i ++ )
		    	{
		               	discovery.getRemoteAdvertisements(null, DiscoveryService.ADV, "Name", tag, numExpectedResponses, null);
                    		Thread.sleep(milliSecSleepCount);
				long curTime = System.currentTimeMillis(); //current time
				long diffTime = curTime - prevTime;
				
				if ( diffTime >= 1000 )
				{
					System.out.println ( "@@@@@ # of servers Responded in " + ((curTime-startTime)/1000) + " seconds: " + ServersResponded + " @@@@@" );
					prevTime = curTime;
				}

		    		if ( ServersResponded > numServers )
		    			break;
		    	}

                    } catch (Exception e) {}

	}
        catch(Exception e) {
            e.printStackTrace();
        }
     }

    //listen to discovery responses
    public void discoveryEvent(DiscoveryEvent ev) {
    	Resolved = true;
        DiscoveryResponseMsg res = ev.getResponse();
        String name = "unknown";

	// Get the responding peer's advertisement
        PeerAdvertisement peerAdv = res.getPeerAdvertisement();
        // some peers may not respond with their peerAdv
        if (peerAdv != null) {
            name = peerAdv.getName();
        }
	// A server which can't serve properly is of no use.
	if ( res.getResponseCount () != numExpectedResponses )
		return;

        //printout each discovered peer

        ModuleSpecAdvertisement mdsadv = null;
        Enumeration en = null;
	en = res.getAdvertisements();
        if (en != null ) {
            while (en.hasMoreElements()) {
                mdsadv = (ModuleSpecAdvertisement) en.nextElement();
                try {

            		// let's print the advertisement as a plain text document
            		StructuredTextDocument doc = (StructuredTextDocument) mdsadv.getDocument( MimeMediaType.TEXT_DEFAULTENCODING );

			String versionStr = mdsadv.getVersion();
			int len = versionStr.length ();
			String numberPartStr = versionStr.substring ( 8, len );
			PipeAdvertisement pAdv = mdsadv.getPipeAdvertisement();
			String [] temp = null;
			temp = pAdv.getDescription().split(",");
			float version = Float.parseFloat ( numberPartStr );

			int hopCount;
			long timeStamp;
			long timeServer;

			try
			{
				hopCount = Integer.parseInt(temp[0]);
				timeStamp = Long.parseLong(temp[1]);
				timeServer = Long.parseLong(temp[2]);
			} catch ( ArrayIndexOutOfBoundsException e )
			{
				System.out.println ( "Array parse error" );
				return;
			}

			if ( ( minVersionNumber <= version ) && ( maxPingTime >= timeStamp ) && ( maxHopCount >= hopCount ) )

			{
				ServerList CurServer = new ServerList( pAdv, version, hopCount, timeStamp );
				int ListPos = FindServerInList (CurServer);
				
				if ( ListPos >= 0 )
				{
					//System.out.println ( "Server Response already found at List position: " + ListPos );
				}
				else
				{
				
				        System.out.println("Got a Discovery Response [" +
                           		res.getResponseCount() + " elements] from peer: " +
                           		name);
				
					System.out.println ( "\nLooking for:" + minVersionNumber + ", " + maxHopCount + ", " + maxPingTime);
					System.out.println ( "Got version: " + version + "; Hop: " + hopCount + "; TimeDiff: "+timeStamp + "; Server Time:"+timeServer +" from ----> Peer ID: " + mdsadv.getDescription());
					
					/*StringWriter out = new StringWriter();
					doc.sendToWriter(out);
					System.out.println(out.toString());
					out.close(); */

					ServerPipeIDList.add ( CurServer );
					ServersResponded ++;
				}
			}
			else
			{
				//System.out.println ( "Sorry, this server's response was not within expected parameters" );
			}

			discovery.flushAdvertisement(pAdv);

        	} catch (Exception ex) {
            		ex.printStackTrace();
            		System.out.println("Client: Error finding the service");
        	}
            }
        }
    }
    
    private int FindServerInList ( ServerList CurServer )
    {
	int size = ServerPipeIDList.size ();

	for ( int i = 0; i < size; i++ )
	{
            	ServerList Elem = ( ServerList ) ServerPipeIDList.elementAt (i);
		boolean IsVersionSame = CurServer.getVersionNum () == Elem.getVersionNum ();
		boolean IsHopSame = CurServer.getNumHop () == Elem.getNumHop ();
		boolean IsPingSame = CurServer.getTimeDiff () == Elem.getTimeDiff ();
		
		if ( IsVersionSame && IsHopSame && IsPingSame )
			return (i);
	}
	
	return (-1);
    }

    private void selectServer () {
      	try {
		System.out.println ( "Sorting" );
		bubbleSort(ServerPipeIDList, SortType.WEIGHTED_SUM);
		printSortedData ();

          	ServerList curServerData = (ServerList) ServerPipeIDList.elementAt (0);
		PipeAdvertisement pipeadv = curServerData.getPipeAdv();
		System.out.println("Current Vector size: " + ServerPipeIDList.size());
		System.out.println("Selected Server Info: file version - " + curServerData.getVersionNum() + "; Hop Count - " + curServerData.getNumHop() + "; Time: " + curServerData.getTimeDiff());

            	// Ok we have our pipe advertiseemnt to talk to the service
            	// create the output pipe endpoint to connect
            	// to the server, try 3 times to bind the pipe endpoint to
            	// the listening endpoint pipe of the service
            	for (int i=0; i<3; i++) {
               		myPipe = pipes.createOutputPipe(pipeadv, 10000);
            	}

            	// create the data string to send to the server
           	String data = "Hey dude, Why dont' you give me " + fileToGet;

            	// create the pipe message
            	msg = new Message();
            	StringMessageElement sme = new StringMessageElement("DataTag", data , null);
            	msg.addMessageElement(null, sme);

            	// send the message to the service pipe
            	myPipe.send (msg);
            	System.out.println("message \"" + data + "\" sent to the Server");
          } catch (Exception ex) {
            	ex.printStackTrace();
            	System.out.println("Client: Error sending message to the service");
    	  }
    }

    private void printSortedData ()
    {
	System.out.println ( "$$$$$ Prining sorted data $$$$$" );

	int size = ServerPipeIDList.size ();

	for ( int i = 0; i < size; i++ )
	{
            	ServerList Elem = ( ServerList ) ServerPipeIDList.elementAt (i);
		System.out.println ( "Version:" + Elem.getVersionNum () + ", Ping Latency:" + Elem.getTimeDiff () + ", Hop Count:" + Elem.getNumHop () );
	}
    }

    //when we get a message, print out the message on the console


   public void pipeMsgEvent(PipeMsgEvent event) {

        Message fileMsg = null;
        try {
            // grab the message from the event
            fileMsg = event.getMessage();
            if (fileMsg == null) {
               System.out.println("Received an empty message, returning");
                return;
            }

            // get the message element named SenderMessage
            MessageElement msgElement = fileMsg.getMessageElement(null, "ServerMsg");
            // Get message
            if (msgElement.toString() == null) {
                System.out.println("null msg received");
            } else {
                //Date date = new Date(System.currentTimeMillis());
                System.out.println("Received  :"+ msgElement.toString());
                count ++;
            }
        } catch (Exception e) {
            System.out.println("Error in getting file messages");
            }
         return;
    }


   private void establishBiDiPipe (Client obj) {
	try{
	    System.out.println("creating the BiDi pipe");
	    FileInputStream is = new FileInputStream("pipe.adv");
            pipeAdv = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(MimeMediaType.XMLUTF8, is);
            is.close();
	    pipe = new JxtaBiDiPipe();
            pipe.setReliable(true);
            System.out.println("Attempting to establish a connection");
            pipe.connect(netPeerGroup,null,pipeAdv,180000,obj);
	    waitUntilCompleted();
 	}
	catch (Exception e) {
            System.out.println("failed to bind the JxtaBiDiPipe due to the following exception");
            e.printStackTrace();
            System.exit(-1);
        }
    }

   private void waitUntilCompleted() {
        try {
            synchronized(completeLock) {
                completeLock.wait();
            }
            System.out.println("Done.");
        } catch (InterruptedException e) {
            System.out.println("Interrupted.");
        }
    }

   private void bubbleSort(Vector v, SortType type) {

   	int size = v.size ();
	double maxHops = 0;
	double maxPingTime = 0;

       	for ( int j = 0; j < size; j++ )
    	{
            	ServerList First  = ( ServerList ) v.elementAt (j);

		double curPingTime = First.getTimeDiff ();
		double curHops	= First.getNumHop ();

		if ( curPingTime > maxPingTime )
			maxPingTime = curPingTime;

		if ( curHops > maxHops )
			maxHops = curHops;
	}

    	for ( int i = 0; i < size; i ++ )
    	{
        	for ( int j = 0; j < ( size - 1 ); j ++ )
        	{
            		ServerList First  = ( ServerList ) v.elementAt (j);
            		ServerList Second = ( ServerList ) v.elementAt (j + 1);

			boolean bIsVersionMore		= ( First.getVersionNum () < Second.getVersionNum () );
			boolean bIsPingLess		= ( First.getTimeDiff () > Second.getTimeDiff() );
			boolean bIsHopLess		= ( First.getNumHop () > Second.getVersionNum () );

			double normalisedHop1		= First.getNumHop ()/ maxHops;
			double normalisedHop2		= Second.getNumHop ()/ maxHops;

			double normalisedPingTime1	= First.getTimeDiff ()/ maxPingTime;
			double normalisedPingTime2	= Second.getTimeDiff ()/ maxPingTime;

			// Ping time being more important than hopWeight, we give more weight to that.
			double hopWeight	= 0.2;
			double pingWeight = 0.8; // All weights should sum to 1.0.

			double weightedSum1 = hopWeight * normalisedHop1 + pingWeight * normalisedPingTime1;
			double weightedSum2 = hopWeight * normalisedHop2 + pingWeight * normalisedPingTime2;

			boolean bShouldWeSwap =  false;

			// If one needs to sort by ping times exclusively, set pingWeight to 1.0 and hopWeight to 0.0.
			switch (type)
			{
			 case VERSION:	bShouldWeSwap = bIsVersionMore; break;
			 				 /*( bIsVersionMore ||
						     	  ( !bIsVersionMore && ( bIsPingLess || bIsHopLess ) ) ); break; */

			 case WEIGHTED_SUM: bShouldWeSwap = ( bIsVersionMore ||
			 				     ( !bIsVersionMore && ( weightedSum1 > weightedSum2 ) ) ); break;
			}

             		if ( bShouldWeSwap )
            		{
                		v.add ( j + 1, First );
                		v.add ( j, Second );
            		}
        	}
    	}
    }

     //============ Micro test code --- uses resolve query services =================

    private void microTest(){
    	ResolverService resolver = netPeerGroup.getResolverService();
        MicroQueryHandler handler = new  MicroQueryHandler(handlerName,credential);
        resolver.registerHandler(handlerName, handler);
	sendResolveQuery();
    }

    // Send the query message
    public void sendResolveQuery(){
        try{

            ResolverService resolver = netPeerGroup.getResolverService();
	    nowClientTime = System.currentTimeMillis();
            // Create a document with the time
            StructuredTextDocument doc = null;
            doc = (StructuredTextDocument)StructuredDocumentFactory.newStructuredDocument(new MimeMediaType("text/xml"),"Time1",""+nowClientTime);
            ResolverQueryMsg message = null;
	    StringWriter out = new StringWriter();
	    doc.sendToWriter(out);
	    String xml = out.toString();
            message = new ResolverQuery(handlerName, credential,netPeerGroup.getPeerID().toString(), xml,1);
            System.out.println("Sending query");
	    System.out.println ( "My ID: " + netPeerGroup.getPeerID().toString() + ", Name: " + netPeerGroup.getPeerName () );
            // Broadcast to all members of the group
            resolver.sendQuery(null, message);

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    class MicroQueryHandler implements QueryHandler{
        protected String handlerName;
        protected StructuredTextDocument credential;
        protected SimpleDateFormat format   = new SimpleDateFormat("MM, dd, yyyy hh:mm:ss.S");

	//constructor
        public MicroQueryHandler(String handlerName, StructuredTextDocument credential){
            this.handlerName = handlerName;
            this.credential = credential;
        }

       public void processResponse(ResolverResponseMsg response) {
            System.out.println("Received a response");
        }

       public int processQuery(ResolverQueryMsg query) {
            System.out.println("Received a query");
            return ResolverService.OK;
        }
    }

    //============ End - Micro test code  =================
}

