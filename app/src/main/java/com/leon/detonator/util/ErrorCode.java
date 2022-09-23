package com.leon.detonator.util;

import java.util.HashMap;
import java.util.Map;

public class ErrorCode {
    public static final Map<String, String> downloadErrorCode;
    public static final Map<String, String> detonatorErrorCode;

    static {
        downloadErrorCode = new HashMap<>();
        downloadErrorCode.put("0", "成功");
        downloadErrorCode.put("1", "非法的申请信息");
        downloadErrorCode.put("2", "未找到该起爆器设备信息或起爆器未设置作业任务");
        downloadErrorCode.put("3", "该起爆器未设置作业任务");
        downloadErrorCode.put("4", "起爆器在黑名单中");
        downloadErrorCode.put("5", "起爆位置不在起爆区域内");
        downloadErrorCode.put("6", "起爆位置在禁爆区域内");
        downloadErrorCode.put("7", "该起爆器已注销/报废");
        downloadErrorCode.put("8", "禁爆任务");
        downloadErrorCode.put("9", "作业合同存在项目");
        downloadErrorCode.put("10", "作业任务未设置准爆区域");
        downloadErrorCode.put("11", "离线下载不支持生产厂家试爆");
        downloadErrorCode.put("12", "营业性单位必须设置合同或者项目");
        downloadErrorCode.put("99", "网络连接失败");
        detonatorErrorCode = new HashMap<>();
        detonatorErrorCode.put("0", "雷管正常");
        detonatorErrorCode.put("1", "雷管在黑名单中");
        detonatorErrorCode.put("2", "雷管已使用");
        detonatorErrorCode.put("3", "申请的雷管UID不存在");

    }
}
