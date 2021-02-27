package com.xeio.recruittagscanner.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xeio.recruittagscanner.Globals
import com.xeio.recruittagscanner.R
import com.xeio.recruittagscanner.data.Operator
import com.xeio.recruittagscanner.services.ScreenshotNotificationService
import java.lang.Exception

class RecruitmentManager {
    companion object {
        fun checkRecruitment(context: Context, text: String, screenshotUri: Uri) {
            val lines = text.lines()
            val foundTags = DataManager.searchTags.filter { tag -> lines.any { line -> tag.equals(line,true) } }.toList()

            Log.i(Globals.TAG, "Detected tags: " + foundTags.joinToString())
            if (foundTags.count() < 5) {
                Log.i(Globals.TAG, "Alltext: $text")
            }

            if (foundTags.count() == 5) {
                if (RecruitPrefsManager.getDeleteSetting(context)) {
                    try {
                        val deleteCount = context.contentResolver.delete(screenshotUri, null, null)
                        Log.i(Globals.TAG, "Deleted '$deleteCount' files.")
                    }
                    catch (e: Exception) {
                        Log.i(Globals.TAG, "File delete failed: $e")
                    }
                }

                var bestCombo: Collection<String> = listOf()
                var bestScore = 0
                var bestMinLevel = 0
                var bestHasBot = false
                for (combo in combinationsUpToLength(foundTags, 3)) {
                    val comboOperators = DataManager.recruitableOperators
                            .filter { op -> op.level < 6 || combo.contains("Top Operator") } //Ignore 6* unless Top tag is present
                            .filter { op -> combo.intersect(op.localizedTags).count() == combo.count() }

                    if (comboOperators.isEmpty()) continue

                    val (score, minLevel, hasBot) = calcScoreMinLevelAndBot(combo, comboOperators)
                    if (score > bestScore) {
                        bestCombo = combo
                        bestMinLevel = minLevel
                        bestScore = score
                        bestHasBot = hasBot
                    }
                }

                if (RecruitPrefsManager.getHideNotificationSetting(context)) {
                    context.sendBroadcast(Intent(ScreenshotNotificationService.clearScreenshotNotification))
                }

                sendNotification(context, bestMinLevel, bestCombo, bestHasBot)
            }
        }

        private fun <T> combinationsUpToLength(tags: List<T>, length: Int, current: Int = 0): Sequence<List<T>> {
            if (length == 0) return emptySequence()
            if (current >= tags.size) return emptySequence()

            return sequence {
                for (i in tags.indices) {
                    val singleItemList = listOf(tags[i])
                    yield(singleItemList)

                    for (recursedSequence in combinationsUpToLength(tags, length - 1, i + 1)) {
                        yield(singleItemList.plus(recursedSequence))
                    }
                }
            }
        }

        private fun sendNotification(context: Context, minLevel: Int, bestTagCombo: Collection<String>, hasBot: Boolean) {
            if (minLevel > 3) {
                val text = "Best Combo: ${bestTagCombo.joinToString()} $minLevel*" +
                        if (minLevel == 4 && hasBot) {
                            " (Robot Possible)"
                        } else {
                            ""
                        }
                val title = "Recruitment Found"

                val headsUpView = RemoteViews(context.packageName, R.layout.heads_up_layout)
                headsUpView.setTextViewText(R.id.successMessage, title)
                headsUpView.setTextViewText(R.id.tagsMessage, text)

                val builder = NotificationCompat.Builder(context, Globals.RECRUIT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setTimeoutAfter(10000)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCustomHeadsUpContentView(headsUpView)

                with(NotificationManagerCompat.from(context)) {
                    notify(123456, builder.build())
                }
            } else {
                val toast = Toast.makeText(context, "No 4* Combos", Toast.LENGTH_SHORT)
                toast.show()
            }
        }

        private fun calcScoreMinLevelAndBot(tags: List<String>, operators: List<Operator>): Triple<Int, Int, Boolean> {
            if (operators.count() == 0) return Triple(0, 0, false)

            val maxLevel = operators.maxByOrNull { it.level }!!.level
            val minLevel = operators.minByOrNull { it.level }!!.level

            val minWithout1Stars = operators.filter{ it.level > 1}.minByOrNull { it.level }?.level
            if(minWithout1Stars != null && minWithout1Stars > minLevel){
                return Triple(minWithout1Stars * 1000 + maxLevel * 100 + (5 - tags.count()) * 10 + 1, minWithout1Stars, true)
            }

            return Triple(minLevel * 1000 + maxLevel * 100 + (5 - tags.count()) * 10, minLevel, false)
        }
    }
}