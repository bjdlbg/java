# java
串口通信程序以及三方jar包（serial port communication of Java）

### 电脑端的串口接收端口
AccessPort
非常常用的一个调试工具，可以设置波特率等参数。打开一个端口以及接受或读取数据。

### RXTXcomm.jar
第三方库支持，原生java环境也就是安装的jdk和jre中没有该jar包，需要手动导入
1. 首先需要判断本地java的版本，之后解压安装对应的jar包（32bit\64bit)
2. 将RXTXcomm.jar放入本地java安装目录中，jdk\jre\lib\ext中
3. rxtxParallel.dll与rxtxSerial.dll两个文件需要放在jdk\jre\bin中
（也可以使用ide手动导入jar包为三方依赖）

### 物联网开发需要的vspd
这是一个可以在电脑端口虚拟出来的两个串口，并且可以是这两个串口连到一起，达到串口串口通讯的目的。

### 在串口中调试串口
手动传入串口的name，调整波特率等参数
主函数为Win10RXTXtest.java


