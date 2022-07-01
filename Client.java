import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client{
	ObjectOutputStream out;
	ObjectInputStream in;
	String str;
	Socket socket;
	static String Client_Name;
	static int CONNECT_PORT;
	private String FILE_TO_RECEIVED;
	private String STORED_FILE;
	private boolean connected = false;
	public static void main(String[] args) {
		System.out.println("*****************************");
		System.out.println("*   欢迎来到AustinMa的聊天室   *");
		System.out.println("*      群聊 私聊 文件 文本     *");
		System.out.println("*        在这里都能实现        *");
		System.out.println("*****************************");
		System.out.println("**你是用户端**");
		System.out.print("首先请输入所要使用的端口号 ：");
		Scanner sc=new Scanner(System.in);
		CONNECT_PORT = sc.nextInt();

		System.out.print("然后请输入所要使用的昵称（用于生成文件夹,存放传输文件）：");
		sc.nextLine();
		Client_Name = sc.nextLine();

		mkDir(Client_Name);
		new Client().begin();
	}
	void begin() {
		try{
			socket = new Socket("localhost",CONNECT_PORT);
			//初始化inputStream以及outputStream
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			//从标准输入中读取命令
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("----------请输入命令----------");
			Server_Connection ser = new Server_Connection();
			new Thread(ser).start();
			while(true){
				str = bufferedReader.readLine();//read a sentence from the standard input
				if(str.equals("quit"))
				{
					connected=false;
					break;
				}
				sendMessage(str);//Send the sentence to the server
				System.out.println("----------已发送成功----------");
				System.out.println("----------请输入命令----------");
			}
		}
		catch (ConnectException e) {
			System.err.println("Connection refused. You need to initiate a server first.");
		}
		catch(UnknownHostException unknownHost){
			System.err.println("You are trying to connect to an unknown host!");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
		finally{
			//关闭连接
			try{
				in.close();
				out.close();
				socket.close();
			}
			catch(IOException ioException){
				ioException.printStackTrace();
			}
		}
	}
	private class Server_Connection implements Runnable {
		public void run() { //receive message from server;
			try {
				in = new ObjectInputStream(socket.getInputStream());
				connected = true;
				while (connected) {
					// inStr代表用户端读进来别人传送的String
					String inStr = (String)in.readObject();
					if(!inStr.equals("file")) {
						//	传的非文件
						System.out.println(inStr);
					}else {
						String senderNumber=(String)in.readObject();
						STORED_FILE = (String)in.readObject();
						//创建一个文件路径存放收到的文件;
						FILE_TO_RECEIVED = Client_Name + "/" + STORED_FILE;
						byte[] contents = new byte[10000];
						//Initialize the FileOutputStream to the output file's full path.
						FileOutputStream fos = new FileOutputStream(FILE_TO_RECEIVED);
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						InputStream is = socket.getInputStream();
						String inLengthStr = (String)in.readObject();
						int fileLength= Integer.valueOf(inLengthStr);
						int bytesRead = 0;
						int[] total=new int[1];
						//边读入向外写出
						while((bytesRead=is.read(contents))!=-1){
							total[0]= total[0]+bytesRead;
							bos.write(contents, 0, bytesRead);
							if(total[0]==fileLength){
								break;
							}
						}
						bos.flush();
						System.out.println("-----用户"+senderNumber+"给你发来了 File: "+STORED_FILE+"-----");
					}
				}
			} catch (SocketException e1) {
				System.out.println("Bye!");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
	}
	//传送命令（信息）到ouput流，传到服务端
	void sendMessage(String msg)
	{
		try{
			//stream write the message
			out.writeObject(msg);
			out.flush();
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}
	/*Create directory if new client is created*/
	private static boolean mkDir(String name) {
		File dir = new File(name);
		if(dir.exists()) {
			return false;
		}
		if(dir.mkdir()) {
			return true;
		} else {
			return false;
		}
	}
}
