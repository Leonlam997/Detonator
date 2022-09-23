package com.leon.detonator.mina.server;

import com.leon.detonator.mina.vo.MinaMessageRec;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.transport.socket.SocketSessionConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MinaHandler extends IoHandlerAdapter {

    //private static Logger logger = Logger.getLogger(MinaHandler.class);

    private static ConcurrentMap<String, IoSession> connctionMap = new ConcurrentHashMap<String, IoSession>();

    public void messageReceived(IoSession session, Object message) {
        if (message == null) {
            //logger.error("�쳣����");
            return;
        }
        //ע:message������MessageCodecFactory.decoder����,����ǿתΪ��decoder���ص�����
        MinaMessageRec mess = (MinaMessageRec) message;
        //����,��MinaServer���߳�Ӧ����.
        session.setAttribute("sn", mess.getSn());
        //��������,����Ӧ��
        connctionMap.put(mess.getSn(), session);
        //logger.info("ȱʧ������:" + mess.getUnPackNo().size());
        if (!mess.isOver() &&
                mess.getUnPackNo().size() == 0) {
            mess.setOver(true);//�����C�߳�������̰߳�ȫ����.��������ʱ������ظ�
//			MinaMessageOp.msgMap.remove(mess.getSn());
            //��messʵ���������ݿ�,�ٴ��ڼ������ʧ����ʹ��sessionӦ��Է�(���Է�˵��Ӧ����������Ӧ��,���ڷ�����Ϻ�4���ӻ�Ͽ�����)
            //logger.info("�յ���������:" + mess.getQbDate());
            // ��������
        }
    }

    public void messageSent(IoSession session, Object message) {
        //ע:message������MessageCodecFactory.encoder����,����ǿתΪ��decoder���ص�����
        //logger.info(session.getId() + "��������:" + new String((byte[]) message));
    }

    public void sessionClosed(IoSession session) {
        connctionMap.remove(session.getAttribute("sn"));
        session.close(true);
        //logger.info(session.getId() + "���ӹر�:" + session.getAttribute("sn"));
        //System.out.println(session.getId() + "���ӹر�:" + session.getAttribute("sn"));
    }

    public void sessionCreated(IoSession session) {
        // ���������ӱ�����ʱ�˷��������ã�����϶���sessionOpened(IoSession
        // session)����֮ǰ�����ã���������Զ�Socket����һЩ�������
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
            cfg.setWriteTimeout(1000 * 10);
        }
        //logger.info(session.getId() + "���Ӵ���");
    }

    public void sessionIdle(IoSession session, IdleStatus idle)
            throws Exception {
        String sn = String.valueOf(session.getAttribute("sn"));
        MinaMessageRec rec = MinaMessageOp.msgMap.get(sn);
        if (rec == null || (rec.getUnPackNo().size() == 0 && rec.getSendCount() == 0)) { // ��ȷ�������ݰ����ѻظ�3�κ�ر�����
            sessionClosed(session);
        }
    }

    public void sessionOpened(IoSession session) {
        //logger.info(session.getId()+ "���ӳ�ʱ�ر�");
    }

    public IoSession getConnction(String sn) {
        //logger.info(sn + "���ӽ���");
        return connctionMap.get(sn);
    }
}
