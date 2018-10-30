package com.jitlogic.zorka.net;

import java.net.Socket;

public interface TcpSessionFactory {

    Runnable getSession(Socket socket);

}
