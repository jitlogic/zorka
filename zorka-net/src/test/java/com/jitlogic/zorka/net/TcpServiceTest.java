package com.jitlogic.zorka.net;

import com.jitlogic.zorka.common.util.ZorkaConfig;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.impl.ConsoleTrapper;
import org.slf4j.impl.ZorkaLoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TcpServiceTest {

    private static Executor executor = Executors.newSingleThreadExecutor();

    private static class EchoSessionFactory implements TcpSessionFactory {

        @Override
        public Runnable getSession(final Socket socket) {
            return new Runnable() {
                @Override
                public void run() {
                    try {
                        InputStream is = socket.getInputStream();
                        OutputStream os = socket.getOutputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        String line = br.readLine();
                        os.write(line.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
        }
    }

    @Test @Ignore
    public void testEchoService() throws Exception {
        ZorkaLoggerFactory.getInstance().swapTrapper(new ConsoleTrapper());
        TcpService svc = new TcpService(new ZorkaConfig(), executor, new EchoSessionFactory(),
                "test", "127.0.0.1", 10000);
        svc.restart();
        Thread.sleep(120000);
    }

}
