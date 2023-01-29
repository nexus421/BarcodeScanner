package bayern.kickner.barcode_scanner_library

/**
 *
 * @param timeToWaitBetweenScans If a code was detected, the app waits this in milliseconds after the same barcode will be triggered again. This should prevent to scan a Barcode multiple times
 * @param ignoreSameCodeAlways if set to true, no barcode will be triggered more than once. Example: Scan barcode "1234", call callback, scan "1234" again -> ignore as long as the same barcode is scanned. This will override timeToWaitBetweenScans
 *
 */
data class ContinuousScanSettings(val timeToWaitBetweenScans: Int = 500, val ignoreSameCodeAlways: Boolean = false) {

    private var lastScanned = ""
    private var lastScannedTime: Long = Long.MAX_VALUE

    fun checkInputAndReturnIfOk(input: String): String? {
        if (ignoreSameCodeAlways && lastScanned == input) return null
        if (System.currentTimeMillis() < (lastScannedTime + timeToWaitBetweenScans)) return null
        lastScanned = input
        lastScannedTime = System.currentTimeMillis()
        return input
    }

}
