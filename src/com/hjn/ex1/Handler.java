package com.hjn.ex1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Handler 处理客户端的请求，包括"cd"、"ls"、"get"、"quit"请求
 * @author HJN
 *
 */
public class Handler implements Runnable { // 负责与单个客户通信的线程
	private Socket socket;
	private SocketAddress socketAddress;
	private DatagramSocket dgSocket; // 客户端DatagramSocket

	private String BASE_PATH = null;
	private String CURRENT_PATH = null;
	private List<String> CURRENT_FILE = new ArrayList<String>(); // 存储当前目录下的文件列表
	private List<String> CURRENT_DIR = new ArrayList<String>(); // 存储当前目录下的文件夹列表
	
	private static final int BYTE_LEN = 8192; // 设置每次传输数据的长度

	BufferedReader br;
	BufferedWriter bw;
	PrintWriter pw;
	InputStream is;

	/**
	 * Handler 构造函数，处理与一个客户端的连接
	 * @param socket FileServer传过来的服务器TCPsocket
	 * @param dgSocket FileServer传过来的服务器UDPsocket
	 * @param BASE_PATH FileServer传过来的文件存储根目录
	 * @throws SocketException
	 */
	public Handler(Socket socket, DatagramSocket dgSocket, String BASE_PATH) throws SocketException {
		this.socket = socket;
		this.dgSocket = dgSocket;
		this.BASE_PATH = BASE_PATH;
		this.CURRENT_PATH = BASE_PATH;
	}

	/**
	 * initStream 初始化字符读入和写出流
	 * @throws IOException
	 */
	public void initStream() throws IOException { // 初始化输入输出流对象方法
		br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		pw = new PrintWriter(bw, true);
	}

	/**
	 * run 自动调用该方法，处理客户端传来的请求
	 */
	public void run() { // 执行的内容
		try {
			System.out.println("新连接，连接地址：" + socket.getInetAddress() + "：" + socket.getPort()); // 客户端信息
			initStream(); // 初始化输入输出流对象
			
			pw.println(socket.getInetAddress() + ":" + socket.getPort() + ">" + "连接成功"); // 给客户端传递“连接成功”的信息
			
			String info = null;
			String[] cmd = null;
			while (null != (info = br.readLine())) {
				cmd = info.split(" ");
				System.out.println(socket.getInetAddress() + "," + socket.getPort() + " : " + info);
				if (cmd[0].equals("bye")) { // 如果用户输入“bye”就退出
					break;
				} else if (cmd[0].equals("get")) {
					String fileName = cmd[1];
					receiveInform();
					// 从本地抓取文件，发送给客户端
					sendFile(fileName);
				} else if (cmd[0].equals("ls")) {
					ls();
				} else if (cmd[0].equals("cd")) {
//					cdRespond();
					if (cmd.length == 2) {
						cd(cmd[1]);
					} else {
						pw.println("unknown cmd");
						pw.println("end");
					}
				} else if (!cmd[0].equals("cd") && !cmd[0].equals("ls") && !cmd[0].equals("bye")
						&& !cmd[0].equals("get")) {
					pw.println("unknown cmd");
					pw.println("end");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != socket) {
				try {
					socket.close();
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
		File dir = new File(this.CURRENT_PATH);
		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("目录不存在，更新当前文件和目录出错");
			return;
		}
		String[] files = dir.list();
		for (int i = 0; i < files.length; i++) {
			File file = new File(dir, files[i]);
			if (file.isFile()) { // 该对象是file
				CURRENT_FILE.add(file.getName());
			} else { // 该对象是dir
				CURRENT_DIR.add(file.getName());
			}
		}
		System.out.println("更新当前文件和目录完毕");
	}
	
	public void cd(String cmd) throws IOException {
		String path = null;
		if (cmd.equals("..")) {
			if (CURRENT_PATH.equals(BASE_PATH)) {
				pw.println(CURRENT_PATH + " > " + "OK");
				pw.println("end");
				System.out.println("已在根目录");
				updateCurrentFile();
				return;
			} else {
				File temp = new File(CURRENT_PATH);
				path = temp.getParent();
				CURRENT_PATH = path;
				pw.println(CURRENT_PATH + " > " + "OK");
				pw.println("end");
				System.out.println("回到父目录");
				updateCurrentFile();
				return;
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
				pw.println(CURRENT_PATH + " > " + "OK");
				pw.println("end");
				System.out.println("转移到\t" + path);
				updateCurrentFile();
				return;
			} else {
				pw.println("unknown dir");
				pw.println("end");
				System.out.println("没有该目录");
				updateCurrentFile();
				return;
			}
		}

	}

	public void ls() throws IOException {
		System.out.println("开始发送目录...");
		File dir = new File(this.CURRENT_PATH);
		if (!dir.exists() || !dir.isDirectory()) {
			System.out.println("目录不存在");
			return;
		}
		String[] files = dir.list();
		for (int i = 0; i < files.length; i++) {
			File file = new File(dir, files[i]);
			if (file.isFile()) {
				pw.println("<file>\t" + file.getName() + "\t" + formatFileSize(file.length()));
			} else {
				pw.println("<dir>\t" + file.getName() + "\t" + formatFileSize(getFolderSize(file)));
			}
		}
		pw.println("end");
		System.out.println("发送目录成功");
	}
	
	/**
	 * formatFileSize 输入文件的大小，返回文件的格式化大小
	 * @param file 文件的大小
	 * @return 返回文件的格式化大小，带单位
	 */
	public static String formatFileSize(long file) {
		DecimalFormat df = new DecimalFormat("#.00");
		String fileSizeString = "";
		if (file < 1024) {
			fileSizeString = df.format((double) file) + "B";
		} else if (file < 1048576) {
			fileSizeString = df.format((double) file / 1024) + "KB";
		} else if (file < 1073741824) {
			fileSizeString = df.format((double) file / 1048576) + "MB";
		} else {
			fileSizeString = df.format((double) file / 1073741824) + "GB";
		}
		return fileSizeString;
	}
	
	/**
	 * getFolderSize 获取文件夹（包含其子文件）所占空间大小
	 * @param file 要查询的文件对象
	 * @return 返回文件夹（包含其子文件）所占空间大小
	 */
	public static long getFolderSize(File file) {
		long size = 0;
		try {
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				if (fileList[i].isDirectory()) {
					size = size + getFolderSize(fileList[i]);
				} else {
					size = size + fileList[i].length();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return size;
	}

	/**
	 * receiveInform 接收客户端UDP传来的信息，借此获取客户端UDP的端口号和地址
	 * @throws IOException
	 */
	public void receiveInform() throws IOException {
		DatagramPacket dp = new DatagramPacket(new byte[512], 512);
		dgSocket.receive(dp); // 接收客户端信息
		String msg = new String(dp.getData(), 0, dp.getLength());
		// 获取客户端信息
		System.out.println(dp.getAddress() + "," + dp.getPort() + " : " + msg);
		this.socketAddress = new InetSocketAddress(dp.getAddress(), dp.getPort()); // 指定UDP客户端地址
	}

	/**
	 * sendFile 服务器端通过UDP，发送文件给客户端
	 * @param fileName 需要发送的文件名
	 */
	public void sendFile(String fileName) {
		System.out.println("开始发送文件：" + fileName);
		try {
			try {
				is = new FileInputStream(new File(CURRENT_PATH + "\\" + fileName));
			} catch (FileNotFoundException e) {
				System.out.println("不存在该文件，取消文件传输");
				pw.println("不存在该文件，取消文件传输");
//				e.printStackTrace();
				return;
			}
			// 传两次，因为客户端br.readLine()使用了两次【readLine()函数使用一次会移动读取指针】
			pw.println(is.available()); // 告诉客户端要接收的文件的大小（单位：byte）
			pw.println(is.available()); // 告诉客户端要接收的文件的大小（单位：byte）
			byte[] b = new byte[BYTE_LEN];
			// 创建UDP数据报
			while (is.read(b) != -1) {
				DatagramPacket dp = new DatagramPacket(b, b.length, socketAddress);
				dgSocket.send(dp);
				TimeUnit.MICROSECONDS.sleep(100); // 限制传输速度
			}
			System.out.println("文件发送成功");

		} catch (InterruptedException e) {
			System.out.println("文件接收失败");
			e.printStackTrace();
		} catch (SocketException e) {
			System.out.println("文件接收失败");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("文件接收失败");
			e.printStackTrace();
		}
	}
}
