package bayern.kickner.barcode_scanner_library

enum class Torch {
    /**
     * Always on
     */
    ForceOn,

    /**
     * Always off
     */
    Off,

    /**
     * Starting off.
     * User can activate the torch by pressing the torch button.
     */
    Manual
}