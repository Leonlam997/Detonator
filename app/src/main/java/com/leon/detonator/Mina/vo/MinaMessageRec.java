package com.leon.detonator.Mina.vo;

import com.leon.detonator.Mina.server.MinaMessageOp;

import org.apache.mina.core.buffer.IoBuffer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MinaMessageRec {
    //�ѽ����İ����
    private final List<String> packNo = new ArrayList<String>();
    // �˴�������Ժ���Ϣbody����(��ӿ�)�Լ���set����
    private double lng;// ���� Longitude ��дLng γ�� Latitude ��дLat
    private double lat;
    private String sn;// �ն����к�
    private String qbDate;// ��ʱ��,yyyymmddHHmiss
    private ConcurrentMap<String, String> leiguan = new ConcurrentHashMap<String, String>();//
    //�������ڼ��©������
    private int packCount;
    //�Ƿ��ѽ������
    private boolean isOver = false;
    //�׸��ְ��Ľ���ʱ��,�����жϲ���,��ǰ�趨Ϊ2����δ����ͼ���Ƿ���Ҫ����
    private Date recordTime = new Date();
    // �ظ�����
    private int sendCount = 3;


    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getQbDate() {
        return qbDate;
    }

    public void setQbDate(String qbDate) {
        this.qbDate = qbDate;
    }

    public boolean isOver() {
        return isOver;
    }

    public void setOver(boolean isOver) {
        this.isOver = isOver;
    }

    public Date getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Date recordTime) {
        this.recordTime = recordTime;
    }

    public Map<String, String> getLeiguan() {
        return leiguan;
    }

    public void putLeiguan(String a, String b) {
        leiguan.put(a, b);
    }

    public int getPackCount() {
        return packCount;
    }

    public void setPackCount(int packCount) {
        this.packCount = packCount;
    }

    public List<String> getUnPackNo() {
        List<String> tmp = new ArrayList<String>();
        if (packNo.size() == packCount) {
            return tmp;
        }
        for (int i = 1; i <= packCount; i++) {
            String s = i < 10 ? ("0" + i) : String.valueOf(i);
            if (!packNo.contains(s)) {
                tmp.add(s);
            }
        }
        return tmp;
    }

    public void addPackNo(String packNo) {
        boolean iscz = false;
        for (String no : this.packNo) {
            if (no.equals(packNo)) {
                iscz = true;
                break;
            }
        }
        if (!iscz) {
            this.packNo.add(packNo);
        }
    }

    public String getLngStr() {
        return String.valueOf(lng * 10000).split("\\.")[0];
    }

    public String getLatStr() {
        return String.valueOf(lat * 10000).split("\\.")[0];
    }

    public int getSendCount() {
        sendCount = sendCount > 0 ? sendCount - 1 : -1;
        return sendCount;
    }

    public void setSendCount(int sendCount) {
        this.sendCount = sendCount;
    }

    public byte[] toByte() {
        byte bw = 36;//$
        //�÷�������ģ�ⷢ��,������
        IoBuffer buf = IoBuffer.allocate(300).setAutoExpand(true);
        buf.mark();
        StringBuilder a = null;
        if (leiguan.size() == 0) {
            a = new StringBuilder("*02" + "01042" + this.getSn() + this.getLngStr() + this.getLatStr() + this.getQbDate());
            System.out.println(a.length());
        } else {
            a = new StringBuilder("*02" + "02158" + this.getSn());
            for (String key : leiguan.keySet()) {
                a.append(key).append(leiguan.get(key));
            }
        }
        //��ֵ,�򳤶Ȳ���
        byte[] b = a.toString().getBytes();
        buf.put(b);
        //����xor
        StringBuilder xor = new StringBuilder(String.valueOf(MinaMessageOp.getXor(b, 0, b.length)));
        while (xor.length() < 3) {
            xor.insert(0, "0");
        }
        buf.put(xor.toString().getBytes());
//		buf.put("120".getBytes());
        buf.put(bw);
        //������Ч����
        byte[] bb = new byte[buf.position()];
        buf.reset();
        buf.get(bb);
//		System.out.println(new String(buf.array()));
        return bb;
    }
}
