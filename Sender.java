import java.io.*;
import java.net.*;
import java.util.regex.*;
import javax.swing.JOptionPane;
 
public class Sender implements Runnable {
	final static String IP_REGEX = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])([.](\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}"; 	//pattern string for ip
	static boolean sendLock = false;		//if sendLock is false, sender cann't send files to receiver.
	Socket socket = null;
    DataInputStream fileIn = null;			//used for file
    DataOutputStream outSocket = null;		//used for socket
    static DataInputStream inSocket = null; //used for socket
    int packageSize = 1024;					//the unit size when send a file
    String ip;
    int port = 9000;						//set the default port number
      
    /**
	 * constructor of the class
	 * @param ip receiver's ip
	 * @param port port number
	 */
    public Sender(String ip) {
    	this.ip = ip;
    }
    public Sender(String ip, int port) {
        this.ip=ip;
        this.port=port;
    }
 
    /**
	 * connect to receiver and build the outputStream object
	 */
    public boolean connect() {
        try {
            socket = new Socket(ip, port);
            outSocket = new DataOutputStream(socket.getOutputStream());
            inSocket = new DataInputStream(socket.getInputStream());
            return true;
        }
        catch (Exception e) {
        	e.printStackTrace();
            return false;
        }
    }
    /**
     * close the output stream
     */
    public void close() {
        try {
            outSocket.close();
            System.out.println("Sender has been closed successfully！");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
	 * Send an integer number, ie packageSize, to receiver
	 */
    public void sendNumber(int data) {
        try {
            outSocket.writeInt(data);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    /**
     * Send a string
     * @param data sent string
     * @return data's length
     */
    public long sendStr(String str) {
        try {
            outSocket.writeUTF(str);
            return str.length();
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
 
    /**
     * Send a file
     * @param filePath file's absolute path
     * @return file size
     */
    public long sendFile(String filePath) {        
        File fileObj = new File(filePath);
        if (!fileObj.exists() || !fileObj.isFile())	{	//check if it exists or is a file        
            System.out.println("The file doesn't exist or it's not a file at all!!!\n");
            System.exit(0);
        }
        
        long fileSize = 0;
        long beginTime = System.currentTimeMillis();	//record the beginTime
        
        //parse the file name and path
        String name = null;								//here, name is the actual filename
        String path = null;			
        int i = filePath.lastIndexOf("/");			//if there's no substring "/", then it returns -1
        if (i == -1)								//filePath contains no path info
        	name = filePath;
        else {
        	name = filePath.substring(i+1);			//i+1 is the beginning index
            path = filePath.substring(0, i);
        }
        
        // send fileName
        try {
			outSocket.writeUTF(name);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        
        sendNumber(packageSize);
        
        try {
            fileIn = new DataInputStream(new FileInputStream(fileObj));
        }
        catch (FileNotFoundException e) {
        	System.out.println("File not found!");
            e.printStackTrace();
        }
        
        System.out.println("Sending file ---> \033[31m" + name + "\033[0m ...");
        
        //read file and send the package
        try {
            byte[] b = new byte[packageSize];
            int n = fileIn.read(b);
            while (n != -1) {	 		// not to the end of the file
                if (n == packageSize) {
                    outSocket.write(b);
                }
                else {
                    outSocket.write(b, 0, n);
                }
                fileSize += n;
                n = fileIn.read(b);
            }
            System.out.println("Done.\nThe transmitting info of this file is as below: ");
        }
        catch (Exception e) {
        	System.out.println("Error occurs when sending file!\n");
        	System.exit(0);
            //e.printStackTrace();
        }
 
        //close fileInputStream: fileIn
        try {
            fileIn.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        //statistical info
        long endTime = System.currentTimeMillis();
        double time = (endTime - beginTime) + 0.001;  	// consuming time, 0.001 acts as a infinitesimal factor
        double speed = (double)fileSize / time;			// transmit speed
        System.out.printf("Time used: %.3f s\n", time/1000);
        System.out.printf("File size: %.3f Kb\n", fileSize / 1024.0);
        System.out.printf("Transmit speed: %.3f Kb/s\n\n", speed*1000 / 1024.0);
        
        return fileSize;
    }
    
    //the next two methods are overloaded ones
    /** 
     * Given absolute path and root folder, then get the relative path
	 * @param abPath the absolute path
	 * @param rtPath the root path
	 */
    public String getRelativePath(String abPath, String rtPath) {
    	int l = rtPath.length();  	//根路径长度
        String r = abPath.substring(l);
        return r;
    }
    /** 
     * Given absolute path, root folder and file name, then get the relative path
     * @param abPath the absolute path
	 * @param rtPath the root path 
	 * @param fileName file's name
	 */
    public String getRelativePath(String abPath, String rtPath,String fileName) {
    	int lenRootPath = rtPath.length();
        int lenFileName = fileName.length();
        int lenAbPath=abPath.length();
        String relaPath = abPath.substring(lenRootPath, lenAbPath-lenFileName-1);
        return relaPath;
    }
    
    /**
     * act as deamon program of sendFolder
     * @param folderPath absolute path of the folder to be sent
     * @return total file size
     */
    public long sendFolderDeamon(String folderPath) {
        long fileSize = sendfolder(folderPath,folderPath);
        if(fileSize != -1) { 
              sendStr("suc");
              System.out.println("Send the folder( or file) successfully!");
        }
        return fileSize;
    }
    
    /**
     * send folder recursively
     * @param subFolder	sub-folder or sub-file in folderPath, the same with folderPath at first, but changing recursively
     * @param folderPath stay unchanged
     * @return the total length of the files that have been sent
     */
    public long sendfolder(String subFolder,String folderPath) {
    	long totalFSize=0;		//total file size
        try {
            File a = new File(subFolder);
            
            if ( !a.exists() ) {
                System.out.println("The folder doesn't exist!");
                sendStr("null");
                System.exit(0);
            }
            
            if( a.isDirectory() )
            	System.out.println("Entering directory: \033[36m" + subFolder + "\033[0m \nDone.\n");
            
            if( a.isFile() ) {					//folderPath is a pathName of a single file
            	sendStr("file");				//Single file
            	outSocket.writeUTF("");
            	return sendFile(a.getAbsolutePath());            	
            }
                       
            File[] temp = a.listFiles();		// get the file list, but the file's order is not specified
            int i;
            for (i = 0; i < temp.length; i++) {
                if (temp[i].isFile()) 			//temp[i] is a file, sent directly
                {
                    String relaPath = getRelativePath(temp[i].getAbsolutePath(), folderPath, temp[i].getName());	//get the relative path
                    sendStr("file");				//Send info to receiver that there's file to be sent
                    outSocket.writeUTF(relaPath);	//send the relative path
                    long q=sendFile(temp[i].getAbsolutePath());		//send the file with absolute path
                    Thread.sleep(500);
                    totalFSize+=q;
                }
                else if (temp[i].isDirectory())		//temp[i] is a directory, then sent recursively
                {
                    String relaPath = getRelativePath(temp[i].getAbsolutePath(), folderPath);	//get relative path
                    sendStr("dir");					// Send info to receiver that there's folder to be sent
                    if(relaPath.startsWith("/"))	// trim the first character '/' if it has
                    	relaPath = relaPath.substring(1);
                    outSocket.writeUTF(relaPath);
                    long q=sendfolder(temp[i].getAbsolutePath(), folderPath);  //send the sub-folder recursively

                    //after send all the files in temp[i], leaving it
                    outSocket.writeUTF("outDir");
                    outSocket.writeUTF(relaPath);
                    System.out.println("Leaving directory: \033[36m" + temp[i].getAbsolutePath() + "\033[0m\nDone.\n");
                    totalFSize+=q;
                }
            }
            return totalFSize;
        }
        catch (Exception e) {
            System.out.println("Error occurs while sending folder!");
            sendStr("err");
            System.exit(0);
            // e.printStackTrace();
            return -1;           
        }
    }
    
    public static void main(String[] args) {
    	//get receiver's ip and match it with the given regex IP_REGEX
    	String rcvrIP = null;
    	Pattern p = null;
    	Matcher m = null;
    	while (true) {
	    	rcvrIP = JOptionPane.showInputDialog("Input receiver's ip address:").trim();
	    	p = Pattern.compile(IP_REGEX);
	    	m = p.matcher(rcvrIP);
	    	if(m.matches())
	    		break;
	    	else
	    		JOptionPane.showMessageDialog(null, "The format of ip is wrong, input again!");
    	}
    	
    	Sender sender =null;
    /*	//let the user choose whether to specify a port number
    	int inputPort = JOptionPane.showConfirmDialog(null, "Do you want to specify port number?");
    	if(inputPort == 0) {
    		portStr = JOptionPane.showInputDialog("Input the port number(bigger than 1024):");
    		port = Integer.parseInt(portStr);
    		sender = new Sender(rcvrIP, port);
    	}
    	else
    */
    	sender = new Sender(rcvrIP);
        new Thread(sender).start();		//create a thread and start it
        if( !sender.connect() ) {		//connect and check
        	System.out.println("Cann't connect to receiver!");
        	System.exit(0);
        }
        else 
        	System.out.println("Connect to receiver successfully!\n");
        
        // waiting for sending flag
        while(!sendLock) {
        	try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	try {
    			sendLock = inSocket.readBoolean();
    		} catch (IOException e1) {
    			System.out.println("Cannot get sending privilege from receiver!");
    			System.exit(0);
    			e1.printStackTrace();
    		}
        }
        
        String pathStr = JOptionPane.showInputDialog("Input the path to be sent:");
        if (pathStr.endsWith("/"))
        	pathStr = pathStr.substring(0, pathStr.length()-1);
        System.out.println("Begin to send files.....\n");
        long totalTimeBegin = System.currentTimeMillis();
        long totalFileSize = sender.sendFolderDeamon(pathStr);
        long totalTimeEnd = System.currentTimeMillis();
        long totalTime = totalTimeEnd - totalTimeBegin;
        System.out.println("The overall transmiting info is as below:");
        System.out.printf("Total size: %.3f Kb\n", totalFileSize / 1024.0);
        System.out.printf("Time used: %.3f s\n", totalTime / 1000.0 );
        System.out.printf("Ave speed: %.3f Kb/s\n", (double)totalFileSize*1000 / (totalTime*1024) );
        
        sender.close();
    }
    
    /**
     * This function does nothing
     */
    public void run() {
        
    }
}
