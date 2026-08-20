// Der `buildscript`-Block wird für die Deklaration von Plugins nicht mehr benötigt,
// wenn der `plugins`-Block verwendet wird. [2]

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
}
