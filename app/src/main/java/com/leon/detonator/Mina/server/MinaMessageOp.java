package com.leon.detonator.Mina.server;

import com.leon.detonator.Mina.vo.MinaMessageRec;
import com.leon.detonator.Mina.vo.MinaMessageSend;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * ������: gps�ն˷��͵���Ϣ�������ɱ����ʵ�� ��ڷ���Ϊdecoder���õ�readFromBytes()
 */
public class MinaMessageOp {
    //ConcurrentMap�̰߳�ȫ
    //�����Ӱ�����,���зְ�ʱ���õ���ֵ���Ǳ����뵽msgMap��
    //���ڴ�ŷְ���Ϣ,�Ա����зְ�����ʱ��������,��sn��dataType���key,thisChildData���value
    public static ConcurrentMap<String, MinaMessageRec> msgMap = new ConcurrentHashMap<String, MinaMessageRec>();
    //�ظ���Ϣ,�յ���Ϣʱ���轫��Ϣѹ�뱾map,��MinaServer.bufaProcess,key=sn,value[0]=num[1] [1]=O/E
    public static ConcurrentLinkedQueue<MinaMessageSend> yingdaQueue = new ConcurrentLinkedQueue<MinaMessageSend>();
    private String sn;// �ն����к�
    private int packCount;//��ʾ������ ֵ1��ʾ���ְ�

    private String packNo;//��ǰ�����,��λ

    private String errorMessage;//������Ϣ,��δ�����ô��,�Է��Ƿ���Ҫ��ֵ.

    /**
     * ��ȡУ���
     */
    public static byte getXor(byte[] data, int begin, int end) {
        byte A = 0;
        for (int i = begin; i < end; i++) {
            A ^= data[i];
        }
        return A;
    }

    /**
     * ��������ת��Ӧ��(byte[])
     *
     * @return
     */
    public final byte[] writeToBytes(MinaMessageSend mess) {
        byte bw = 36;//$
        //�÷�������ģ�ⷢ��,������
        IoBuffer buf = IoBuffer.allocate(300).setAutoExpand(true);
        buf.mark();
        String a = null;
        if ("R".equals(mess.getCommType())) { // ����
            // �������ݰ�����
            String sjbNum = mess.getPackNo().length() < 20 ? "0" + (mess.getPackNo().length() / 2)
                    : String.valueOf(mess.getPackNo().length() / 2);
            // ���ݰ�����,�������,�������,��������
            a = "#" + (mess.getPackNo().length() + 10) + mess.getCommType() + sjbNum + mess.getPackNo();
        } else { // ��ȷ
            a = "#08" + mess.getCommType();
        }
        byte[] b = a.getBytes();
        buf.put(b);
        //����xor
        String xor = String.valueOf(MinaMessageOp.getXor(b, 0, b.length));
        while (xor.length() < 3) {
            xor = "0" + xor;
        }
        buf.put(xor.getBytes());
        buf.put(bw);
        //������Ч����
        byte[] bb = new byte[buf.position()];
        buf.reset();
        buf.get(bb);
        //byte[] bb = buf.array();
        return bb;
    }

    /**
     * �������յ����ն����ݵ�������
     *
     * @param messageBytes,����ͷβ
     */
    public final MinaMessageRec readFromBytes(byte[] messageBytes) {
        //��֤����������,ȡ��xor��������xor��getXor()�ȶ�,����������㷨,���޸�getCheckXor����
        byte xor = getXor(messageBytes, 0, messageBytes.length - 4);//
        //�Է�Ҫ��xor��byteֵ���ַ�����ʽ,��3���ֽ�
        byte[] tmp = new byte[3];
        System.arraycopy(messageBytes, messageBytes.length - 4, tmp, 0, 3);
        if (xor == Integer.parseInt(new String(tmp))) {
            //ִ��parseOneData�õ��������һЩ����,�Թ�ServerHandler.messageReceived()ʹ��
            MinaMessageRec a = this.parseOneData(messageBytes);
            //ѹ��Ӧ����Ϣ
            yingdaQueue.add(new MinaMessageSend(sn, "O", packNo));
            return a;
        } else {
            errorMessage = "У���벻��ȷ";
            System.out.println(xor + errorMessage + new String(tmp));
            System.out.println("��������" + new String(messageBytes));
        }
        yingdaQueue.add(new MinaMessageSend(sn, "E", packNo));
        return null;
    }

    /**
     * һ�ΰ�������
     *
     * @param messageBytes
     * @return isover
     */
    private MinaMessageRec parseOneData(byte[] messageBytes) {
        //����messageBytes�õ�num������ֵ
        byte[] tmp = new byte[14];//һ���׹ܳ���,����������ظ�new
        System.arraycopy(messageBytes, 1, tmp, 0, 4);
        String pack = new String(tmp, 0, 4);
        packCount = Integer.parseInt(pack.substring(0, 2));
        packNo = pack.substring(2);
        //��������
        System.arraycopy(messageBytes, 8, tmp, 0, 8);
//		System.arraycopy(messageBytes, 5, tmp, 0, 6);
        sn = new String(tmp, 0, 8);
        //ȡ��data����
        MinaMessageRec md = null;
        //��������Ϣ
        int i = 16;
//		int i = 11;
        if ("01".equals(packNo)) {//�װ�
            md = msgMap.get(sn);
            if (md == null) {
                md = new MinaMessageRec();
            }
            msgMap.put(sn, md);
            md.setSn(sn);
            md.setPackCount(packCount);
            //������γ��13�ֽ�
            System.arraycopy(messageBytes, i, tmp, 0, 13);
            pack = new String(tmp, 0, 13);
            md.setLng(Integer.parseInt(pack.substring(0, 7)) / 10000d);
            md.setLat(Integer.parseInt(pack.substring(7)) / 10000d);
            //��ʱ��12�ֽ�
            i += 13;
            System.arraycopy(messageBytes, i, tmp, 0, 12);
            md.setQbDate(new String(tmp, 0, 12));
            i += 12;
        } else {
            md = msgMap.get(sn);
            if (md == null) {
                md = new MinaMessageRec();
            }
            msgMap.put(sn, md);
            md.setSn(sn);
            md.setPackCount(packCount);
        }
        md.addPackNo(packNo);
        //�����׹���
        while (messageBytes.length > i + 10) {
            System.arraycopy(messageBytes, i, tmp, 0, 14);
            pack = new String(tmp);
            md.putLeiguan(pack.substring(0, 13), pack.substring(13));
            i += 14;
        }
        return md;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}