package com.xeio.recruittagscanner.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xeio.recruittagscanner.Globals
import com.xeio.recruittagscanner.R
import com.xeio.recruittagscanner.data.Operator
import com.xeio.recruittagscanner.services.ScreenshotNotificationService

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
                    //screenshotFile.delete()
                    var deleteCount = context.contentResolver.delete(screenshotUri, null, null)
                    Log.i(Globals.TAG, "Deleted '$deleteCount' files.")
                }

                var bestScore = 0
                var bestCombo: Collection<String> = listOf()
                var bestMinLevel = 0
                for (combo in combinationsUpToLength(foundTags, 3)) {
                    val comboOperators = DataManager.recruitableOperators.filter { op ->
                        combo.intersect(op.localizedTags).count() == combo.count()
                    }

                    if (comboOperators.isEmpty()) continue

                    val score = calcScore(combo, comboOperators)
                    if (score > bestScore) {
                        bestCombo = combo
                        bestScore = score
                        bestMinLevel = comboOperators.minBy { it.level }!!.level
                    }
                }

                if (RecruitPrefsManager.getHideNotificationSetting(context)) {
                    context.sendBroadcast(Intent(ScreenshotNotificationService.clearScreenshotNotification))
                }

                sendNotification(
                    context,
                    bestMinLevel >= 4,
                    "Best Combo: ${bestCombo.joinToString()} $bestMinLevel*"
                )
            }
        }

        private fun <T> combinationsUpToLength(
            tags: List<T>,
            length: Int,
            current: Int = 0
        ): Sequence<List<T>> {
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

        private fun sendNotification(context: Context, success: Boolean, text: String) {
            var builder = NotificationCompat.Builder(context, Globals.RECRUIT_CHANNEL_ID)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(if (success) "Recruitment Found" else "No 4* Recruit Pattern")
                .setContentText(text)
                .setTimeoutAfter(10000)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            with(NotificationManagerCompat.from(context)) {
                // notificationId is a unique int for each notification that you must define
                notify(123456, builder.build())
            }
        }

        private fun calcScore(tags: List<String>, operators: List<Operator>): Int {
            if (operators.count() == 0) return 0
            val maxLevel = operators.maxBy { it.level }!!.level
            val minLevel = operators.minBy { it.level }!!.level

            return minLevel * 100 + maxLevel * 10 + (5 - tags.count())
        }
    }
}