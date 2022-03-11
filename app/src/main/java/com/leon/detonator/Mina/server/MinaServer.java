package com.leon.detonator.Mina.server;

import com.leon.detonator.Mina.vo.MinaMessageRec;
import com.leon.detonator.Mina.vo.MinaMessageSend;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MinaServer {

    private static MinaServer minaServer = null;
    public boolean isOpen = false;
    public IoAcceptor dataAccepter = null;
    private MinaHandler handler = new MinaHandler();
    private Thread bufaProcess = null;
    private boolean bufaProcessSwitch = false;

    private ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);//�̳߳�

    private MinaServer() {
        //����ģʽ
    }

    public static MinaServer getInstance() {
        if (minaServer == null) {
            minaServer = new MinaServer();
        }
        return minaServer;
    }

    public static void main(String[] args) {
        MinaServer.getInstance().start();
//		MinaServer.getInstance().stop();
    }

    /**
     * ����mina������,������Ӧ�ն�����
     *
     * @return
     */
    public boolean start() {
        try {
            dataAccepter = new NioSocketAcceptor();
            //�趨��־���˼���
            LoggingFilter log = new LoggingFilter();
            log.setMessageReceivedLogLevel(LogLevel.WARN);
            dataAccepter.getFilterChain().addLast("logger", log);

            //���ý�����(����ʱ)�ͱ�����(����ʱ)
            dataAccepter.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MessageCodecFactory()));

            //�趨���ò���
            IoSessionConfig config = dataAccepter.getSessionConfig();
            config.setReadBufferSize(4096);
            config.setWriteTimeout(1000 * 10);
            config.setWriterIdleTime(100000);
            config.setIdleTime(IdleStatus.BOTH_IDLE, 30);

            //���ñ���ͽ����Ļص���
            dataAccepter.setHandler(handler);
            //�󶨼����˿�,�˴�ӦΪ������IP,Ϊ127.0.0.1����ʹ��IP���ӱ�����
            dataAccepter.bind(new InetSocketAddress(1089));

            //logger.info("Mina�����������ɹ�!�˿ں�:" + 1089);
            System.out.println("Mina�����������ɹ�!�˿ں�:" + 1089);
            isOpen = true;

            bufaProcessSwitch = true;
            bufaProcess = new Thread(new Runnable() {
                public void run() {
                    int times = 0;//����
                    try {
                        while (bufaProcessSwitch) {
//							yingda(times);
                            yingdaBufa(times);
                            //����
                            times++;
                            Thread.sleep(5000L);
                        }
                    } catch (Exception e) {
                        //logger.error(e);
                    }
                }
            });
            bufaProcess.start();
        } catch (Exception e) {
            isOpen = false;
            //logger.error("Mina����������ʧ��:" + e);
        }
        return isOpen;
    }


    protected void yingdaBufa(int times) {
        //��������ÿ��ȫ��.
        if (MinaMessageOp.msgMap.size() == 0) {
            return;
        }
        //�м�ȱʧ�ķְ�...���һ���ְ�ȱʧ...�����һ�ְ�ȱʧ��������а����ᶪ��,���Ҫ�󲹷�.
        final List<MinaMessageSend> sendlist = new ArrayList<MinaMessageSend>();
        Iterator<String> it = MinaMessageOp.msgMap.keySet().iterator();
        long now = new Date().getTime();
        List<String> removeKey = new ArrayList<String>(); // ���Ҫ���Ƴ���sn
        while (it.hasNext()) {
            String sn = it.next();
            MinaMessageRec rec = MinaMessageOp.msgMap.get(sn);
            List<String> unpack = rec.getUnPackNo();
            if (unpack.size() == 0) {// ������ȷ
                if (times % 10 == 0) {
                    //logger.info("�ȴ�Ӧ�������:" + MinaMessageOp.msgMap.size());
                }
                MinaMessageSend mess = new MinaMessageSend(sn, "O", null);
                sendlist.add(mess);
                if (rec.getSendCount() < 0) {  // �ظ�����С��0�����sn���Ƴ���Ĭ�ϻظ�3�Σ�
                    removeKey.add(mess.getSn());
                }
            }
            //��֤�Ƿ�2����֮ǰ
            long cz = now - rec.getRecordTime().getTime();
            if (cz < 2 * 60 * 1001) {
                continue;
            }
            if (cz > 4 * 60 * 1000) { // ���ӳ���4���ӣ����Ƴ�sn
                removeKey.add(sn);
                continue;
            }
            if (unpack.size() > 0) {// ����
                //ÿ10�������־
                if (times % 10 == 0) {
                    //logger.info("��ǰ���մ�������(δ������ϵ�����Ϣ):" + MinaMessageOp.msgMap.size());
                    //logger.info("������2����ǰ����δ������ϵļ��Ҫ�󲹷�.");
                }
                StringBuffer sb = new StringBuffer();
                for (String s : unpack) {
                    sb.append(s);
                }
                MinaMessageSend mess = new MinaMessageSend(sn, "R", sb.toString());
                sendlist.add(mess);
            }
        }
        for (String key : removeKey) {
            MinaMessageOp.msgMap.remove(key);
        }
        //��Ƶ��Ӧ��,20���̳߳�
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                send(sendlist);
            }
        });
    }

    /**
     * ֹͣ����
     */
    public void stop() {
        bufaProcessSwitch = false;
        if (dataAccepter != null) {
            //�ͷŶ˿�
            dataAccepter.unbind();
            //�����־������
            dataAccepter.getFilterChain().clear(); // ���Filter,��ֹ�´���������ʱ������������
            //�ͷ���Դ
            dataAccepter.dispose(); // ������дһ����洢IoAccept��ͨ��spring����������������dispose��Ҳ�����´���һ���µġ����߿�����init�����ڲ����д�����
            dataAccepter = null;
        }
    }


    protected void send(List<MinaMessageSend> msgList) {
        for (MinaMessageSend msend : msgList) {
            if (MinaMessageOp.msgMap.get(msend.getSn()) != null) {
                send(msend);
            }
        }
    }

    private boolean send(MinaMessageSend msg) {
        IoSession session = handler.getConnction(msg.getSn());
        if (session != null && session.isConnected()) {
            byte[] a = new MinaMessageOp().writeToBytes(msg);
            WriteFuture wf = session.write(a);
            wf.awaitUninterruptibly(1000);
            if (wf.isWritten()) {
                return true;
            } else {
                Throwable tr = wf.getException();
                if (tr != null) {
                    //logger.error(tr.getMessage(), tr);
                }
            }
        }
        return false;
    }

	/*public IoSession getSession(long sid) {
		return dataAccepter.getManagedSessions().get(sid);
	}*/

}
