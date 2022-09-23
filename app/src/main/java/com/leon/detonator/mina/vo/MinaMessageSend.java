package com.leon.detonator.mina.vo;


public class MinaMessageSend {
    // �˴�������Ժ���Ϣbody����(��ӿ�)�Լ���set����
    private String sn;// �ն����к�
    private String commType = "R";// ��������"O/E/R"
    private String packNo;//�������߻�Ӧ�İ���

    public MinaMessageSend(String sn, String commType, String packNo) {
        super();
        this.sn = sn;
        this.commType = commType;
        this.packNo = packNo;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getCommType() {
        return commType;
    }

    public void setCommType(String commType) {
        this.commType = commType;
    }

    public String getPackNo() {
        return packNo;
    }

    public void setPackNo(String packNo) {
        this.packNo = packNo;
    }

}
