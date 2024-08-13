package org.andbootmgr.app.util

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.Shell.FLAG_NON_ROOT_SHELL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

// Manage & extract Toolkit
class Toolkit(private val ctx: Context) {
	companion object {
		private const val TAG = "ABM_AssetCopy"
		private const val DEBUG = false
	}

	private var fail = false
	private val targetPath = File(ctx.filesDir.parentFile, "assets")

	suspend fun copyAssets(uinf: suspend () -> Unit, callback: suspend (Boolean) -> Unit) {
		val shell = Shell.Builder.create().setFlags(FLAG_NON_ROOT_SHELL).setTimeout(30).setContext(ctx).build()
		var b = try {
			withContext(Dispatchers.IO) {
				ctx.assets.open("cp/_ts").use { it.readBytes() }
			}
		} catch (e: IOException) {
			e.printStackTrace()
			fail = true
			callback(true)
			return
		}
		val s = String(b).trim()
		b = try {
			withContext(Dispatchers.IO) {
				FileInputStream(File(targetPath, "_ts")).use { it.readBytes() }
			}
		} catch (e: IOException) {
			ByteArray(0)
		}
		val s2 = String(b).trim()
		if (s != s2) {
			uinf.invoke()
			shell.newJob().add("rm -rf " + targetPath.absolutePath).exec()
			if (!targetPath.exists()) fail = fail or !targetPath.mkdir()
			if (!File(ctx.filesDir.parentFile, "files").exists()) fail = fail or !File(ctx.filesDir.parentFile, "files").mkdir()
			if (!File(ctx.filesDir.parentFile, "cache").exists()) fail = fail or !File(ctx.filesDir.parentFile, "cache").mkdir()
			copyAssets("Toolkit", "Toolkit")
			copyAssets("cp", "")
		}
		shell.newJob().add("chmod -R +x " + targetPath.absolutePath).exec()
		callback(fail)
	}

	private fun copyAssets(src: String, outp: String) {
		val assetManager: AssetManager = ctx.assets
		var files: Array<String>? = null
		try {
			files = assetManager.list(src)
		} catch (e: IOException) {
			Log.e(TAG, "Failed to get asset file list.", e)
			fail = true
		}
		assert(files != null)
		for (filename in files!!) {
			copyAssets(src, outp, assetManager, filename)
		}
	}

	private fun copyAssets(
		src: String,
		outp: String,
		assetManager: AssetManager,
		filename: String
	) {
		val `in`: InputStream
		val out: OutputStream
		try {
			`in` = assetManager.open("$src/$filename")
			val outFile = File(File(targetPath, outp), filename)
			out = FileOutputStream(outFile)
			copyFile(`in`, out)
			`in`.close()
			out.flush()
			out.close()
		} catch (e: FileNotFoundException) {
			val r = File(targetPath, outp).mkdir()
			if (DEBUG) Log.d(TAG, "Result of mkdir #1: $r")
			if (DEBUG) Log.d(TAG, Log.getStackTraceString(e))
			try {
				assetManager.open(src + File.separator + filename).close()
				copyAssets(src, outp, assetManager, filename)
			} catch (e2: FileNotFoundException) {
				val r2 = File(File(targetPath, outp), filename).mkdir()
				if (DEBUG) Log.d(TAG, "Result of mkdir #2: $r2")
				if (DEBUG) Log.d(TAG, Log.getStackTraceString(e2))
				copyAssets(src + File.separator + filename, outp + File.separator + filename)
			} catch (ex: IOException) {
				Log.e(TAG, "Failed to copy asset file: $filename", ex)
				fail = true
			}
		} catch (e: IOException) {
			Log.e(TAG, "Failed to copy asset file: $filename", e)
			fail = true
		}
	}

	@Throws(IOException::class)
	fun copyFile(`in`: InputStream, out: OutputStream) {
		val buffer = ByteArray(1024)
		var read: Int
		while (`in`.read(buffer).also { read = it } != -1) {
			out.write(buffer, 0, read)
		}
	}
}