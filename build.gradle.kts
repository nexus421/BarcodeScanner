// Der `buildscript`-Block wird für die Deklaration von Plugins nicht mehr benötigt,
// wenn der `plugins`-Block verwendet wird. [2]

plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.0" apply false
}
