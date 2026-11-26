# ==============================================================================
# ProGuard & R8 Optimization and Obfuscation Rules
# Project: Insulin Calculator (network.spiritscorp.insulincalc)
# ==============================================================================

# Preserve line number information for readable stack traces in crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve generic signatures and runtime annotations for reflection, Coroutines & Compose
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
