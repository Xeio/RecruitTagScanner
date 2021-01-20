package com.xeio.recruittagscanner.managers

import com.google.gson.Gson
import com.xeio.recruittagscanner.data.Operator
import com.xeio.recruittagscanner.data.Tag
import com.xeio.recruittagscanner.data.Type
import java.io.InputStreamReader
import java.net.URL

class DataManager {
    companion object {
        var allTags = arrayOf<Tag>()
        var allTypes = arrayOf<Type>()
        var allOperators = arrayOf<Operator>()
        var searchTags = listOf<String>()
        var recruitableOperators = listOf<Operator>()

        fun init(){
            allTags = getTags()
            allTypes = getTypes()
            allOperators = getOperators()

            recruitableOperators = allOperators.filter { op -> !op.hidden && !op.globalHidden }

            val translatedTags = mutableListOf<String>()
            translatedTags.addAll(allTags.filter { t -> t.tagEN.isNotBlank() }.map { t -> t.tagEN })
            translatedTags.addAll(allTypes.filter { t -> t.typeEN.isNotBlank() } .map { t -> t.typeEN })
            searchTags = translatedTags.distinct()
        }

        private fun getTags(): Array<Tag> {
            val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-tags.json")
            val inputReader = InputStreamReader(url.openConnection().getInputStream())
            return Gson().fromJson(inputReader, Array<Tag>::class.java)
        }

        private fun getTypes(): Array<Type> {
            val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-type.json")
            val inputReader = InputStreamReader(url.openConnection().getInputStream())
            return Gson().fromJson(inputReader, Array<Type>::class.java)
        }

        private fun getOperators(): Array<Operator> {
            val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-akhr.json")
            val inputReader = InputStreamReader(url.openConnection().getInputStream())
            return Gson().fromJson(inputReader, Array<Operator>::class.java)
        }
    }
}