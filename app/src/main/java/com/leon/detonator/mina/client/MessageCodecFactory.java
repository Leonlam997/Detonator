package com.leon.detonator.mina.client;

import com.leon.detonator.mina.server.MessageEncoder;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MessageCodecFactory implements ProtocolCodecFactory {

    private final MessageDecoder decoder;
    private final MessageEncoder encoder;

    public MessageCodecFactory() {
        this.decoder = new MessageDecoder();
        this.encoder = new MessageEncoder();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return this.encoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return this.decoder;
    }

}
