package com.xeio.recruittagscanner.data

import com.google.gson.annotations.SerializedName

class Tag
{
    @SerializedName("tag_cn")
    var tagCN: String = ""

    @SerializedName("tag_en")
    var tagEN: String = ""

    @SerializedName("tag_jp")
    var tagJP: String = ""

    @SerializedName("tag_kr")
    var tagKR: String = ""

    @SerializedName("type")
    var type: String = ""
}
