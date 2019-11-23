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
import java.util.Scanner;

/**
 * FileClient 客户端，与服务器端进行交互和文件传输
 * @author HJN
 *
 */
public class FileClient {
	private static final int UDP_PORT = 2020; // UDP服务器port
	private String remoteIp = "127.0.0.1"; // UDP服务器IP
	private static final int BYTE_LEN = 8192; // 设置每次传输数据的长度

//	private static final int PORT = 2021; // TCP连接端口
	private static int PORT = 2021; // TCP连接端口
//	private static final String HOST = "127.0.0.1"; // TCP连接地址
	private static String HOST = "127.0.0.1"; // TCP连接地址
//	private static String CLIENT_PATH = null; // 不能当做参数传入，服务器的地址应该对客户端保持隐藏
//	private static String BASE_PATH = null; // 删除该属性，客户端不需要服务器端的路径
//	private static String CURRENT_PATH = null; // 删除该属性，客户端不需要服务器端的路径

	public DatagramSocket dgSocket;
	public Socket socket;

	BufferedReader br;
	BufferedWriter bw;
	PrintWriter pw;
	OutputStream os;

	/**
	 * FileClient 构造函数，初始化客户端的TCPsocket和UDPsocket，TCP连接到服务器端的主机地址和端口号
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public FileClient() throws UnknownHostException, IOException {
		socket = new Socket();
		socket.connect(new InetSocketAddress(HOST, PORT));
		dgSocket = new DatagramSocket(); // 随机可用端口，又称匿名端口
	}

	/**
	 * initStream 初始化字符读入和写出流
	 * @throws IOException
	 */
	public void initStream() throws IOException { // 初始化输入输出流对象方法
		// 客户端输出流，向服务器发消息
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		// 客户端输入流，接收服务器消息
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		pw = new PrintWriter(bw, true); // 装饰输出流，及时刷新
	}

	/**
	 * main 主函数，获取arguments,初始化HOST和PORT
	 * @param args 包含服务器端的IP地址和端口号
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {

		/*
		 * 实现：服务器端启动时需传递root目录参数，并校验该目录是否有效；
		 */
		try {
			if (args.length == 0) {
				throw new IllegalArgumentException("输入数据为空");
			} else if(args.length == 2) {
				HOST = args[0];
				PORT = Integer.parseInt(args[1]);
			} else if(args.length > 2) {
				throw new IllegalArgumentException("输入参数多于2个，请调整参数个数");
			}
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}

		new FileClient().service();

	}

	/**
	 * service 实现获取输入缓冲区命令的读取和服务器的信息交互，同时实现了UDP的文件传输
	 * @throws InterruptedException
	 */
	public void service() {
		try {
			initStream(); // 初始化输入输出流对象
			
			String msg = br.readLine();
			System.out.println(msg); // 输出客户端连接TCP服务器端成功的信息

			Scanner in = new Scanner(System.in); // 接受用户信息
			String[] cmd = null;

			do {
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
				} else {
					do {
						String arg = br.readLine(); // 获取服务器传来的文件信息
						if (!arg.equals("end")) {
							System.out.println(arg); // 如果不是结束符，则输出文件信息
						} else {
							break;
						}
					} while (true);
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

	/**
	 * inform 在传输文件之前，客户端通知服务器端，告知其IP地址和端口号
	 * 
	 * 更新：服务器不再回复客户端
	 * @throws IOException
	 */
	public void inform() throws IOException {
		SocketAddress socketAddres = new InetSocketAddress(remoteIp, UDP_PORT); // 指定服务器端地址

		String s = "give the address and port of Client";
		byte[] info = s.getBytes();
		// 创建数据包，指定服务器地址
		DatagramPacket dp = new DatagramPacket(info, info.length, socketAddres);
		dgSocket.send(dp); // 向服务器端发送数据包
	}

	/**
	 * receiveFile 接收服务器端传来的文件
	 * @param fileName 服务器传来的文件名字
	 * @throws IOException
	 */
	public void receiveFile(String fileName) throws IOException {
		try {
			os = new FileOutputStream(new File(fileName)); // new File会在当前目录下生成文件（相当于在当前目录下接收文件）
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
