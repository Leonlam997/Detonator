package com.leon.detonator.mina.client;

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
        byte[] a = in.array();
        int i = 0;
        while (in.remaining() > 0) {
            in.mark();
            byte tag = in.get();//�ҵ���ͷ,��Է��������10����byte,��get()Ĭ�ϻὫ����Ϊ��16����
            a[i] = tag;
            i++;
        }

        out.write(a); // ��������Message���¼�
        return false;
    }
}
