package nio;


import com.sun.org.apache.bcel.internal.generic.Select;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.sql.ClientInfoStatus;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author syz
 * @date 2019-01-07 20:36
 */
public class NioServer {
    private int port = 8080;
    private Charset charset = Charset.forName("UTF-8");
    private Selector selector;
    private final String USER_EXIT = "对不起，当前昵称已经存在，请换一个昵称";
    private final String USER_CONTENT_SPLIT = "#@#";
    private Set<String> users = new HashSet<>();
    public NioServer(int port) throws IOException {
        this.port = port;
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(this.port));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务已经连接，监听端口是 ：" + this.port);
    }
    public void listener () throws IOException {
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
    }
    public void process (SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            client.register(selector,SelectionKey.OP_READ);
            key.interestOps(SelectionKey.OP_ACCEPT);
            client.write(charset.encode("请输入你的昵称： "));
        }
        if (key.isReadable()) {
            SocketChannel client = (SocketChannel)key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            StringBuilder content = new StringBuilder();
            try{
                 while (client.read(buffer) > 0) {
                     buffer.flip();
                     content.append(charset.decode(buffer));
                 }
                 key.interestOps(SelectionKey.OP_READ);
            }catch (Exception e) {
                key.cancel();
                if (key.channel() != null) {
                    key.channel().close();
                }
                e.printStackTrace();
            }
            if (content.length() > 0) {
                String[] contentArr = content.toString().split(USER_CONTENT_SPLIT);
                if (contentArr != null && contentArr.length == 1) {
                    String nick = contentArr[0];
                    if (users.contains(nick)) {
                        client.write(charset.encode(USER_EXIT));
                    } else {
                        users.add(nick);
                        int count = onlineCount();
                        String message = "你好，欢迎进入聊天室，当前在线人数为："+ count;
                        broadcast(null,message);
                    }
                } else if (contentArr != null && contentArr.length > 0) {
                    String nick = contentArr[0];
                    String message = content.substring(nick.length()  +USER_CONTENT_SPLIT.length());
                    message = nick +" 说： " + message;
                    broadcast(client,message);

                }
            }
        }
    }
    public int onlineCount(){
        int res  = 0;
        for (SelectionKey key:selector.keys()) {
            Channel target = key.channel();
            if (target instanceof  SocketChannel) {
                res++;
            }
        }
        return res;
    }
    public void broadcast(SocketChannel client,String content) throws IOException {
        for (SelectionKey key:selector.keys()) {
            Channel target = key.channel();
            if (target != client && target instanceof  SocketChannel) {
                 ((SocketChannel) target).write(charset.encode(content));
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new NioServer(8080).listener();
    }
}
