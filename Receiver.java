import java.io.*;
import java.net.*;

import javax.swing.JOptionPane;

public class Receiver implements Runnable {    
    ServerSocket server = null;
    Socket socket = null;
    DataOutputStream fileOut = null;		//file output stream
    DataInputStream input = null;			//used for socket
    static DataOutputStream output = null;	//used for socket
    File file = null;
    int packageSize = -1;					//packageSize - unit size when transmitting file. Its value of -1 denotes some error occurs
    final int port = 9000;					//default number
    String path=null;
    static long totalTimeBegin = 0;			//record the beginning totalTime, and trigger it in method of sendFolder()
    
    /**
     * constructor
     * @param port port number
     * @param path path name
     */
    public Receiver(String path) {
        this.path = path;
    }
    
    //make a server socket and listening for connection
    public boolean listen() {
        try {
            server = new ServerSocket(port);
            socket = server.accept();
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            System.out.println("Sender has connected successfully!\nSender's ip address and port number are: " + socket.getRemoteSocketAddress() + "\n");
            return true;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // close the dataInputStream - input
    public void close() {
        try {
            input.close();
            output.close();
            System.out.println("Receiver has been closed successfully！");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /** get an integer from sender
     * @return The returned integer is assigned to packageSize
     */
    public int getNumber() {
        int number;
        try {
            number = input.readInt();            
            return number;
        }
        catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * get a string from sender
     * @return the string read from sender's socket
     */
    public String getStr() {
        String str = null;
        try {
            str = input.readUTF();
            return str;
        }
        catch (EOFException e) {	//get EOF, it's useless here, ignore it and get the next UTF
        	try {
        		str = input.readUTF();
                return str;
            }
            catch (IOException e1) {
                e1.printStackTrace();
                return null;
            }
        }
        catch (IOException e)  {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get file from sender and store it
     * @param path path input which the file will be stored
     * @return file size
     */
    public long getFile(String path) {
        long beginTime = System.currentTimeMillis();
        String fileName = getStr();
        packageSize = getNumber();
        File file = new File(path + "/" + fileName);
        
        System.out.println("***************" + path + "/" + fileName);
                
        try {	 	// build the new file
            fileOut = new DataOutputStream(new FileOutputStream(file));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
 
        long fileSize = 0;
        System.out.println("Receiving file <--- \033[31m" + fileName + "\033[0m ...");
        //store file
        try {
            byte[] data = new byte[packageSize];
 
            int len = input.read(data);
            while (len != -1) {
                fileSize += len;
                if (len == packageSize)
                    fileOut.write(data);
                else
                {
                    fileOut.write(data, 0, len);
                    break;
                }
                len = input.read(data);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        System.out.println("Done.\nThe receiving information is as below:");
        // statistical info
        long endTime = System.currentTimeMillis();
        long time = endTime - beginTime;
        double speed = (double)fileSize / time;
        System.out.printf("Time used: %.3f s\n", time / 1000.0);
        System.out.printf("File size： %.3f Kb\n", fileSize / 1024.0 );
        System.out.printf("Receiving speed： %.3f kb/s\n\n", speed*1000 / 1024.0 );
 
        //receive over, close DataOutputStream - fileOut
        try {
            fileOut.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return fileSize;
    }
    
    /**
     * get folder from sender
     * @param path the current absolute path
     * @return the total size of received files, or -1 if some error occurs
     */
    public long getFolder(String path) {
    	long sum = 0;
        String folder = null;					// folder's pathname
        String flag = getStr();					// flag string
        totalTimeBegin = System.currentTimeMillis();	//after receive the first flag, the receiving work truly begins. So, notice the occasion.
        while (true) {
            if(flag.equals("suc")) {			// receive successfully
            	return sum;
            }
            else if (flag.equals("dir")) {			// is directory
                folder = getStr();					// get the relative path
                folder = path + "/" + folder;		// get the absolute path
                System.out.println("Making directionary: \033[36m" + folder + "\033[0m");
                new File(folder).mkdir();			// make directory
                System.out.println("Done.\n");
                flag = getStr();					// get the next flag
            }
            else if (flag.equals("file"))			// is file
            {
                String fileRelaPath = getStr();
                if(fileRelaPath.startsWith("/"))
                	fileRelaPath = fileRelaPath.substring(1);
                long q ;
                if ( fileRelaPath.equals("") )		//in this case, sender only sends a single file or these files are in the first sub-dir of the sending dir
                	q = getFile(path);
                else
                	q = getFile(path + "/" + fileRelaPath);
                sum+=q;
                flag = getStr();
            }
            else if(flag.equals("outDir")) {
            	String leavingPath = path + "/" + getStr();
            	System.out.println("Leaving directory: \033[036m" + leavingPath + "\033[0m\nDone.\n");
            	flag = getStr();
            }
            else if(flag.equals("null")) {
            	System.out.println("There's error in sender. (The folder it referes doesn't exist.)");
            	System.exit(0);
            }
            else if(flag.equals("err")) {
            	System.out.println("Error occurs while receiving folder!");
            	System.exit(0);
            }
            else
                return -1;
        }
    }

    public static void main(String[] args) {
        Receiver f = new Receiver("");
        new Thread(f).start();
        f.listen();
        
        //enter the target path and build it
        String tarPath = JOptionPane.showInputDialog(null, "Input the target path:", "Receiver - Get Target Path", JOptionPane.QUESTION_MESSAGE);
        if (tarPath.endsWith("/"))
        	tarPath = tarPath.substring(0, tarPath.length()-1);
      	if( !new File(tarPath).mkdir() ) {
      		System.out.println("You have conceled to receive it.");
      		System.exit(0);
      	}
        
        //write sending flag to sender to notify it that receiver is ready to receive
        try {
			output.writeBoolean(true);
		} catch (IOException e) {
			System.out.println("Cann't write sending flag to Sender!");
			System.exit(0);
			e.printStackTrace();
		}
        
        System.out.println("Begin to receive files. These files will be store under: \033[36m" + tarPath + "\033[0m\n");
        //long totalTimeBegin = System.currentTimeMillis();
        double totalSize = (double)f.getFolder(tarPath) / 1024;						//receive the folder and get the total fileSize
        long totalTimeEnd = System.currentTimeMillis();
        double totalTime = 0.001 + (double)(totalTimeEnd - totalTimeBegin) / 1000;	// 0.001 acts as a minimum factor
        System.out.println("Receiving done.\nThe overall receiving info is as below: ");
        System.out.printf("Total size: %.3f Kb\n", totalSize);
        System.out.printf("Time used: %.3f s\n", totalTime);
        System.out.printf("Ave speed: %.3f Kb/s\n", totalSize / totalTime);
        f.close();
    }
    /**
     * This function does nothing
     */
    public void run() {
    	
    }
 }
