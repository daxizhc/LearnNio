package com.tony.learn.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class NioServer {

    public static void main(String[] args) throws IOException {
        NioServer nioServer = new NioServer();
        nioServer.start();
    }

    public void start() throws IOException {
        //1、创建Selector
        Selector selector = Selector.open();

        //2、通过ServerSocketChannel创建channel通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();

        //3、为channel通道绑定监听端口
        serverSocketChannel.bind(new InetSocketAddress(8000));

        //4、设置channel为非阻塞模式
        serverSocketChannel.configureBlocking(false);

        //5、将channel注册到selector上，监听连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功");

        //6、循环等待新接入的连接
        while (true){
            // 阻塞方法，获取可用channel数量
            // 空轮询bug 在linux下，不会阻塞，cpu利用率100%，
            int readyChannels = selector.select();

            if(readyChannels == 0) continue;

            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()){
                SelectionKey selectionKey = iterator.next();

                // 移除Set中的当前selectionKey
                iterator.remove();

                //7、根据就绪状态，调用对应方法处理
                // 如果是接入事件
                if(selectionKey.isAcceptable()){
                    acceptHandler(serverSocketChannel, selector);
                }
                // 如果是可读事件
                if(selectionKey.isReadable()){
                    readHandler(selectionKey, selector);
                }
            }
        }
    }

    private void acceptHandler(ServerSocketChannel serverSocketChannel, Selector selector) throws IOException {
        // 如果是接入事件，创建socketChannel
        SocketChannel socketChannel = serverSocketChannel.accept();
        // 将socketChannel设置为非阻塞工作模式
        socketChannel.configureBlocking(false);
        // 将channel注册到selector上，监听可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);
        // 回复客户端提示信息
        socketChannel.write(Charset.forName("UTF-8").encode("你与聊天室其他人都不是朋友关系，请注意隐私安全"));
    }

    @SuppressWarnings("Duplicates")
    private void readHandler(SelectionKey selectionKey, Selector selector) throws IOException {
        // 从selectionKey中获取就绪的channel
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        // 创建buffer
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 使用buffer循环读取客户端请求信息
        StringBuilder request = new StringBuilder();
        while (socketChannel.read(byteBuffer) > 0){
            // 切换buffer为读模式
            byteBuffer.flip();
            //读取buffer中的内容
            request.append(Charset.forName("UTF-8").decode(byteBuffer));
        }

        // 将channel再次注册到selector上，监听他的可读事件
        socketChannel.register(selector, SelectionKey.OP_READ);

        // 将客户端发送的请求信息广播給其他的客户端
        if(request.length() > 0){
            System.out.println(":: " + request);
            broadCast(selector, socketChannel, request.toString());
        }
    }

    private void broadCast(Selector selector, SocketChannel sourceChannel, String request) throws IOException {
        // 获取到所以已接入的客户端channel
        Set<SelectionKey> selectionKeySet = selector.keys();

        for (SelectionKey selectionKey : selectionKeySet) {
            Channel targetChannel = selectionKey.channel();
            // 剔除发消息的客户端
            if(targetChannel instanceof SocketChannel && targetChannel != sourceChannel){
                // 将消息广播
                ((SocketChannel) targetChannel).write(Charset.forName("UTF-8").encode(request));
            }
        }
    }

}
