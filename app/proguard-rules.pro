# Keep Gson models inside core.model & core.i18n
-keep class com.velvet.couplesgame.core.model.** { *; }
-keep class com.velvet.couplesgame.core.i18n.** { *; }
-keep class com.velvet.couplesgame.core.engine.ProceduralTaskGenerator$** { *; }

# Keep Room entities
-keep class com.velvet.couplesgame.data.local.entity.** { *; }

# Keep the Gson-serialised sealed letter record: its field names are the
# on-disk JSON keys, so renaming them would make stored letters unreadable
# after an app update.
-keep class com.velvet.couplesgame.core.letter.SealedLetterManager$StoredLetter { *; }
