package com.hjn.ex1;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hjn.ex1.Handler;

/**
 * @author HJN
 *
 */
public class FileServer {
	ServerSocket serverSocket;
	private static final int PORT = 2021; // 端口
	private DatagramSocket dgSocket; // 客户端DatagramSocket
	private static final int UDP_PORT = 2020; // UDP端口号
	private static String BASE_PATH = null;

	ExecutorService executorService; // 线程池
	final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

	public FileServer() throws IOException {
		serverSocket = new ServerSocket(PORT); // 创建服务器端套接字

		// 创建线程池
		// Runtime的availableProcessors()方法返回当前系统可用处理器的数目
		// 由JVM根据系统的情况来决定线程的数量
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);

		System.out.println("服务器启动。");
		dgSocket = new DatagramSocket(UDP_PORT);
	}

	public static void main(String[] args) throws IOException {
		/*
		 * 实现：服务器端启动时需传递root目录参数，并校验该目录是否有效；
		 */
		try {
			if (args.length == 0)
				throw new IllegalArgumentException("输入数据为空");
			BASE_PATH = args[0];
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
		}

		File check = new File(BASE_PATH);
		if (check.exists()) {
//			System.out.println("root目录有效");
		} else {
			System.out.println("root目录无效，请输入正确的root目录后，重启服务器");
			return;
		}

		new FileServer().service(); // 启动服务
	}

	/**
	 * service implements
	 */
	public void service() {

		while (true) {
			Socket socket = null;
			try {
				socket = serverSocket.accept(); // 接受到此套接字的连接
				executorService.execute(new Handler(socket, dgSocket, BASE_PATH)); // 把执行交给线程池来维护
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 此处不能close socket，要维持之前的所有客户连接
		}

	}

}
