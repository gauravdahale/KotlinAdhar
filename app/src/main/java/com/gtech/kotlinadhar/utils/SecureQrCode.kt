package com.gtech.kotlinadhar.utils

import android.content.Context
import kotlin.Throws
import android.util.Log
import com.gemalto.jp2.JP2Decoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.experimental.and

/**
 * Class to decode scanned QRcode
 */
class SecureQrCode(protected var mContext: Context, scanData: String?) {
    protected var emailMobilePresent = 0
    protected var imageStartIndex = 0
    protected var imageEndIndex = 0
    protected var decodedData: ArrayList<String>? = null
    protected var signature: String? = null
    protected var email: String? = null
    lateinit var bigIntScanData :BigInteger
    protected var mobile: String? = null
    var scannedAadharCard: AadharCard
        protected set

    /**
     * Decompress the byte array, compression used is GZIP
     * @param byteScanData compressed byte array
     * @return uncompressed byte array
     * @throws QrCodeException
     */
    @Throws(QrCodeException::class)
    protected fun decompressData(byteScanData: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream(byteScanData.size)
        val bin = ByteArrayInputStream(byteScanData)
        var gis: GZIPInputStream? = null
        gis = try {
            GZIPInputStream(bin)
        } catch (e: IOException) {
            Log.e("Exception", "Decompressing QRcode, Opening byte stream failed: $e")
            throw QrCodeException("Error in opening Gzip byte stream while decompressing QRcode", e)
        }
        var size = 0
        val buf = ByteArray(1024)
        while (size >= 0) {
            try {
                size = gis!!.read(buf, 0, buf.size)
                if (size > 0) {
                    bos.write(buf, 0, size)
                }
            } catch (e: IOException) {
                Log.e("Exception", "Decompressing QRcode, writing byte stream failed: $e")
                throw QrCodeException("Error in writing byte stream while decompressing QRcode", e)
            }
        }
        try {
            gis!!.close()
            bin.close()
        } catch (e: IOException) {
            Log.e("Exception", "Decompressing QRcode, closing byte stream failed: $e")
            throw QrCodeException("Error in closing byte stream while decompressing QRcode", e)
        }
        return bos.toByteArray()
    }

    /**
     * Function to split byte array with delimiter
     * @param source source byte array
     * @return list of separated byte arrays
     */
    protected fun separateData(source: ByteArray): List<ByteArray> {
        val separatedParts: MutableList<ByteArray> = LinkedList()
        var begin = 0
        for (i in source.indices) {
            if (source[i] == SEPARATOR_BYTE) {
                // skip if first or last byte is separator
                if (i != 0 && i != source.size - 1) {
                    separatedParts.add(Arrays.copyOfRange(source, begin, i))
                }
                begin = i + 1
                // check if we have got all the parts of text data
                if (separatedParts.size == VTC_INDEX + 1) {
                    // this is required to extract image data
                    imageStartIndex = begin
                    break
                }
            }
        }
        return separatedParts
    }

    /**
     * function to decode string values
     * @param encodedData
     * @throws QrCodeException
     */
    @Throws(QrCodeException::class)
    protected fun decodeData(encodedData: List<ByteArray>) {
        val i = encodedData.iterator()
        decodedData = ArrayList()
        while (i.hasNext()) {
            try {
                var char = "ISO-8859-1"
                decodedData!!.add(String(i.next(), charset(char) ))
            } catch (e: UnsupportedEncodingException) {
                Log.e("Exception", "Decoding QRcode, ISO-8859-1 not supported: $e")
                throw QrCodeException("Decoding QRcode, ISO-8859-1 not supported", e)
            }
        }
        // set the value of email/mobile present flag
        emailMobilePresent = decodedData!![0].toInt()

        // populate decoded data
        scannedAadharCard.name = decodedData!![2]
        scannedAadharCard.dateOfBirth = decodedData!![3]
        scannedAadharCard.gender = decodedData!![4]
        scannedAadharCard.careOf = decodedData!![5]
        scannedAadharCard.district = decodedData!![6]
        scannedAadharCard.landmark = decodedData!![7]
        scannedAadharCard.house = decodedData!![8]
        scannedAadharCard.location = decodedData!![9]
        scannedAadharCard.pinCode = decodedData!![10]
        scannedAadharCard.postOffice = decodedData!![11]
        scannedAadharCard.state = decodedData!![12]
        scannedAadharCard.street = decodedData!![13]
        scannedAadharCard.subDistrict = decodedData!![14]
        scannedAadharCard.vtc = decodedData!![15]
    }

    /**
     * ref : https://uidai.gov.in/2-uncategorised/11320-aadhaar-paperless-offline-e-kyc-3.html
     * Hashing logic for Email ID :
     * Sha256(Sha256(Email+SharePhrase))*number of times last digit of Aadhaar number
     * (Ref ID field contains last 4 digits).
     *
     * Example :
     * Email: abc@gm.com
     * Aadhaar Number:XXXX XXXX 3632
     * Passcode : Lock@487
     * Hash : Sha256(Sha256(abc@gm.comLock@487))*2
     * In case of Aadhaar number ends with Zero we will hashed one time.
     * **********************************************************************
     * **********************************************************************
     * Hashing logic for Mobile Number :
     * Sha256(Sha256(Mobile+SharePhrase))*number of times last digit of Aadhaar number
     * (Ref ID field contains last 4 digits).
     *
     * Example :
     * Mobile: 1234567890
     * Aadhaar Number:XXXX XXXX 3632
     * Passcode : Lock@487
     * Hash: Sha256(Sha256(1234567890Lock@487))*2
     * In case of Aadhaar number ends with Zero we will hashed one time.
     * @param decompressedData
     * @throws QrCodeException
     */
    @Throws(QrCodeException::class)
    protected fun decodeMobileEmail(decompressedData: ByteArray) {
        var mobileStartIndex = 0
        var mobileEndIndex = 0
        var emailStartIndex = 0
        var emailEndIndex = 0
        when (emailMobilePresent) {
            3 -> {
                // both email mobile present
                mobileStartIndex = decompressedData.size - 289 // length -1 -256 -32
                mobileEndIndex = decompressedData.size - 257 // length -1 -256
                emailStartIndex = decompressedData.size - 322 // length -1 -256 -32 -1 -32
                emailEndIndex = decompressedData.size - 290 // length -1 -256 -32 -1
                mobile = bytesToHex(
                    Arrays.copyOfRange(
                        decompressedData,
                        mobileStartIndex,
                        mobileEndIndex + 1
                    )
                )
                email = bytesToHex(
                    Arrays.copyOfRange(
                        decompressedData,
                        emailStartIndex,
                        emailEndIndex + 1
                    )
                )
                // set image end index, it will be used to extract image data
                imageEndIndex = decompressedData.size - 323
            }
            2 -> {
                // only mobile
                email = ""
                mobileStartIndex = decompressedData.size - 289 // length -1 -256 -32
                mobileEndIndex = decompressedData.size - 257 // length -1 -256
                mobile = bytesToHex(
                    Arrays.copyOfRange(
                        decompressedData,
                        mobileStartIndex,
                        mobileEndIndex + 1
                    )
                )
                // set image end index, it will be used to extract image data
                imageEndIndex = decompressedData.size - 290
            }
            1 -> {
                // only email
                mobile = ""
                emailStartIndex = decompressedData.size - 289 // length -1 -256 -32
                emailEndIndex = decompressedData.size - 257 // length -1 -256
                email = bytesToHex(
                    Arrays.copyOfRange(
                        decompressedData,
                        emailStartIndex,
                        emailEndIndex + 1
                    )
                )
                // set image end index, it will be used to extract image data
                imageEndIndex = decompressedData.size - 290
            }
            else -> {
                // no mobile or email
                mobile = ""
                email = ""
                // set image end index, it will be used to extract image data
                imageEndIndex = decompressedData.size - 257
            }
        }
    }

    @Throws(QrCodeException::class)
    protected fun decodeImage(decompressedData: ByteArray?) {
        // image start and end indexes are calculated in functions : separateData and decodeMobileEmail
        val imageBytes = Arrays.copyOfRange(decompressedData, imageStartIndex, imageEndIndex + 1)
        val bmp = JP2Decoder(imageBytes).decode()
        scannedAadharCard.image = bmp
    }

    @Throws(QrCodeException::class)
    protected fun decodeSignature(decompressedData: ByteArray) {
        // extract 256 bytes from the end of the byte array
        val startIndex = decompressedData.size - 257
        val noOfBytes = 256
        signature = try {
            var char = "ISO-8859-1"
            String(decompressedData, startIndex, noOfBytes, charset(char))
        } catch (e: UnsupportedEncodingException) {
            Log.e("Exception", "Decoding Signature of QRcode, ISO-8859-1 not supported: $e")
            throw QrCodeException("Decoding Signature of QRcode, ISO-8859-1 not supported", e)
        }
    }

    companion object {
        protected const val SEPARATOR_BYTE = 255.toByte()
        protected const val REFERENCE_ID_INDEX = 1
        protected const val NAME_INDEX = 2
        protected const val DATE_OF_BIRTH_INDEX = 3
        protected const val GENDER_INDEX = 4
        protected const val CARE_OF_INDEX = 5
        protected const val DISTRICT_INDEX = 6
        protected const val LANDMARK_INDEX = 7
        protected const val HOUSE_INDEX = 8
        protected const val LOCATION_INDEX = 9
        protected const val PIN_CODE_INDEX = 10
        protected const val POST_OFFICE_INDEX = 11
        protected const val STATE_INDEX = 12
        protected const val STREET_INDEX = 13
        protected const val SUB_DISTRICT_INDEX = 14
        protected const val VTC_INDEX = 15

        /**
         * Convert byte array to hex string
         * @param bytes
         * @return
         */
        fun bytesToHex(bytes: ByteArray): String {
            val hexArray = "0123456789ABCDEF".toCharArray()
            val hexChars = CharArray(bytes.size * 2)
            for (j in bytes.indices) {
                val v: Int = (bytes[j] and 0xFF.toByte()).toInt()
                hexChars[j * 2] = hexArray[v ushr 4]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }

        private const val TAG = "SecureQrCode"
    }

    init {
        scannedAadharCard = AadharCard()
        Log.d(Companion.TAG, "scandat: ${scanData.toString()}: ")
        // 1. Convert Base10 to BigInt
      if (scanData!=null)  bigIntScanData = BigInteger(scanData,10)
        bigIntScanData= BigInteger(scanData!!.toString().trim().replace("\"", ""))

//        result.toString().trim().replaceAll("\"","")
        // 2. Convert BigInt to Byte Array
        val byteScanData = bigIntScanData.toByteArray()

        // 3. Decompress Byte Array
        val decompByteScanData = decompressData(byteScanData)

        // 4. Split the byte array using delimiter
        val parts = separateData(decompByteScanData)
        // Throw error if there are no parts
        if (parts.isEmpty()) {
            throw QrCodeException("Invalid QR Code Data, no parts found after splitting by delimiter")
        }

        // 5. decode extracted data to string
        decodeData(parts)

        // 6. Extract Signature
        decodeSignature(decompByteScanData)

        // 7. Email and Mobile number
        decodeMobileEmail(decompByteScanData)

        // 8. Extract Image
        decodeImage(decompByteScanData)
        Log.d("Rajdeol", "Data Decoded")
    }
}