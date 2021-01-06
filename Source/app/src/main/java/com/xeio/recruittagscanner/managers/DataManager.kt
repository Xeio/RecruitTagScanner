package com.xeio.recruittagscanner.managers

import com.google.gson.Gson
import com.xeio.recruittagscanner.data.Operator
import com.xeio.recruittagscanner.data.Tag
import com.xeio.recruittagscanner.data.Type
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.URL

class DataManager {
    companion object {
        var allTags: Array<Tag> = GetTags()
        var allTypes: Array<Type> = GetTypes()
        var allOperators: Array<Operator> = GetOperators()
        var recruitableOperators: List<Operator> = List(0) { Operator() }

        private fun GetTags(): Array<Tag> {
            GlobalScope.launch {
                val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-tags.json")
                val inputReader = InputStreamReader(url.openConnection().getInputStream())
                val tags = Gson().fromJson(inputReader, Array<Tag>::class.java)
                allTags = tags
            }
            return Array(0) { Tag() }
        }

        private fun GetTypes(): Array<Type> {
            GlobalScope.launch {
                val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-type.json")
                val inputReader = InputStreamReader(url.openConnection().getInputStream())
                val types = Gson().fromJson(inputReader, Array<Type>::class.java)
                allTypes = types
            }
            return Array(0) { Type() }
        }

        private fun GetOperators(): Array<Operator> {
            GlobalScope.launch {
                val url = URL("https://aceship.github.io/AN-EN-Tags/json/tl-akhr.json")
                val inputReader = InputStreamReader(url.openConnection().getInputStream())
                val operators = Gson().fromJson(inputReader, Array<Operator>::class.java)
                allOperators = operators
                recruitableOperators = operators.filter { op -> !op.hidden && !op.globalHidden }
            }
            return Array(0) { Operator() }
        }

        var searchTags: List<String>
            get() {
                var translatedTags = MutableList(0) { "" }
                translatedTags.addAll(allTags.filter { t -> !t.tagEN.isNullOrBlank() }.map { t -> t.tagEN })
                translatedTags.addAll(allTypes.filter { t -> !t.typeEN.isNullOrBlank() } .map { t -> t.typeEN })
                //searchTags = translatedTags
                return translatedTags.distinct()
            }
            private set(_) {}
    }
}