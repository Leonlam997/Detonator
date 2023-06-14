package com.leon.detonator.bean;

public class EnterpriseBean {
    private long id;
    private boolean selected;
    private String code;
    private String blasterId;
    private boolean commercial;
    private String contract;
    private String project;

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
}
