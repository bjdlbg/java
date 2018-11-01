

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class MainClass {
    // RxTx comm协议：
    // StandardCharsets.UTF_8字符​​串，以CR作为分隔符
    // NUL，LF和CR将从rx'ed字符串中删除
    private static final int NUL = (byte)'\0';
    private static final int LF = (byte)'\n';
    private static final int CR = (byte)'\r';

    //将以下内容声明为静态，以便只有一个单独的
    //无论创建多少个对象，都要在内存中复制它们
    //以及启动了多少个连接
    private static List<String> resultList = Collections.synchronizedList(new ArrayList<>());
    private static InputStream in = null;
    private static OutputStream out = null;
    private static SerialPort serialPort = null;
    private static int numberOfCopy = 0;
    //声明这些以便我们可以跟踪它们并完成所有操作
    // COM端口关闭时要删除的工作线程
    private Thread readThread = null;
    private Thread writeThread = null;

    /**
     * Constructor
     */
    public MainClass() {
        super();
        numberOfCopy++;
    }

    /**
     * Connect to a given COM port
     * 连接到给定的COM端口
     * @param portName
     * @throws Exception
     */
    public void connect ( String portName ) throws Exception
    {
        CommPortIdentifier portIdentifier = CommPortIdentifier
                .getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() ) {
            //端口可能已经打开并且可用
            return;
        } else {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),
                    2000);
            if ( commPort instanceof SerialPort ) {
                serialPort = (SerialPort) commPort;
                //选择端口波特率，数据位，结束位，奇偶校验为none
                serialPort.setSerialPortParams(9600,	//57600,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);


                serialPort.setFlowControlMode(
                        SerialPort.FLOWCONTROL_RTSCTS_IN);
                serialPort.setRTS(true);

                in = serialPort.getInputStream();
                out = serialPort.getOutputStream();

                //让工作线程连续读取端口
                readThread = new Thread(new SerialReader(in));
                readThread.start();
            } else {
                throw new Exception("Error: Only serial ports are "
                        + "handled by this example.");
            }
        }
    }

    /**
     * Close a connection and release all resources if it is the last one
     * 关闭连接并释放所有资源（如果它是最后一个）
     * @return the number of instances still in running
     * @return 仍在运行的实例数
     */
    public int close() {
        if (--numberOfCopy > 0) return numberOfCopy;

        if (readThread != null) {
            readThread.interrupt();
            try {
                readThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readThread = null;
        }
        if (writeThread != null) {
            writeThread.interrupt();
            try {
                writeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writeThread = null;
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            in = null;
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = null;
        }
        if (serialPort != null) {
            serialPort.close();
            serialPort = null;
        }
        return 0;
    }

    /**
     * Close all connections
     */
    public void closeAll() {
        //force to last copy left and close
        numberOfCopy = 1;
        close();
    }

    /**
     * Read the oldest string received and remove it from the list
     * 读取收到的最旧字符串并将其从列表中删除
     * 从com读取
     * @return string of data or null otherwise
     */
    public String read() {
        synchronized (resultList) {
            if (resultList.size() > 0) {
                String data = resultList.get(0);
                resultList.remove(0);
                return data;
            }
        }
        return null;
    }

    /**
     * Write data in byte[]
     * @param data
     */
    public void write(byte[] data) {
        //wait for 1sec if the previous writing has not finished
        //and then force it to stop
        if (writeThread != null) {
            try {
                writeThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            writeThread.interrupt();
        }
        //let the working thread to write to COM port
        writeThread = new Thread(new SerialWriter(out, data));
        writeThread.start();
    }

    /**
     * Write data in string
     * @param data
     */
    public void write(String data) {
        write(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Helper method to add a string to the list, thread-safe
     * @param data
     */
    private static void addToList(String data) {
        synchronized (resultList) {
            resultList.add(data);
        }
    }

    /**
     * @author liy
     * 工作线程连续读取端口
     * 将结果添加到列表中。可以在外部中断
     */
    private static class SerialReader implements Runnable {
        InputStream in;

        public SerialReader ( InputStream in ) {
            this.in = in;
        }

        @Override
        public void run () {
            byte[] buffer = new byte[1024];
            int len = -1;
            byte[] data = new  byte[512];
            int dataIndex = 0;
            try {
                while ( true ) {
                    //读取输入流并决定是否中断并继续
                    len = this.in.read(buffer);
                    if (len < 0 || Thread.interrupted()) {
                        break;
                    } else if (len==0) {
                        Thread.sleep(100);
                        continue;
                    }

                    //读取输入流并决定是否中断并继续
                    for (int i = 0; i < len; i++) {
                        byte currentByte = buffer[i];
                        //ignore NUL and LF
                        if ((currentByte != NUL) && (currentByte != LF)) {
                            //CR is the delimiter
                            if (currentByte==CR) {
                                if (dataIndex != 0) {
                                    //收到一个完整的字符串，保存
                                    addToList(new String(data, 0, dataIndex,
                                            StandardCharsets.UTF_8));
                                    dataIndex = 0;	//ready for the next string
                                }
                            } else {
                                data[dataIndex++] = currentByte;
                            }
                        }
                    }

                }
            } catch ( IOException e ) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @author liy
     * 工作线程将数据写入端口直到完成
     * 可以在外部中断
     */
    private static class SerialWriter implements Runnable {
        OutputStream out;
        byte[] data;

        public SerialWriter ( OutputStream out, byte[] data ) {
            this.out = out;
            this.data = data;
        }

        @Override
        public void run () {
            try {
                for (int i = 0; i < data.length; i++) {
                    if (Thread.interrupted()) break;
                    this.out.write(data[i]);
                }
                this.out.flush();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

}
