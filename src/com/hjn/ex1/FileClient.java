/**
 * 
 */
package com.hjn.ex1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author HJN
 *
 */
public class FileClient {
	private static final int UDP_PORT = 2020; // UDP服务器port
	private String remoteIp = "127.0.0.1"; // UDP服务器IP
	private static final int BYTE_LEN = 8192; // 设置每次传输数据的长度

	private static final int PORT = 2021; // TCP连接端口
	private static final String HOST = "127.0.0.1"; // TCP连接地址
	private static String CLIENT_PATH = null;
	private static String BASE_PATH = null;
	private static String CURRENT_PATH = null;
	private List<String> CURRENT_FILE = new ArrayList<String>();
	private List<String> CURRENT_DIR = new ArrayList<String>();

	public DatagramSocket dgSocket;
	public Socket socket;

	BufferedReader br;
	BufferedWriter bw;
	PrintWriter pw;
	OutputStream os;

	public FileClient() throws UnknownHostException, IOException {
		socket = new Socket();
		socket.connect(new InetSocketAddress(HOST, PORT));
		dgSocket = new DatagramSocket(); // 随机可用端口，又称匿名端口
	}

	public void initStream() throws IOException { // 初始化输入输出流对象方法
		// 客户端输出流，向服务器发消息
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		// 客户端输入流，接收服务器消息
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		pw = new PrintWriter(bw, true); // 装饰输出流，及时刷新
		String msg = br.readLine();
		System.out.println(msg);
	}

	public static void main(String[] args) throws UnknownHostException, IOException {

		/*
		 * 实现：服务器端启动时需传递root目录参数，并校验该目录是否有效；
		 */
		try {
			if (args.length == 0)
				throw new IllegalArgumentException("输入数据为空");
			CLIENT_PATH = args[0];
			BASE_PATH = args[1];
			CURRENT_PATH = BASE_PATH;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}

		new FileClient().service();

	}

	/**
	 * send implements
	 * 
	 * @throws InterruptedException
	 */
	public void service() {
		try {
			initStream(); // 初始化输入输出流对象

			Scanner in = new Scanner(System.in); // 接受用户信息
			String msg = null;
			String[] cmd = null;

			do {
				updateCurrentFile(); // 更新CURRENT_FILE

				// 开始执行用户输入的命令
				msg = in.nextLine();
				if (msg == null)
					break;
				pw.println(msg); // 发送给服务器端

				cmd = msg.split(" ");
				if (cmd[0].equals("bye")) {
					break; // 退出
				} else if (cmd[0].equals("get")) {
					String fileName = cmd[1];
					inform();
					receiveFile(fileName);
				} else if (cmd[0].equals("ls")) {
					do {
						String arg = br.readLine(); // 获取服务器传来的文件信息
						if (!arg.equals("end")) {
							System.out.println(arg); // 如果不是结束符，则输出文件信息
						} else {
							break;
						}
					} while (true);

				} else if (cmd[0].equals("cd")) {
					if (cmd.length == 2) {
						cdControl(cmd[1]);
					} else {
						System.out.println("unknown cmd");
					}
				} else if (!cmd[0].equals("cd") && !cmd[0].equals("ls") && !cmd[0].equals("bye")
						&& !cmd[0].equals("get")) {
					String arg = br.readLine();
					System.out.println(arg);
				}

			} while (true);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					socket.close(); // 断开连接
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/*
	 * 更新CURRENT_FILE
	 */
	public void updateCurrentFile() throws IOException {
		pw.println("ls");
		do {
			String arg = br.readLine(); // 获取服务器传来的文件信息

			if (!arg.equals("end")) {
				/*
				 * 将当前文件信息保存在CURRENT_FILE中
				 */
				String[] temp = arg.split("\t");
				if (temp[0].equals("<file>")) {
					CURRENT_FILE.add(temp[1]);
				} else if (temp[0].equals("<dir>")) {
					CURRENT_DIR.add(temp[1]);
				}
			} else {
				break;
			}
		} while (true);
	}

	public void cdControl(String cmd) throws IOException {
		if (cmd.equals("..")) {
			if (CURRENT_PATH.equals(BASE_PATH)) {
				pw.println("1_1"); // 将check信息发送给服务器
//				System.out.print(BASE_PATH + " > ");
				System.out.println(br.readLine());
				return;
			} else {
				File temp = new File(CURRENT_PATH);
				CURRENT_PATH = temp.getParent();
				pw.println("1_2"); // 将check信息发送给服务器
				pw.println(CURRENT_PATH); // 发送给服务器端
//				System.out.print(CURRENT_PATH + " > ");
				System.out.println(br.readLine());
			}
		} else {
			boolean checkDir = false;
			// 检查要移动的目录是否存在
			for (String s : CURRENT_DIR) {
				if (s.equals(cmd))
					checkDir = true;
			}
			if (checkDir) {
				CURRENT_PATH = CURRENT_PATH + "\\" + cmd;
				pw.println("1_3"); // 将check信息发送给服务器
				pw.println(CURRENT_PATH); // 发送给服务器端
//				System.out.print(CURRENT_PATH + " > ");
				System.out.println(br.readLine());
			} else {
				pw.println("1_4"); // 将check信息发送给服务器
				System.out.println(br.readLine());
			}
		}

	}

	public void inform() throws IOException {
		SocketAddress socketAddres = new InetSocketAddress(remoteIp, UDP_PORT); // 指定服务器端地址

		String s = "give the address and port of Client";
		byte[] info = s.getBytes();
		// 创建数据包，指定服务器地址
		DatagramPacket dp = new DatagramPacket(info, info.length, socketAddres);
		dgSocket.send(dp); // 向服务器端发送数据包
		DatagramPacket inputDp = new DatagramPacket(new byte[512], 512);
		dgSocket.receive(inputDp); // 接受服务器返回的信息
		String msg = new String(inputDp.getData(), 0, inputDp.getLength());
	}

	public void receiveFile(String fileName) throws IOException {
		try {
			os = new FileOutputStream(new File(CLIENT_PATH + "\\" + fileName));
			byte[] b = new byte[BYTE_LEN];
			DatagramPacket dp = new DatagramPacket(b, b.length);
			if (br.readLine().equals("不存在该文件，取消文件传输")) {
				System.out.println("unknown file");
				return;
			} else {
				System.out.println("开始接收文件：" + fileName);
			}
			int length = Integer.parseInt(br.readLine()); // 获取服务器端传来的文件的大小（单位：byte）
			while (length > 0) {
				// 接收数据
				dgSocket.receive(dp);
				os.write(b, 0, b.length);
				os.flush();
				length -= BYTE_LEN;
			}
			System.out.println("文件接收完毕");
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
