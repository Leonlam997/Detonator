package com.leon.detonator.mina.client;

import android.os.Handler;
import android.os.Message;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

import java.util.Arrays;


public class MinaHandler extends IoHandlerAdapter {
    public final static int MINA_DATA = 100;
    public final static int MINA_NORMAL = 101;
    public final static int MINA_ERROR = 102;
    private final Handler msgHandler;

    MinaHandler(Handler msgHandler) {
        this.msgHandler = msgHandler;
    }

    public void messageReceived(IoSession session, Object message) {
        if (message == null) {
            sendMsg(MINA_ERROR, "发生异常！");
            return;
        }
        int i = 0;
        for (; i < ((byte[]) message).length; i++)
            if ((byte) 0 == ((byte[]) message)[i])
                break;
        sendMsg(MINA_DATA, new String(Arrays.copyOfRange(((byte[]) message), 0, i)));
    }

    public void messageSent(IoSession session, Object message) {
        //BaseApplication.writeFile(new String((byte[]) message));
        //sendMsg(3,"发送数据:"+new String((byte[]) message));
    }

    public void sessionClosed(IoSession session) {
        session.close(true);
        sendMsg(MINA_NORMAL, "连接关闭！");
    }

    public void sessionCreated(IoSession session) {
        IoSessionConfig cfg1 = session.getConfig();
        if (cfg1 instanceof SocketSessionConfig) {
            SocketSessionConfig cfg = (SocketSessionConfig) session.getConfig();
            // ((SocketSessionConfig) cfg).setReceiveBufferSize(4096);
            cfg.setReceiveBufferSize(2 * 1024 * 1024);
            cfg.setReadBufferSize(2 * 1024 * 1024);
            cfg.setKeepAlive(true);
            // if (session.== TransportType.SOCKET) {
            // ((SocketSessionConfig) cfg).setKeepAlive(true);
            cfg.setSoLinger(0);
            cfg.setTcpNoDelay(true);
            cfg.setWriteTimeout(1000);
        }
    }

    public void sessionIdle(IoSession session, IdleStatus idle)
            throws Exception {
        sendMsg(MINA_NORMAL, "连接超时关闭！");
    }

    public void sessionOpened(IoSession session) {
        sendMsg(MINA_NORMAL, "连接建立！");
    }

    private void sendMsg(int what, String msg) {
        Message message = msgHandler.obtainMessage(what);
        message.obj = msg;
        msgHandler.sendMessage(message);
    }
}
