package com.xeio.recruittagscanner.data

import com.google.gson.annotations.SerializedName
import com.xeio.recruittagscanner.managers.DataManager

class Operator
{
    @SerializedName("name_cn")
    var nameCN: String = ""

    @SerializedName("name_en")
    var nameEN: String = ""

    @SerializedName("name_jp")
    var nameJP: String = ""

    @SerializedName("name_kr")
    var nameKR: String = ""

    @SerializedName("camp")
    var camp: String = ""

    @SerializedName("type")
    var type: String = ""

    @SerializedName("level")
    var level: Int = 0

    @SerializedName("sex")
    var sex: String = ""

    @SerializedName("tags")
    var tags: Array<String> = Array(0) {""}

    @SerializedName("hidden")
    var hidden: Boolean = false

    @SerializedName("globalHidden")
    var globalHidden: Boolean = false

    val localizedTags: List<String> by lazy {
        tags.map { charTag -> DataManager.allTags.firstOrNull { tag -> tag.tagCN == charTag }?.tagEN }
            .plus(DataManager.allTypes.firstOrNull { type -> type.typeCN == this.type }?.typeEN)
            .mapNotNull { it }
    }
}