
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class Win10RxTXTest {

    public static void main ( String[] args ) {
        MainClass rxtx = new MainClass();
        try {
            rxtx.connect("COM2");

            //定义一个工作线程来读取RxTx并在控制台中显示
            Thread readData = new Thread(new Runnable() {
                @Override
                public void run() {
                    String data;
                    while(true) {
                        if (Thread.interrupted()) break;
                        data = rxtx.read();
                        if (data != null) {
                            System.out.print(data);

                            //数据到达时的实时
                            System.out.println(
                                    (new SimpleDateFormat(" {HH:mm:ss}"))
                                            .format(new Date()));
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }
            });
            readData.start();

            //定义一个输入线程以从用户获取输入并发送到RxTx
            Thread userInput = new Thread(new Runnable() {
                @Override
                public void run() {
                    int c = 0;
                    byte[] buf = new byte[1204];
                    try {
                        while ( ( c = System.in.read(buf)) > -1 )
                        {
                            if (Thread.interrupted()) break;
                            //"---" is a string to terminate the program
                            if (c > 0) {
                                if (c==5 && buf[0]=='-'
                                        && buf[1]=='-' && buf[2]=='-') {
                                    break;
                                }
                                //"+++" is a special string sent to RxTx
                                if (c==5 && buf[0]=='+'
                                        && buf[1]=='+' && buf[2]=='+') {
                                    c = 3;
                                }
                                rxtx.write(Arrays.copyOf(buf, c));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            userInput.start();

            //通常程序由用户终止
            //所以我们等待输入线程完成
            userInput.join();
            readData.interrupt();
            readData.join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭连接并且释放所有资源
            rxtx.close();
            System.out.println("Finished!");
        }
    }
}