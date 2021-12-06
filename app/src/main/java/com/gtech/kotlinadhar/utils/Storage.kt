package com.gtech.kotlinadhar.utils

import android.content.Context
import android.util.Log
import java.io.*
import java.lang.StringBuilder

/**
 * Class to save data into internal storage
 * This data gets deleted when the app is uninstalled
 * Created by RajinderPal on 6/16/2016.
 */
class Storage
/**
 * constructor
 * @param activity
 */(protected var mContext: Context) {
    /**
     * Write to storage file
     * @param data
     */
    fun writeToFile(data: String?) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(mContext.openFileOutput(STORAGE_FILE_NAME, Context.MODE_PRIVATE))
            outputStreamWriter.write(data)
            outputStreamWriter.close()
        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    /**
     * Read from storage file
     * @return String
     */
    fun readFromFile(): String {
        // ensure file is created
        checkFilePresent(STORAGE_FILE_NAME)
        var ret = ""
        try {
            val inputStream: InputStream? = mContext.openFileInput(STORAGE_FILE_NAME)
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var receiveString: String? = ""
                val stringBuilder = StringBuilder()
                while (bufferedReader.readLine().also { receiveString = it } != null) {
                    stringBuilder.append(receiveString)
                }
                inputStream.close()
                ret = stringBuilder.toString()
            }
        } catch (e: FileNotFoundException) {
            Log.e("Storage activity", "File not found: $e")
        } catch (e: IOException) {
            Log.e("Storage activity", "Can not read file: $e")
        }
        return ret
    }

    private fun checkFilePresent(fileName: String) {
        val path = mContext.filesDir.absolutePath + "/" + fileName
        val file = File(path)
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        protected const val STORAGE_FILE_NAME = "data_storage.txt"
    }
}