# 网络与分布式计算_实验1



## 1 简介：

Network_ex_1是一个eclipse的java project，其功能是实现本地和服务器之间的文件传输和信息交互。



## 2 如何使用：

1. 用Eclipse从外部文件夹打开本项目，打开Eclipse，选择"File"下的"Open Projects from File System"

2. 导入项目之后，右击项目名，选择选项"Run As->Run configuration"

   ```
   配置FileServer的Arguments为
   "你的Eclipse工作目录的路径\Network_ex\src"
   
   比如：
   我的Eclipse工作目录的路径为
   "E:\software Design\Java_web_learning"
   
   则我的FileServer的Arguments为
   "E:\software Design\Java_web_learning\Network_ex\src"
   ```

   ```
   配置FileClient的Arguments为
   服务器TCP的IP地址 服务器TCP的端口号 服务器UDP的IP地址 服务器UDP的端口号
   
   比如：
   服务器TCP的IP地址为127.0.0.1
   服务器TCP的端口号为2021
   服务器UDP的IP地址为127.0.0.1
   服务器UDP的端口号为2021
   
   则我的FileServer的Arguments为
   "127.0.0.1" "2021" "127.0.0.1" "2020"
   ```

3. 分别运行FileServer.java和FileClient.java，开始使用本项目



## 3 客户端——主要功能

1. 功能1：服务器返回当前目录文件列表

   1. 输入（输入命令后请按回车键）：

      ```
      ls
      ```

      

2. 功能2：进入指定目录

   1. 输入（输入命令后请按回车键）：

      ```
      cd <dir>
      ```

   2. 注意：如果该目录不存在，控制台会输出

      ```
      unknown dir
      ```

      

3. 功能3：退到上一级目录（但当前目录为根目录时，不做变动）

   1. 输入（输入命令后请按回车键）：

      ```
      cd ..
      ```

4. 功能4：通过UDP下载指定文件，保存到客户端当前目录下

   1. 输入（输入命令后请按回车键）：

      ```
      get <file>
      ```

   2. 注意：如果该文件不存在，控制台会输出

      ```
      unknown file
      ```

5. 功能5：断开连接，结束客户端的运行

   1. 输入（输入命令后请按回车键）：

      ```
      bye
      ```





## 4 遇到问题？

请联系作者——hjn

QQ：1286039722@qq.com

