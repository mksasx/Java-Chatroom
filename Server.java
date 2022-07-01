import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Server {
	ServerSocket Acceptor;
	int clientNum = 1;
	static int PORT_NUMBER;
	String FILE_PATH;
	boolean started = false;
	static HashMap<Integer,Client_Connection> map = new HashMap<>();
	/*解析两种文件名字，一种是有引号的*/
	private static String extractFileName(String str) {
		String[] a = str.split("/");
		int length = a.length;
		String temp = a[length - 1];
		if(temp.charAt(0) == '"') {
			return temp.substring(1, temp.length() - 1);
		} else {
			return temp.substring(0, temp.length() - 1);
		}
	}
	/*获得双引号里面的内容*/
	private static String content(String sentence) {
		int cursor = 0;
		while(sentence.charAt(cursor) != '\"') {
			cursor++;
		}
		int start = cursor++;

		while(sentence.charAt(cursor) != '\"') {
			cursor++;
		}
		int end = cursor;
		return sentence.substring(start + 1, end);
	}
	/*获得要传给的客户端序号*/
	public int getTarget(String client) {
		return (client.charAt(client.length() - 1) - '0') - 1 ;
	}
	public static void main(String[] args) {
		System.out.println("*****************************");
		System.out.println("*   欢迎来到AustinMa的聊天室   *");
		System.out.println("*      群聊 私聊 文件 文本     *");
		System.out.println("*        在这里都能实现        *");
		System.out.println("*****************************");
		System.out.println("**你是服务端**");
		System.out.print("首先请输入所要使用的端口号：");
		Scanner sc=new Scanner(System.in);
		PORT_NUMBER = sc.nextInt();
		System.out.println("--------------------服务端已启动--------------------");
		Runnable ListenCmd = () -> {
			while (true){
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
				while(true){
					try {
						String cmdText = bufferedReader.readLine();
						String[] tmp=cmdText.split("\\s+");
						if(tmp[0].equals("all"))
						{
							if(tmp[1].equals("message"))
							{
								map.forEach((key, value) -> {
									value.send("-----服务端群发了消息:"+content(cmdText)+"-----");
								});
							}

						}
						if(tmp[0].equals("single"))
						{
							if(tmp[1].equals("message"))
							{
								Client_Connection c = map.get(Integer.parseInt(tmp[tmp.length-1]));
								c.send("-----服务端向你私聊了消息:"+content(cmdText)+"-----");
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread listenCMDThread = new Thread(ListenCmd);
		listenCMDThread.start();
		new Server().begin();
	}
	public void begin() {
		try {
			Acceptor = new ServerSocket(PORT_NUMBER);
			started = true;
		} catch (BindException e1) {
			//port的exception
			System.out.println("Port is in use.");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();					
		}
		try {
			while (started) {
				Socket socket = Acceptor.accept();
				Client_Connection c = new Client_Connection(socket);
				new Thread(c).start();  
				map.put(clientNum,c);
				System.out.println("------用户Client " + clientNum + " 已上线------");
//				TimeUnit.MILLISECONDS.sleep(100);
				c.send("你的编号是"+Integer.toString(clientNum));
				clientNum++;
			}
		} catch(IOException e) {
			e.printStackTrace();		
		}  finally{
			try {
				Acceptor.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
//	每当有一个客户端连接进来后，就启动一个单独的线程进行处理
	class Client_Connection implements Runnable {
		private Socket socket;
		private ObjectInputStream in;
		private ObjectOutputStream out;
		private boolean connected;
		private int number = clientNum;
		String str;
		public Client_Connection(Socket socket) throws IOException {
			this.socket = socket;
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
		}
		/*传输（广泛意义上的）*/
		public void send(String str) {
			try {
				out.writeObject(str);
				out.flush();
			} catch(IOException e) {
				Collection<Client_Connection> col = map.values ();
//				map.remove(this);
				col.remove (this);
				System.out.println("I/O exception");
			}
		}
		/*传输文件*/
		private void sendFile(String path) {
			try {
				File file = new File(path);
		        FileInputStream fis = new FileInputStream(file);
		        BufferedInputStream bis = new BufferedInputStream(fis); 
		        OutputStream os = socket.getOutputStream();
		        byte[] contents;
		        long fileLength = file.length();
		        String fileClientString=Long.toString(fileLength);   
		        send(fileClientString);
		        long current = 0;
//				通过逼近算得大小
		        while(current!=fileLength){ 
		            int size = 10000;
		            if(fileLength - current >= size)
		                current += size;    
		            else{ 
		                size = (int)(fileLength - current); 
		                current = fileLength;
		            } 
		            contents = new byte[size]; 
		            bis.read(contents, 0, size); 
		            os.write(contents);
		        }   
		        os.flush(); 
			} catch(IOException e) {
				Collection<Client_Connection> col = map.values ();
				col.remove (this);
				System.out.println("I/O exception");
			}
		}
		public void run() {
 
				try {
//					in = new ObjectInputStream(socket.getInputStream());
//					out = new ObjectOutputStream(socket.getOutputStream());
//					out.flush();
					connected = true;
					while(connected) {
						str = (String)in.readObject();
						String[] temp = str.split("\\s+");
						//发送方法
						String method = temp[0];
						if(method.equals("quit"))
						{
							connected=false;
							break;
						}
						//发送的东西的类型
						String MOF = temp[1];
						if(MOF.equals("file")) {
							FILE_PATH = temp[2].substring(1,temp[2].length()-1);
						}
						String message = content(str);// extract content from str
						if(method.equals("all")) {  //群聊
							if(MOF.equals("message")) {
								map.forEach((key, value) -> {
									if(key!=number) {
										Client_Connection c = map.get(key);
										c.send("-----用户Client"+number+"向所有人发送了消息: "+message+"-----");
									}
								});
							}
							if(MOF.equals("file")) {
								map.forEach((key, value) -> {
									if(key!=number) {
										Client_Connection c = map.get(key);
										c.send("file");
										c.send(Integer.toString(number));	//send file's sender_num
										c.send(extractFileName(temp[2]));	// send file name
										c.sendFile(FILE_PATH);	//send file
									}
								});
							}
							System.out.println("-----用户Client"+number+" 向所有人"+"传输了 "+MOF+"-----");
						} else if(method.equals("single")) {  //私聊
							int target = 0;
							if(MOF.equals("message")) {
								String client = temp[temp.length - 1];
								target = getTarget(client);
								Client_Connection c = map.get(target+1);
								c.send("-----用户Client"+number+"向你私聊了消息:"+message+"-----");
							}
							if(MOF.equals("file")) {
								String client = temp[temp.length - 1];
								target = getTarget(client);
								Client_Connection c = map.get(target+1);
								c.send("file");
								c.send(Integer.toString(number));
								c.send(extractFileName(temp[2]));
								c.sendFile(FILE_PATH);
							}
							int Target_Client=target+1;
							System.out.println("-----用户Client"+number+" 单发了"+" "+MOF+" 给用户Client"+Target_Client+"-----");
						} else if(method.equals("shield")) { //屏蔽某个人发送
							int target = 0;
							if(MOF.equals("message")) {
								String client = temp[temp.length - 1];
								target = getTarget(client);
								int finalTarget = target;
								map.forEach((key, value) -> {
									if(key!= finalTarget+1&&key!=number) {
										Client_Connection c = map.get(key);
										c.send("-----用户Client"+number+"屏蔽了用户Client"+(finalTarget+1)+",向其他人发送了消息: "+message+"-----");
									}
								});
							}
							if(MOF.equals("file")) {
								String client = temp[temp.length - 1];
								target = getTarget(client);
								int finalTarget = target;
								map.forEach((key, value) -> {
									if(key!= finalTarget+1&&key!=number) {
										Client_Connection c = map.get(key);
										c.send("file");
										c.send(Integer.toString(number));
										c.send(extractFileName(temp[2]));
										c.sendFile(FILE_PATH);
									}
								});
							}
							int blockClient=target+1;
							System.out.println("-----用户Client"+number+" 给除了 用户Client"+blockClient+"的用户发送了"+MOF+"-----");
						} else {
							System.out.println("-----请输入正确指令-----");
						}
					}
				} catch (EOFException e) {
					Collection<Client_Connection> col = map.values ();
					col.remove (this);
//					clientNum--;
					System.out.println("-----用户Client " + this.number + " 下线了-----");
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch(NullPointerException e) {
					Collection<Client_Connection> col = map.values ();
					col.remove (this);
//					clientNum--;
					System.out.println("-----用户Client " + this.number + " 下线了-----");
				} finally {
					try {
						if(out != null) {
							out.close();
						}
					} catch(IOException e) {
						e.printStackTrace();
					}
 				
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (socket != null)
						try {
							socket.close();
							socket = null; 
						} catch (IOException e) {
							e.printStackTrace();
						}
					Collection<Client_Connection> col = map.values ();
					col.remove (this);
//					clientNum--;
				}	
			}
	}

}