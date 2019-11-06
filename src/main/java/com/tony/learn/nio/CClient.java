package com.tony.learn.nio;

import java.io.IOException;

public class CClient {

    public static void main(String[] args) throws IOException {
        new NioClient().start("c");
    }

}
