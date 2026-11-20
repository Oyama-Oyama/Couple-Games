# Keep Gson models inside core.model & core.i18n
-keep class com.velvet.couplesgame.core.model.** { *; }
-keep class com.velvet.couplesgame.core.i18n.** { *; }
-keep class com.velvet.couplesgame.core.engine.ProceduralTaskGenerator$** { *; }

# Keep Room entities
-keep class com.velvet.couplesgame.data.local.entity.** { *; }
