package com.leon.detonator.mina.server;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class MessageDecoder extends CumulativeProtocolDecoder {

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //������3�鷽ʽд����:
        //String message = "";//��������������.
        //�˴���in��ȡ������,IoBuffer������е��������ݿ���α�
        //out.write(message); // ����ServerHandler�Ľ���messageReceived�¼�,message��Ϊ�ڶ�������

        //�����Ǵ���.
        if (in.remaining() < 1) {
            return false;
        }
        int pos = 0;
        while (in.remaining() > 0) {
            in.mark();
            byte tag = in.get();//�ҵ���ͷ,��Է��������10����byte,��get()Ĭ�ϻὫ����Ϊ��16����
            // �������Ŀ�ʼλ��
            if (tag == 0x2A //��ͷ���*
                    && in.remaining() > 0) {
                // Ѱ�Ұ��Ľ���λ��, ��ֹ������0x2A,����ȡ��������Ŀ�ʼλ��(�����ʼ�ͽ������һ��)
                tag = in.get();//ȡ��һ��
                while (tag != 0x24) { // ��β���$
                    if (in.remaining() <= 0) {
                        //û���ҵ�������ǣ��ȴ���һ�η���
                        in.reset();//�ص�mark����ͷ���,�ȴ�������������(ע: ��ָͬһ�����Ӽ�������)
                        return false;
                    }
                    tag = in.get();
                }
                //�ҵ��������
                pos = in.position();
                //ȡ�������֮������ݽ��н���
                int packetLength = pos - in.markValue();
                if (packetLength > 1) {
                    byte[] tmp = new byte[packetLength];
                    in.reset();
                    in.get(tmp);
                    MinaMessageOp message = new MinaMessageOp();
                    out.write(message.readFromBytes(tmp)); // ��������Message���¼�
                } else {
                    // ˵��������0x2A
                    in.reset();
                    in.get(); // ����2A˵��ǰ���ǰ�β�������ǰ�ͷ
                }
            }
        }
        return false;//ֻ��false
    }
}
