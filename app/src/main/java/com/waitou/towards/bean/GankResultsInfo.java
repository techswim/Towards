package com.waitou.towards.bean;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by waitou on 17/2/16.
 */

public class GankResultsInfo {

    @SerializedName("Android")
    @Expose
    public List<GankResultsTypeInfo> Android;

    @SerializedName("iOS")
    @Expose
    public List<GankResultsTypeInfo> iOS;

    @SerializedName("休息视频")
    @Expose
    public List<GankResultsTypeInfo> 休息视频;

    @SerializedName("前端")
    @Expose
    public List<GankResultsTypeInfo> 前端;

    @SerializedName("拓展资源")
    @Expose
    public List<GankResultsTypeInfo> 拓展资源;

    @SerializedName("福利")
    @Expose
    public List<GankResultsTypeInfo> 福利;
}
