folderTransporterWithinLAN
==========================

It can transports folder within LAN.


The usage tutorial is as below:
该软件的使用方法如下：
1、进入源码路径，分别执行以下两条命令：
	java Receiver
	java Sender


2、接下来Sender弹出input输入框，要求输入Receiver的ip地址，
在输入框中输入Receiver的ip地址（如本机127.0.0.1），并点击
OK按钮进入下一步；


3、接着Receiver弹出input输入框，要求输入存储路径，在输入
框中输入要存储的路径即可；
如 /home/zdb/store
注意：路径须为绝对路径，并且home目录不能用~表示，须用
/home/usrname的形式表示！！！


4、接下来Sender又弹出input输入框，要求输入发送目录，在输入
框中输入要发送的目录即可；
如，/home/zdb/javaTest
同样，路径须为绝对路径，并且home目录不能用~表示，须用
/home/usrname的形式表示！！！


5、在第4步点击OK之后，Sender便向Receiver发送数据，同时
Receiver将接受到的文件原样存储在指定路径下。另外，在传输
过程中，可以看到相应的传输信息。
