package com.leon.detonator.bean;

public class EnterpriseBean {
    private long id;
    /**
     * 是否选中
     **/
    private boolean selected;
    /**
     * 单位代码
     **/
    private String code;
    /**
     * 起爆员身份证号
     **/
    private String blasterId;
    /**
     * 是否营业性起爆单位
     **/
    private boolean commercial;
    /**
     * 合同编号
     **/
    private String contract;
    /**
     * 项目编号
     **/
    private String project;
    /**
     * 是否已删除
     **/
    private boolean deleted;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBlasterId() {
        return blasterId;
    }

    public void setBlasterId(String blasterId) {
        this.blasterId = blasterId;
    }

    public boolean isCommercial() {
        return commercial;
    }

    public void setCommercial(boolean commercial) {
        this.commercial = commercial;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
