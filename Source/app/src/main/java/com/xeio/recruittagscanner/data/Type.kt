package com.xeio.recruittagscanner.data

import com.google.gson.annotations.SerializedName

class Type
{
    @SerializedName("type_cn")
    var typeCN: String = ""

    @SerializedName("type_en")
    var typeEN: String = ""

    @SerializedName("type_jp")
    var typeJP: String = ""

    @SerializedName("type_kr")
    var typeKR: String = ""

    @SerializedName("type_data")
    var typeData: String = ""
}