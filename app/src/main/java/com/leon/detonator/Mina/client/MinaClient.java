package com.leon.detonator.Mina.client;

import android.os.Handler;
import android.os.Message;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Mina.vo.MinaMessageRec;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MinaClient {
    private List<DetonatorInfoBean> detonatorList;
    private Date explodeTime;
    private String sn;
    private double lng, lat;
    private Handler handler;
    private NioSocketConnector connector;
    private ConnectFuture cf;
    private String host;

    public void uploadRecord() {
        try {
            if (null == connector) {
                connector = new NioSocketConnector();
                connector.getFilterChain().addLast("encode", new ProtocolCodecFilter(new MessageCodecFactory()));
                connector.getSessionConfig().setReadBufferSize(2048);
                connector.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10000);
                connector.setConnectTimeoutMillis(1000 * 60 * 3);
                connector.setHandler(new MinaHandler(handler));
            }
            if (null == cf && null != host) {
                String[] server = host.split(":");
                cf = connector.connect(new InetSocketAddress(server[0], Integer.parseInt(server[1])));
                cf.awaitUninterruptibly();
            }

            MinaMessageRec message = new MinaMessageRec();
            message.setLng(lng);
            message.setLat(lat);
            message.setSn(sn);
            SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss", Locale.CHINA);
            message.setQbDate(format.format(explodeTime));

            cf.getSession().write(message.toByte());

            for (DetonatorInfoBean b : detonatorList) {
                message.putLeiguan(b.getAddress(), "O");
            }
            cf.getSession().write(message.toByte());
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            Message message = handler.obtainMessage(MinaHandler.MINA_ERROR);
            message.obj = "连接服务器失败！";
            handler.sendMessage(message);
        }
    }

    public void closeConnect() {
        try {
            if (null != cf)
                cf.getSession().getCloseFuture().awaitUninterruptibly();
            if (null != connector)
                connector.dispose();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }


    public void setDetonatorList(List<DetonatorInfoBean> detonatorList) {
        this.detonatorList = detonatorList;
    }

    public void setExplodeTime(Date explodeTime) {
        this.explodeTime = explodeTime;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setHost(String host) {
        this.host = host;
    }
}