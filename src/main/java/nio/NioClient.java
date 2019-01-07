package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * @author syz
 * @date 2019-01-07 20:36
 */
public class NioClient {
    private int port = 8080;
    private Charset charset = Charset.forName("UTF-8");
    private Selector selector;
    private String nick = "";
    private final String USER_EXIT = "对不起，当前昵称已经存在，请换一个昵称";
    private final String USER_CONTENT_SPLIT = "#@#";
    private SocketChannel client;
    private Set<String> users = new HashSet<>();
    public NioClient(int port) throws IOException {
         client = SocketChannel.open(new InetSocketAddress("localhost",this.port));
        client.configureBlocking(false);
        selector = Selector.open();
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("服务已经连接，监听端口是 ：" + this.port);
    }
    public void session (){
        new Reader().start();
        new Writer().start();
    }
    public class Reader extends Thread {

        @Override
        public void run() {
            try {
                while (true) {
                    int wait = selector.select();
                    if (wait == 0) continue;
                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> it = keys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();
                        keys.remove(key);
                        process(key);
                    }

                }
            }catch (Exception e) {
                e.printStackTrace();
            }



        }
    }
    public class Writer extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("".equals(line)) continue;
                if ("".equals(nick)) {
                    nick = line;
                    line = nick + USER_CONTENT_SPLIT;
                } else {
                    line = nick + USER_CONTENT_SPLIT +line;
                }
                try {
                    client.write(charset.encode(line));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            scanner.close();
        }
    }
    public void process (SelectionKey key) throws IOException {
        if (key.isReadable()) {
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder content = new StringBuilder();
            while (client.read(buffer) > 0) {
                buffer.flip();
                content.append(charset.decode(buffer));
            }
            if (client.equals(USER_EXIT)) {
                nick = "";
            }
            System.out.println(content);
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    public static void main(String[] args) throws IOException {
        new NioClient(8080).session();
    }
}
